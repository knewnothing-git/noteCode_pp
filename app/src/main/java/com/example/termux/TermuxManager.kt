package com.example.termux

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.UUID

object TermuxManager {
    private const val TAG = "TermuxManager"

    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())

    private val _status = MutableStateFlow(TermuxEnvironmentStatus())
    val status: StateFlow<TermuxEnvironmentStatus> = _status.asStateFlow()

    private val _currentExecution = MutableStateFlow<TermuxExecutionItem?>(null)
    val currentExecution: StateFlow<TermuxExecutionItem?> = _currentExecution.asStateFlow()

    private val _history = MutableStateFlow<List<TermuxExecutionItem>>(emptyList())
    val history: StateFlow<List<TermuxExecutionItem>> = _history.asStateFlow()

    private val _workingDirectory = MutableStateFlow(TermuxConstants.DEFAULT_HOME)
    val workingDirectory: StateFlow<String> = _workingDirectory.asStateFlow()

    private val _runMode = MutableStateFlow(TermuxRunMode.BACKGROUND_CONSOLE)
    val runMode: StateFlow<TermuxRunMode> = _runMode.asStateFlow()

    // Store in-flight executions mapped by executionId
    private val activeExecutions = mutableMapOf<String, TermuxExecutionItem>()
    private var localProcess: Process? = null

    fun setWorkingDirectory(dir: String) {
        val cleanDir = dir.trim().ifBlank { TermuxConstants.DEFAULT_HOME }
        _workingDirectory.value = cleanDir
    }

    fun setRunMode(mode: TermuxRunMode) {
        _runMode.value = mode
    }

    /**
     * Inspects device packages to determine if Termux is installed and whether
     * RUN_COMMAND permission is granted.
     */
    fun checkEnvironment(context: Context) {
        try {
            val pm = context.packageManager
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageInfo(TermuxConstants.TERMUX_PACKAGE_NAME, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageInfo(TermuxConstants.TERMUX_PACKAGE_NAME, 0)
            }

            val versionName = packageInfo.versionName
            val hasPermission = context.checkCallingOrSelfPermission(TermuxConstants.PERMISSION_RUN_COMMAND) == PackageManager.PERMISSION_GRANTED

            _status.update {
                it.copy(
                    isTermuxInstalled = true,
                    termuxVersion = versionName,
                    hasRunCommandPermission = hasPermission
                )
            }
        } catch (_: PackageManager.NameNotFoundException) {
            _status.update {
                it.copy(
                    isTermuxInstalled = false,
                    termuxVersion = null,
                    hasRunCommandPermission = false
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking Termux environment: ${e.message}")
        }
    }

    /**
     * Executes a command via Termux RUN_COMMAND service or local fallback.
     */
    fun runCommand(
        context: Context,
        rawCommand: String,
        workDir: String? = null,
        mode: TermuxRunMode? = null
    ): TermuxExecutionItem {
        val command = rawCommand.trim()
        val effectiveWorkDir = workDir ?: _workingDirectory.value
        val effectiveMode = mode ?: _runMode.value
        val executionId = UUID.randomUUID().toString()

        val executionItem = TermuxExecutionItem(
            id = executionId,
            command = command,
            workDir = effectiveWorkDir,
            mode = effectiveMode,
            isRunning = true
        )

        _currentExecution.value = executionItem
        activeExecutions[executionId] = executionItem

        checkEnvironment(context)

        if (_status.value.isTermuxInstalled) {
            sendTermuxRunIntent(context, executionItem)
        } else {
            // Termux not installed: Run with local shell fallback and display diagnostic guide
            runLocalFallback(executionItem)
        }

        return executionItem
    }

    private fun sendTermuxRunIntent(context: Context, item: TermuxExecutionItem) {
        try {
            val intent = Intent(TermuxConstants.ACTION_RUN_COMMAND).apply {
                setClassName(TermuxConstants.TERMUX_PACKAGE_NAME, TermuxConstants.TERMUX_SERVICE_NAME)
                putExtra(TermuxConstants.EXTRA_COMMAND_PATH, TermuxConstants.DEFAULT_BASH)

                // Prepend login environment exports so node, npm, and pnpm in Termux $PREFIX/bin are always resolved
                val scriptCommand = buildString {
                    append("export PREFIX=\"${TermuxConstants.DEFAULT_PREFIX}\"; ")
                    append("export HOME=\"${TermuxConstants.DEFAULT_HOME}\"; ")
                    append("export PATH=\"\$PREFIX/bin:\$PREFIX/bin/applets:\$PATH\"; ")
                    append("cd \"${item.workDir}\" 2>/dev/null || true; ")
                    append(item.command)
                }

                putExtra(TermuxConstants.EXTRA_ARGUMENTS, arrayOf("-l", "-c", scriptCommand))
                putExtra(TermuxConstants.EXTRA_WORKDIR, item.workDir)
                putExtra(TermuxConstants.EXTRA_BACKGROUND, item.mode == TermuxRunMode.BACKGROUND_CONSOLE)
                putExtra(TermuxConstants.EXTRA_SESSION_ACTION, "0")
                putExtra(TermuxConstants.EXTRA_COMMAND_LABEL, "NoteCode++: ${item.command.take(40)}")

                if (item.mode == TermuxRunMode.BACKGROUND_CONSOLE) {
                    val resultIntent = Intent(context, TermuxResultReceiver::class.java).apply {
                        action = TermuxConstants.RESULT_ACTION
                        putExtra("executionId", item.id)
                        setPackage(context.packageName)
                    }

                    val pendingIntent = PendingIntent.getBroadcast(
                        context,
                        item.id.hashCode(),
                        resultIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                    )
                    putExtra(TermuxConstants.EXTRA_PENDING_INTENT, pendingIntent)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    context.startForegroundService(intent)
                } catch (_: Exception) {
                    context.startService(intent)
                }
            } else {
                context.startService(intent)
            }

            // If user selected interactive mode, bring Termux to the foreground so the session is visible
            if (item.mode == TermuxRunMode.INTERACTIVE_TERMUX) {
                val launchIntent = context.packageManager.getLaunchIntentForPackage(TermuxConstants.TERMUX_PACKAGE_NAME)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                }

                // Interactive mode does not return background pending intent; mark as dispatched
                updateExecution(
                    item.id,
                    stdout = "Command dispatched to interactive Termux terminal session:\n$ ${item.command}\n\nWorking Directory: ${item.workDir}\nSwitched to Termux window.",
                    exitCode = 0,
                    isRunning = false
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send RUN_COMMAND intent: ${e.message}", e)
            updateExecution(
                item.id,
                stderr = "Failed to dispatch command to Termux: ${e.localizedMessage}\n\nCheck that:\n1. Termux is installed.\n2. 'Run commands in Termux' permission is granted in Android App Info settings.\n3. 'allow-external-apps = true' is enabled in ~/.termux/termux.properties.",
                exitCode = 1,
                errorMessage = e.localizedMessage,
                isRunning = false
            )
        }
    }

    private fun runLocalFallback(item: TermuxExecutionItem) {
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val process = ProcessBuilder("/system/bin/sh", "-c", item.command)
                    .redirectErrorStream(false)
                    .start()

                localProcess = process

                val stdoutReader = BufferedReader(InputStreamReader(process.inputStream))
                val stderrReader = BufferedReader(InputStreamReader(process.errorStream))

                val stdout = stdoutReader.readText()
                val stderr = stderrReader.readText()
                val exitCode = process.waitFor()

                withContext(Dispatchers.Main) {
                    val helpfulNote = if (item.command.contains("npm") || item.command.contains("pnpm") || item.command.contains("node")) {
                        "\n\n[NoteCode++ Note: Termux is not currently installed. To run npm and pnpm on Android, install Termux from F-Droid to provide the Node.js runtime.]"
                    } else ""

                    updateExecution(
                        item.id,
                        stdout = stdout + helpfulNote,
                        stderr = stderr,
                        exitCode = exitCode,
                        isRunning = false
                    )
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    updateExecution(
                        item.id,
                        stderr = "Local shell error: ${e.localizedMessage}\n\nTermux is required to run npm and pnpm. Tap 'Setup & Guide' to install Termux.",
                        exitCode = 1,
                        errorMessage = e.localizedMessage,
                        isRunning = false
                    )
                }
            } finally {
                localProcess = null
            }
        }
    }

    /**
     * Receives results dispatched by TermuxResultReceiver.
     */
    fun handleResult(intent: Intent) {
        val executionId = intent.getStringExtra("executionId") ?: _currentExecution.value?.id ?: ""

        val resultBundle = intent.getBundleExtra(TermuxConstants.EXTRA_PLUGIN_RESULT_BUNDLE)
            ?: intent.getBundleExtra(TermuxConstants.EXTRA_PLUGIN_RESULT_BUNDLE_LEGACY)
            ?: intent.extras

        val stdout = resultBundle?.getString(TermuxConstants.EXTRA_PLUGIN_RESULT_BUNDLE_STDOUT)
            ?: resultBundle?.getString("stdout")
            ?: ""

        val stderr = resultBundle?.getString(TermuxConstants.EXTRA_PLUGIN_RESULT_BUNDLE_STDERR)
            ?: resultBundle?.getString("stderr")
            ?: ""

        val exitCode = when {
            resultBundle?.containsKey(TermuxConstants.EXTRA_PLUGIN_RESULT_BUNDLE_EXIT_CODE) == true ->
                resultBundle.getInt(TermuxConstants.EXTRA_PLUGIN_RESULT_BUNDLE_EXIT_CODE)
            resultBundle?.containsKey("exitCode") == true ->
                resultBundle.getInt("exitCode")
            else -> 0
        }

        val err = resultBundle?.getInt(TermuxConstants.EXTRA_PLUGIN_RESULT_BUNDLE_ERR, -1) ?: -1
        val errmsg = resultBundle?.getString(TermuxConstants.EXTRA_PLUGIN_RESULT_BUNDLE_ERRMSG) ?: ""

        val combinedStderr = if (err > 0 && errmsg.isNotBlank()) {
            if (stderr.isBlank()) "Termux Error ($err): $errmsg" else "$stderr\nTermux Error ($err): $errmsg"
        } else {
            stderr
        }

        updateExecution(
            executionId,
            stdout = stdout,
            stderr = combinedStderr,
            exitCode = exitCode,
            isRunning = false
        )
    }

    private fun updateExecution(
        id: String,
        stdout: String = "",
        stderr: String = "",
        exitCode: Int? = null,
        errorMessage: String? = null,
        isRunning: Boolean = false
    ) {
        val existing = activeExecutions[id] ?: _currentExecution.value ?: return
        val updated = existing.copy(
            stdout = if (stdout.isNotEmpty()) stdout else existing.stdout,
            stderr = if (stderr.isNotEmpty()) stderr else existing.stderr,
            exitCode = exitCode ?: existing.exitCode,
            errorMessage = errorMessage ?: existing.errorMessage,
            isRunning = isRunning
        )

        activeExecutions[id] = updated

        if (_currentExecution.value?.id == id || _currentExecution.value == null) {
            _currentExecution.value = updated
        }

        if (!isRunning) {
            _history.update { current ->
                listOf(updated) + current.filterNot { it.id == id }.take(25)
            }
        }
    }

    fun clearOutput() {
        _currentExecution.value = null
    }

    fun cancelRunningCommand() {
        localProcess?.destroy()
        localProcess = null
        _currentExecution.value?.let { current ->
            if (current.isRunning) {
                updateExecution(
                    current.id,
                    stderr = "\n[Command canceled by user]",
                    exitCode = 130,
                    isRunning = false
                )
            }
        }
    }

    fun openTermuxApp(context: Context): Boolean {
        return try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(TermuxConstants.TERMUX_PACKAGE_NAME)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                true
            } else {
                openTermuxInstallPage(context)
                false
            }
        } catch (_: Exception) {
            openTermuxInstallPage(context)
            false
        }
    }

    fun openTermuxInstallPage(context: Context) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(TermuxConstants.TERMUX_FDROID_URL)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(TermuxConstants.TERMUX_GITHUB_URL)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (_: Exception) {}
        }
    }

    fun openAppSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {}
    }

    /**
     * Parses `"scripts": { "dev": "...", "build": "..." }` from package.json text.
     */
    fun parsePackageJsonScripts(content: String): List<PackageJsonScript> {
        val scripts = mutableListOf<PackageJsonScript>()
        try {
            val scriptsBlockRegex = Regex("\"scripts\"\\s*:\\s*\\{([\\s\\S]*?)\\}")
            val match = scriptsBlockRegex.find(content) ?: return emptyList()
            val innerContent = match.groupValues[1]

            val entryRegex = Regex("\"([^\"]+)\"\\s*:\\s*\"([^\"]+)\"")
            entryRegex.findAll(innerContent).forEach { entry ->
                val name = entry.groupValues[1].trim()
                val cmd = entry.groupValues[2].trim()
                if (name.isNotEmpty() && cmd.isNotEmpty()) {
                    scripts.add(PackageJsonScript(name, cmd))
                }
            }
        } catch (_: Exception) {}
        return scripts
    }
}
