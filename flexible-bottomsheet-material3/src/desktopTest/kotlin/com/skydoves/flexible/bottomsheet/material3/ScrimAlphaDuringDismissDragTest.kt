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
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.skydoves.flexible.core.FlexibleSheetSize
import com.skydoves.flexible.core.FlexibleSheetState
import com.skydoves.flexible.core.FlexibleSheetValue
import com.skydoves.flexible.core.rememberFlexibleBottomSheetState
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Screenshot regression test for the modal scrim fading with the gesture (issue #59).
 *
 * The scrim alpha used to be a 300ms tween on the boolean "is the sheet's target value still
 * visible". A dismiss gesture flips that boolean the moment the sheet crosses the positional
 * threshold, so the scrim lagged the finger for the whole first half of the drag and then
 * dropped away in one go - and a fast fling, which makes the swipeable re-evaluate its target
 * several times in a row, restarted the tween in the opposite direction mid-animation and made
 * the scrim visibly blink.
 *
 * The alpha is now read from the live sheet offset in the draw phase, and the offset is
 * monotonic across the whole gesture, so the scrim cannot lag, overshoot or blink. This test
 * drags a modal sheet toward hidden in steps without releasing and samples the rendered scrim
 * after each one: the opacity must come down step by step with the sheet, and every sample
 * taken mid-gesture must be *partially* faded. A boolean-driven alpha only ever samples as
 * fully opaque or fully clear, never in between.
 */
@OptIn(ExperimentalTestApi::class)
class ScrimAlphaDuringDismissDragTest {

  private val popupMatcher = SemanticsMatcher.keyIsDefined(SemanticsProperties.IsPopup)

  @Test
  fun modalScrim_fadesWithTheSheetOffset_duringADismissDrag() = runComposeUiTest {
    lateinit var state: FlexibleSheetState
    setContent {
      Box(Modifier.fillMaxSize().background(Color.White)) {
        val sheetState = rememberFlexibleBottomSheetState(
          isModal = true,
          skipHiddenState = false,
          skipSlightlyExpanded = true,
          initialValue = FlexibleSheetValue.IntermediatelyExpanded,
          flexibleSheetSize = FlexibleSheetSize(
            fullyExpanded = 0.9f,
            intermediatelyExpanded = 0.5f,
          ),
        )
        state = sheetState
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

    // The sheet's semantics node spans the whole popup because its `Modifier.offset` sits
    // inside the semantics modifier, so the press has to be aimed at the painted sheet by
    // hand. For a modal sheet the sheet offset is exactly its painted top edge.
    val paintedSheetTop = state.requireOffset()
    val alphas = mutableListOf(sampleScrimAlpha())

    onPopup().performTouchInput { down(Offset(width / 2f, paintedSheetTop + 40f)) }
    repeat(DragSteps) {
      // Steps are large enough that the first one already clears the touch slop, otherwise the
      // sheet would not move at all and the samples would be trivially equal.
      onPopup().performTouchInput { moveBy(Offset(0f, DragStepPixels)) }
      waitForIdle()
      alphas += sampleScrimAlpha()
    }
    // Read while the finger is still down, so it pairs with the last sample above.
    val anchors = state.swipeableState.anchors
    val finalOffset = state.requireOffset()

    onPopup().performTouchInput { up() }
    waitForIdle()

    assertTrue(
      alphas.first() > 0.95f,
      "The scrim was not opaque before the drag: $alphas",
    )

    // The blink in #59 is precisely a sample that goes back up mid-gesture.
    for (step in 1 until alphas.size) {
      assertTrue(
        alphas[step] <= alphas[step - 1] + Tolerance,
        "The scrim got more opaque again at step $step of a dismiss drag: $alphas",
      )
    }

    assertTrue(
      alphas.last() < alphas.first() - 0.3f,
      "The scrim barely faded across the whole dismiss drag: $alphas",
    )

    // The discriminating assertions: an offset-driven alpha is partially faded throughout the
    // gesture and lands exactly on the sheet's own dismissal progress, while a boolean-driven
    // one is pinned to 1f until the target flips and to 0f after it.
    for (step in 1 until alphas.size) {
      assertTrue(
        alphas[step] in Tolerance..(1f - Tolerance),
        "The scrim was not tracking the sheet offset at step $step: $alphas",
      )
    }

    val hidden = anchors.getValue(FlexibleSheetValue.Hidden)
    val leastExpandedVisible = anchors
      .filterKeys { it != FlexibleSheetValue.Hidden }
      .values
      .max()
    val dismissalProgress = ((hidden - finalOffset) / (hidden - leastExpandedVisible))
      .coerceIn(0f, 1f)
    assertTrue(
      abs(alphas.last() - dismissalProgress) <= Tolerance,
      "The scrim opacity ${alphas.last()} did not match the sheet's dismissal progress " +
        "$dismissalProgress (offset $finalOffset, anchors $anchors)",
    )
  }

  /**
   * The opacity of the scrim, sampled near the top of the popup where the sheet never reaches
   * during this gesture.
   *
   * An opaque red scrim over the white background composites to `(1, 1 - alpha, 1 - alpha)`, so
   * the red channel carries no information and the green channel is the one that follows the
   * alpha.
   */
  private fun ComposeUiTest.sampleScrimAlpha(): Float {
    val pixels = onPopup().captureToImage().toPixelMap()
    val color = pixels[pixels.width / 2, 2]
    assertTrue(
      color.red > 0.9f,
      "Sampled a pixel that is not the red scrim over the white background: $color",
    )
    return 1f - color.green
  }

  private fun ComposeUiTest.onPopup() = onAllNodes(popupMatcher).onFirst()

  private companion object {
    const val DragSteps = 5
    const val DragStepPixels = 70f

    /** Slack for pixel quantization of the composited alpha. */
    const val Tolerance = 0.02f
  }
}
