package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.termux.PackageJsonScript
import com.example.termux.TermuxConstants
import com.example.termux.TermuxExecutionItem
import com.example.termux.TermuxManager
import com.example.termux.TermuxRunMode
import com.example.ui.theme.EditorThemeColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermuxConsoleSheet(
    activeDocumentTitle: String,
    activeDocumentContent: String,
    theme: EditorThemeColors,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val envStatus by TermuxManager.status.collectAsStateWithLifecycle()
    val currentExecution by TermuxManager.currentExecution.collectAsStateWithLifecycle()
    val workingDir by TermuxManager.workingDirectory.collectAsStateWithLifecycle()
    val runMode by TermuxManager.runMode.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    var inputCommand by remember { mutableStateOf("pnpm dev") }
    var customDirInput by remember { mutableStateOf(workingDir) }
    var showDirEditor by remember { mutableStateOf(false) }

    // Check environment upon opening
    LaunchedEffect(Unit) {
        TermuxManager.checkEnvironment(context)
    }

    val detectedScripts = remember(activeDocumentContent) {
        TermuxManager.parsePackageJsonScripts(activeDocumentContent)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxHeight(0.92f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF161B22))
                        .border(1.dp, Color(0xFF30363D), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = null,
                        tint = Color(0xFF3DDC84),
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Termux Runner",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "(npm & pnpm)",
                            fontSize = 12.sp,
                            color = Color(0xFF3DDC84),
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Status Pill
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (envStatus.isTermuxInstalled) Color(0xFF3DDC84) else Color(0xFFFFB300))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (envStatus.isTermuxInstalled) {
                                "Termux v${envStatus.termuxVersion ?: "Installed"} Ready"
                            } else {
                                "Termux Not Detected (Setup Needed)"
                            },
                            fontSize = 11.sp,
                            color = if (envStatus.isTermuxInstalled) Color(0xFF3DDC84) else Color(0xFFFFB300),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("termux_sheet_close")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Tabs: 0: Console, 1: package.json (if scripts), 2: Setup & Guide
            PrimaryTabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Console", fontSize = 13.sp) },
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Scripts", fontSize = 13.sp)
                            if (detectedScripts.isNotEmpty()) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(0xFF3DDC84))
                                        .padding(horizontal = 4.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "${detectedScripts.size}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                }
                            }
                        }
                    },
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Setup & Guide", fontSize = 13.sp) },
                    modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            when (selectedTab) {
                0 -> ConsoleTabContent(
                    envStatus = envStatus,
                    currentExecution = currentExecution,
                    inputCommand = inputCommand,
                    onInputCommandChange = { inputCommand = it },
                    workingDir = workingDir,
                    showDirEditor = showDirEditor,
                    customDirInput = customDirInput,
                    onToggleDirEditor = { showDirEditor = !showDirEditor },
                    onCustomDirChange = { customDirInput = it },
                    onApplyCustomDir = {
                        TermuxManager.setWorkingDirectory(customDirInput)
                        showDirEditor = false
                    },
                    runMode = runMode,
                    onRunModeChange = { TermuxManager.setRunMode(it) },
                    activeDocumentTitle = activeDocumentTitle,
                    onRunCommand = { cmd ->
                        TermuxManager.runCommand(context, cmd)
                    },
                    onCancelCommand = {
                        TermuxManager.cancelRunningCommand()
                    },
                    onClearOutput = {
                        TermuxManager.clearOutput()
                    },
                    onOpenTermux = {
                        TermuxManager.openTermuxApp(context)
                    }
                )
                1 -> ScriptsTabContent(
                    detectedScripts = detectedScripts,
                    activeDocumentTitle = activeDocumentTitle,
                    onRunScript = { cmd ->
                        inputCommand = cmd
                        selectedTab = 0
                        TermuxManager.runCommand(context, cmd)
                    }
                )
                2 -> SetupGuideTabContent(
                    envStatus = envStatus,
                    onOpenDownload = { TermuxManager.openTermuxInstallPage(context) },
                    onOpenSettings = { TermuxManager.openAppSettings(context) },
                    onRunSetup = { cmd ->
                        inputCommand = cmd
                        selectedTab = 0
                        TermuxManager.runCommand(context, cmd)
                    }
                )
            }
        }
    }
}

