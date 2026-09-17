/*
 * Designed and developed by 2023 skydoves (Jaewoong Eum)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.skydoves.flexible.bottomsheet.material3

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.skydoves.flexible.core.FlexibleSheetSize
import com.skydoves.flexible.core.FlexibleSheetState
import com.skydoves.flexible.core.FlexibleSheetValue
import com.skydoves.flexible.core.rememberFlexibleBottomSheetState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Screenshot regression test for the modal scrim opacity (issues #59 / #15).
 *
 * The scrim alpha is now read from the live sheet offset instead of cross-fading a boolean,
 * which is what keeps it from blinking when a fast dismiss fling makes the sheet re-evaluate
 * its target value several times (#59). Deriving an alpha from the offset only behaves like
 * the platform bottom sheet if it uses the *scrim* progress and not the visibility progress:
 * the scrim must be fully opaque at every visible anchor and only fade on the last stretch
 * toward hidden. Expanding a sheet from slightly to fully expanded therefore must not change
 * the scrim at all.
 *
 * This renders an opaque scrim over a white background and samples a pixel near the very top of the
 * popup, well above the sheet itself, in both states. Wiring the visibility progress in by mistake
 * would make that pixel a pale red at slightly expanded and a stronger red at fully expanded.
 */
@OptIn(ExperimentalTestApi::class)
class ScrimOpacityAcrossStatesTest {

  @Test
  fun modalScrim_isEquallyOpaque_atEveryVisibleState() = runComposeUiTest {
    val fullyExpand = mutableStateOf(false)
    lateinit var state: FlexibleSheetState
    setContent {
      Box(Modifier.fillMaxSize().background(Color.White)) {
        val sheetState = rememberFlexibleBottomSheetState(
          isModal = true,
          skipHiddenState = true,
          skipSlightlyExpanded = false,
          initialValue = FlexibleSheetValue.SlightlyExpanded,
          flexibleSheetSize = FlexibleSheetSize(
            fullyExpanded = 0.85f,
            intermediatelyExpanded = 0.5f,
            slightlyExpanded = 0.15f,
          ),
        )
        state = sheetState
        val shouldFullyExpand by fullyExpand
        LaunchedEffect(shouldFullyExpand) {
          if (shouldFullyExpand) sheetState.fullyExpand()
        }
        FlexibleBottomSheet(
          onDismissRequest = {},
          sheetState = sheetState,
          scrimColor = Color.Red,
          dragHandle = null,
        ) {
          Box(Modifier.fillMaxWidth().height(80.dp))
        }
      }
    }
    waitForIdle()
    assertEquals(
      FlexibleSheetValue.SlightlyExpanded,
      state.currentValue,
      "The sheet did not settle at SlightlyExpanded",
    )

    val slightlyExpandedScrim = sampleScrimPixel()
    assertOpaqueScrim(slightlyExpandedScrim, "SlightlyExpanded")

    fullyExpand.value = true
    waitForIdle()
    waitUntil(timeoutMillis = 5_000) {
      state.currentValue == FlexibleSheetValue.FullyExpanded
    }
    waitForIdle()

    val fullyExpandedScrim = sampleScrimPixel()
    assertOpaqueScrim(fullyExpandedScrim, "FullyExpanded")

    assertEquals(
      slightlyExpandedScrim,
      fullyExpandedScrim,
      "The scrim changed opacity between SlightlyExpanded and FullyExpanded",
    )
  }

  /**
   * Samples the popup a couple of rows from the top, far above the sheet in either state, so the
   * pixel is the scrim composited over the white background and nothing else.
   */
  private fun ComposeUiTest.sampleScrimPixel(): Color {
    val pixels = onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.IsPopup))
      .onFirst()
      .captureToImage()
      .toPixelMap()
    return pixels[pixels.width / 2, 2]
  }

  private fun assertOpaqueScrim(color: Color, state: String) {
    // An opaque red scrim over white is pure red; any alpha below 1 would let the white through and
    // lift the green and blue channels.
    val opaque = color.red > 0.9f && color.green < 0.1f && color.blue < 0.1f
    assertTrue(opaque, "The scrim was not fully opaque at $state: $color")
  }
}
