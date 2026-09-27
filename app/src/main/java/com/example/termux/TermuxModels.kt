package com.example.termux

import java.util.UUID

enum class TermuxRunMode {
    BACKGROUND_CONSOLE,  // Executes in background, output streams back into NoteCode++ terminal console
    INTERACTIVE_TERMUX   // Launches the full Termux app terminal window for interactive sessions
}

data class TermuxExecutionItem(
    val id: String = UUID.randomUUID().toString(),
    val command: String,
    val workDir: String,
    val mode: TermuxRunMode,
    val timestamp: Long = System.currentTimeMillis(),
    val isRunning: Boolean = false,
    val stdout: String = "",
    val stderr: String = "",
    val exitCode: Int? = null,
    val errorMessage: String? = null
) {
    val isSuccess: Boolean get() = exitCode == 0
    val isFailure: Boolean get() = exitCode != null && exitCode != 0
}

data class TermuxEnvironmentStatus(
    val isTermuxInstalled: Boolean = false,
    val termuxVersion: String? = null,
    val hasRunCommandPermission: Boolean = false
)

data class PackageJsonScript(
    val name: String,
    val command: String
)
