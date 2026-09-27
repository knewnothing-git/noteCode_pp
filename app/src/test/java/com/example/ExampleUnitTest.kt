package com.example

import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun pinchZoomCalculations_clampWithinBounds() {
    val initialSize = 14f
    val zoomInFactor = 1.25f
    val zoomedIn = (initialSize * zoomInFactor).coerceIn(8f, 36f)
    assertEquals(17.5f, zoomedIn, 0.01f)

    val zoomOutFactor = 0.5f
    val zoomedOut = (initialSize * zoomOutFactor).coerceIn(8f, 36f)
    assertEquals(8f, (8f * 0.5f).coerceIn(8f, 36f), 0.01f)
    assertEquals(36f, (30f * 1.5f).coerceIn(8f, 36f), 0.01f)
  }

  @Test
  fun termuxPackageJsonParser_extractsScriptsCorrectly() {
    val samplePackageJson = """
      {
        "name": "my-node-app",
        "version": "1.0.0",
        "scripts": {
          "dev": "vite",
          "build": "tsc && vite build",
          "test": "vitest",
          "start": "node server.js"
        },
        "dependencies": {
          "vue": "^3.4.0"
        }
      }
    """.trimIndent()

    val scripts = com.example.termux.TermuxManager.parsePackageJsonScripts(samplePackageJson)
    assertEquals(4, scripts.size)
    assertEquals("dev", scripts[0].name)
    assertEquals("vite", scripts[0].command)
    assertEquals("build", scripts[1].name)
    assertEquals("tsc && vite build", scripts[1].command)
    assertEquals("test", scripts[2].name)
    assertEquals("start", scripts[3].name)
  }

  @Test
  fun termuxConstants_areProperlyFormed() {
    assertEquals("com.termux", com.example.termux.TermuxConstants.TERMUX_PACKAGE_NAME)
    assertEquals("com.termux.app.RunCommandService", com.example.termux.TermuxConstants.TERMUX_SERVICE_NAME)
    assertEquals("com.termux.RUN_COMMAND", com.example.termux.TermuxConstants.ACTION_RUN_COMMAND)
    assertEquals("com.termux.permission.RUN_COMMAND", com.example.termux.TermuxConstants.PERMISSION_RUN_COMMAND)
    assertTrue(com.example.termux.TermuxConstants.NPM_PNPM_QUICK_PRESETS.contains("pnpm dev"))
    assertTrue(com.example.termux.TermuxConstants.NPM_PNPM_QUICK_PRESETS.contains("npm run dev"))
  }
}
