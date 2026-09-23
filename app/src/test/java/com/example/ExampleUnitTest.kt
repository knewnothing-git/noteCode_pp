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
}
