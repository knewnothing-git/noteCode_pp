package com.example.termux

object TermuxConstants {
    // Termux Package & Service
    const val TERMUX_PACKAGE_NAME = "com.termux"
    const val TERMUX_SERVICE_NAME = "com.termux.app.RunCommandService"
    const val ACTION_RUN_COMMAND = "com.termux.RUN_COMMAND"

    // Intent Extras sent to Termux
    const val EXTRA_COMMAND_PATH = "com.termux.RUN_COMMAND_PATH"
    const val EXTRA_ARGUMENTS = "com.termux.RUN_COMMAND_ARGUMENTS"
    const val EXTRA_WORKDIR = "com.termux.RUN_COMMAND_WORKDIR"
    const val EXTRA_BACKGROUND = "com.termux.RUN_COMMAND_BACKGROUND"
    const val EXTRA_SESSION_ACTION = "com.termux.RUN_COMMAND_SESSION_ACTION"
    const val EXTRA_COMMAND_LABEL = "com.termux.RUN_COMMAND_LABEL"
    const val EXTRA_PENDING_INTENT = "com.termux.RUN_COMMAND_PENDING_INTENT"

    // Result Bundle Keys returned from Termux
    const val EXTRA_PLUGIN_RESULT_BUNDLE = "resultBundle"
    const val EXTRA_PLUGIN_RESULT_BUNDLE_LEGACY = "com.termux.app.TermuxService.EXTRA_PLUGIN_RESULT_BUNDLE"
    const val EXTRA_PLUGIN_RESULT_BUNDLE_STDOUT = "stdout"
    const val EXTRA_PLUGIN_RESULT_BUNDLE_STDERR = "stderr"
    const val EXTRA_PLUGIN_RESULT_BUNDLE_EXIT_CODE = "exitCode"
    const val EXTRA_PLUGIN_RESULT_BUNDLE_ERR = "err"
    const val EXTRA_PLUGIN_RESULT_BUNDLE_ERRMSG = "errmsg"

    // Broadcast action for NoteCode++ result receiver
    const val RESULT_ACTION = "com.example.termux.RESULT_ACTION"

    // Default Termux filesystem paths
    const val DEFAULT_PREFIX = "/data/data/com.termux/files/usr"
    const val DEFAULT_HOME = "/data/data/com.termux/files/home"
    const val DEFAULT_BASH = "/data/data/com.termux/files/usr/bin/bash"
    const val DEFAULT_SH = "/data/data/com.termux/files/usr/bin/sh"
    const val DEFAULT_LOGIN = "/data/data/com.termux/files/usr/bin/login"

    // Permission
    const val PERMISSION_RUN_COMMAND = "com.termux.permission.RUN_COMMAND"

    // Termux Download URLs
    const val TERMUX_FDROID_URL = "https://f-droid.org/en/packages/com.termux/"
    const val TERMUX_GITHUB_URL = "https://github.com/termux/termux-app/releases"

    // Presets for Node, npm, and pnpm
    val NPM_PNPM_QUICK_PRESETS = listOf(
        "pnpm dev",
        "npm run dev",
        "pnpm install",
        "npm install",
        "pnpm build",
        "npm run build",
        "pnpm test",
        "npm test",
        "pnpm start",
        "node -v && npm -v && pnpm -v"
    )

    // Setup command to enable external app execution in Termux
    const val TERMUX_ALLOW_EXTERNAL_APPS_CMD = "echo \"allow-external-apps = true\" >> ~/.termux/termux.properties"

    // Command to install Node.js LTS and pnpm in Termux
    const val INSTALL_NODE_AND_PNPM_CMD = "pkg update -y && pkg install -y nodejs-lts && npm install -g pnpm"
}