@Composable
private fun ConsoleTabContent(
    envStatus: com.example.termux.TermuxEnvironmentStatus,
    currentExecution: TermuxExecutionItem?,
    inputCommand: String,
    onInputCommandChange: (String) -> Unit,
    workingDir: String,
    showDirEditor: Boolean,
    customDirInput: String,
    onToggleDirEditor: () -> Unit,
    onCustomDirChange: (String) -> Unit,
    onApplyCustomDir: () -> Unit,
    runMode: TermuxRunMode,
    onRunModeChange: (TermuxRunMode) -> Unit,
    activeDocumentTitle: String,
    onRunCommand: (String) -> Unit,
    onCancelCommand: () -> Unit,
    onClearOutput: () -> Unit,
    onOpenTermux: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val terminalOutputScroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        // Mode Selector: Background Console vs Interactive Termux Window
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = runMode == TermuxRunMode.BACKGROUND_CONSOLE,
                onClick = { onRunModeChange(TermuxRunMode.BACKGROUND_CONSOLE) },
                label = { Text("App Console", fontSize = 12.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("mode_background_chip")
            )

            FilterChip(
                selected = runMode == TermuxRunMode.INTERACTIVE_TERMUX,
                onClick = { onRunModeChange(TermuxRunMode.INTERACTIVE_TERMUX) },
                label = { Text("Interactive Termux", fontSize = 12.sp) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                modifier = Modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .testTag("mode_interactive_chip")
            )
        }

        // Working Directory Bar
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Dir: ",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = workingDir,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onToggleDirEditor,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("edit_working_dir_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Edit directory",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            AnimatedVisibility(visible = showDirEditor) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    OutlinedTextField(
                        value = customDirInput,
                        onValueChange = onCustomDirChange,
                        label = { Text("Termux Working Directory") },
                        textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                onCustomDirChange(TermuxConstants.DEFAULT_HOME)
                            },
                            modifier = Modifier.defaultMinSize(minHeight = 44.dp)
                        ) {
                            Text("~ (Home)", fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = {
                                onCustomDirChange("/sdcard/Download")
                            },
                            modifier = Modifier.defaultMinSize(minHeight = 44.dp)
                        ) {
                            Text("/sdcard/Download", fontSize = 11.sp)
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Button(
                            onClick = onApplyCustomDir,
                            modifier = Modifier.defaultMinSize(minHeight = 44.dp)
                        ) {
                            Text("Save", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Quick Preset Chips (Horizontal Scroll)
        Text(
            text = "Quick Presets:",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp, bottom = 4.dp)
        )

        val quickPresetsScroll = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(quickPresetsScroll),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val presets = remember(activeDocumentTitle) {
                val list = mutableListOf(
                    "pnpm dev",
                    "npm run dev",
                    "pnpm install",
                    "npm install",
                    "pnpm build",
                    "npm test",
                    "node -v && npm -v && pnpm -v"
                )
                if (activeDocumentTitle.endsWith(".js") || activeDocumentTitle.endsWith(".ts")) {
                    list.add(0, "node \"$activeDocumentTitle\"")
                }
                list
            }

            presets.forEach { preset ->
                FilterChip(
                    selected = inputCommand == preset,
                    onClick = {
                        onInputCommandChange(preset)
                        onRunCommand(preset)
                    },
                    label = {
                        Text(
                            text = preset,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    },
                    modifier = Modifier.defaultMinSize(minHeight = 44.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Command Input Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputCommand,
                onValueChange = onInputCommandChange,
                placeholder = { Text("e.g. pnpm install or npm run dev", fontSize = 13.sp) },
                leadingIcon = {
                    Text(
                        text = "$",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF3DDC84),
                        modifier = Modifier.padding(start = 12.dp)
                    )
                },
                textStyle = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(
                    onGo = {
                        if (inputCommand.isNotBlank()) {
                            onRunCommand(inputCommand)
                        }
                    }
                ),
                modifier = Modifier
                    .weight(1f)
                    .testTag("termux_command_input")
            )

            Spacer(modifier = Modifier.width(8.dp))

            if (currentExecution?.isRunning == true) {
                Button(
                    onClick = onCancelCommand,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("termux_cancel_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = "Stop",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Stop", fontSize = 12.sp, color = Color.White)
                }
            } else {
                Button(
                    onClick = {
                        if (inputCommand.isNotBlank()) {
                            onRunCommand(inputCommand)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3DDC84)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("termux_run_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Run",
                        tint = Color(0xFF0E1117),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Run",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0E1117)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Terminal Output Screen
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0D1117)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 220.dp, max = 380.dp)
                .border(1.dp, Color(0xFF30363D), RoundedCornerShape(10.dp))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Terminal Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF161B22))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Traffic lights
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFFF5F56)))
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFFFFBD2E)))
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(0xFF27C93F)))
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Text(
                        text = when {
                            currentExecution?.isRunning == true -> "Running: ${currentExecution.command}"
                            currentExecution != null -> "$ ${currentExecution.command}"
                            else -> "Termux Shell Console"
                        },
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF8B949E),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    // Exit code badge
                    currentExecution?.let { exec ->
                        if (!exec.isRunning && exec.exitCode != null) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (exec.isSuccess) Color(0xFF238636) else Color(0xFFDA3633))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (exec.isSuccess) "Exit 0" else "Exit ${exec.exitCode}",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                    }

                    // Copy output
                    IconButton(
                        onClick = {
                            val out = buildString {
                                currentExecution?.let {
                                    appendLine("$ ${it.command}")
                                    if (it.stdout.isNotBlank()) appendLine(it.stdout)
                                    if (it.stderr.isNotBlank()) appendLine("STDERR:\n${it.stderr}")
                                }
                            }
                            if (out.isNotBlank()) {
                                clipboardManager.setText(AnnotatedString(out))
                                Toast.makeText(context, "Output copied to clipboard", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Output",
                            tint = Color(0xFF8B949E),
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    // Clear
                    IconButton(
                        onClick = onClearOutput,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear",
                            tint = Color(0xFF8B949E),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }

                if (currentExecution?.isRunning == true) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF3DDC84),
                        trackColor = Color(0xFF21262D)
                    )
                }

                // Terminal Body
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp)
                        .verticalScroll(terminalOutputScroll)
                ) {
                    SelectionContainer {
                        Column {
                            if (currentExecution == null) {
                                Text(
                                    text = """
NoteCode++ Termux Environment Bridge
-------------------------------------
Node.js, npm, and pnpm execution console for Android.

• Tap any preset chip above or type a command.
• Background Mode: Streams logs directly in this console.
• Interactive Mode: Launches full Termux terminal window.

Examples:
  pnpm install
  pnpm dev
  npm run build
  node -v && pnpm -v
                                    """.trimIndent(),
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp,
                                    color = Color(0xFF8B949E)
                                )
                            } else {
                                Text(
                                    text = "$ ${currentExecution.command}\n",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF3DDC84)
                                )

                                if (currentExecution.stdout.isNotBlank()) {
                                    Text(
                                        text = currentExecution.stdout,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        lineHeight = 16.sp,
                                        color = Color(0xFFC9D1D9)
                                    )
                                }

                                if (currentExecution.stderr.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = currentExecution.stderr,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        lineHeight = 16.sp,
                                        color = Color(0xFFFF7B72)
                                    )
                                }

                                if (currentExecution.isRunning) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CircularProgressIndicator(
                                            color = Color(0xFF3DDC84),
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Running in Termux environment...",
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            color = Color(0xFF8B949E)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Open in Termux direct action
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedButton(
                onClick = onOpenTermux,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .defaultMinSize(minHeight = 44.dp)
                    .testTag("open_termux_button")
            ) {
                Icon(
                    imageVector = Icons.Default.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Open Termux App Window", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun ScriptsTabContent(
    detectedScripts: List<PackageJsonScript>,
    activeDocumentTitle: String,
    onRunScript: (String) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(vertical = 4.dp)
    ) {
        if (detectedScripts.isNotEmpty()) {
            Text(
                text = "Detected scripts from $activeDocumentTitle:",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            detectedScripts.forEach { script ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = script.name,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = script.command,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = { onRunScript("pnpm run ${script.name}") },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF69220)),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 44.dp)
                                    .weight(1f)
                            ) {
                                Text("pnpm run ${script.name}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { onRunScript("npm run ${script.name}") },
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 44.dp)
                                    .weight(1f)
                            ) {
                                Text("npm run ${script.name}", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Standard Node & Package Scripts",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tip: Open or edit a package.json file to automatically detect and run project scripts.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    val standardScripts = listOf(
                        "dev" to "Start local dev server (Vite / Next.js / Express)",
                        "build" to "Compile and bundle production assets",
                        "test" to "Execute test runner (Vitest / Jest)",
                        "start" to "Launch production node server",
                        "lint" to "Run ESLint or Prettier code analysis"
                    )

                    standardScripts.forEach { (name, desc) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = name,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = desc,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Button(
                                onClick = { onRunScript("pnpm run $name") },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF69220)),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.defaultMinSize(minHeight = 44.dp)
                            ) {
                                Text("pnpm $name", fontSize = 11.sp)
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            OutlinedButton(
                                onClick = { onRunScript("npm run $name") },
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.defaultMinSize(minHeight = 44.dp)
                            ) {
                                Text("npm $name", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun SetupGuideTabContent(
    envStatus: com.example.termux.TermuxEnvironmentStatus,
    onOpenDownload: () -> Unit,
    onOpenSettings: () -> Unit,
    onRunSetup: (String) -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = "Termux & Node.js Setup Guide",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Follow these 4 simple steps to run npm and pnpm on your Android device.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Step 1: Install Termux
        SetupStepCard(
            stepNumber = 1,
            title = "Install Termux App",
            isDone = envStatus.isTermuxInstalled,
            description = if (envStatus.isTermuxInstalled) {
                "Termux v${envStatus.termuxVersion ?: ""} is installed on this device."
            } else {
                "Termux is required to provide the Linux runtime and Node.js environment. Note: The Google Play Store build is deprecated; install from F-Droid or GitHub."
            },
            actionButtonText = if (envStatus.isTermuxInstalled) "Termux Installed" else "Download Termux (F-Droid)",
            onAction = onOpenDownload,
            isCompleted = envStatus.isTermuxInstalled
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Step 2: Grant RUN_COMMAND Permission
        SetupStepCard(
            stepNumber = 2,
            title = "Grant 'Run Commands' Permission",
            isDone = envStatus.hasRunCommandPermission,
            description = "Android requires allowing NoteCode++ to execute commands in Termux. Go to App Info > Permissions > Additional permissions > Run commands in Termux environment.",
            actionButtonText = "Open NoteCode++ App Settings",
            onAction = onOpenSettings,
            isCompleted = envStatus.hasRunCommandPermission
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Step 3: Enable External Apps in Termux
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "3",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Enable External Apps in Termux",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Open Termux and run this one-line command to allow external applications to dispatch commands:",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF0D1117))
                        .padding(8.dp)
                ) {
                    Text(
                        text = TermuxConstants.TERMUX_ALLOW_EXTERNAL_APPS_CMD,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color(0xFF3DDC84)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(TermuxConstants.TERMUX_ALLOW_EXTERNAL_APPS_CMD))
                            Toast.makeText(context, "Command copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.defaultMinSize(minHeight = 44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy Command", fontSize = 11.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Step 4: Install Node.js and pnpm
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF3DDC84)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "4",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Install Node.js & pnpm in Termux",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Installs Node.js LTS and pnpm globally inside your Termux environment:",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF0D1117))
                        .padding(8.dp)
                ) {
                    Text(
                        text = TermuxConstants.INSTALL_NODE_AND_PNPM_CMD,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color(0xFF3DDC84)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(TermuxConstants.INSTALL_NODE_AND_PNPM_CMD))
                            Toast.makeText(context, "Command copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy", fontSize = 11.sp)
                    }

                    Button(
                        onClick = { onRunSetup(TermuxConstants.INSTALL_NODE_AND_PNPM_CMD) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3DDC84)),
                        modifier = Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 44.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Run in Termux", fontSize = 11.sp, color = Color.Black)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun SetupStepCard(
    stepNumber: Int,
    title: String,
    isDone: Boolean,
    description: String,
    actionButtonText: String,
    onAction: () -> Unit,
    isCompleted: Boolean
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(if (isDone) Color(0xFF3DDC84) else MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    if (isDone) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Completed",
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Text(
                            text = "$stepNumber",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                if (isDone) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFF238636))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Detected",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = description,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onAction,
                    modifier = Modifier.defaultMinSize(minHeight = 44.dp)
                ) {
                    Text(actionButtonText, fontSize = 11.sp)
                }
            }
        }
    }
}
