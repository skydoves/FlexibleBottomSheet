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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.skydoves.flexible.core.FlexibleSheetSize
import com.skydoves.flexible.core.FlexibleSheetValue
import com.skydoves.flexible.core.rememberFlexibleBottomSheetState
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Regression test for the non-modal sheet position at drag start (issues #68 / #67).
 *
 * Touching a non-modal sheet flips it from "as tall as the visible part" to "as tall as the
 * fully expanded sheet" so the whole sheet can travel with the finger. That container growth
 * must be invisible: the painted sheet has to stay exactly where the finger picked it up and
 * then follow the gesture. While the [FlexibleSheetValue.FullyExpanded] anchor was derived
 * from the measured sheet height, that growth moved the anchors underneath the running
 * gesture and the sheet jumped down and animated back (#68).
 *
 * The position is measured from the rendered frame rather than from the sheet's semantics
 * bounds: the sheet is placed with a `Modifier.offset` that sits *inside* its semantics
 * modifier, so the semantics bounds report the container, not the pixels the user sees.
 */
@OptIn(ExperimentalTestApi::class)
class SheetPositionOnDragStartTest {

  private val sheetMatcher =
    SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, "Bottom Sheet")

  @Test
  fun nonModalSheet_doesNotJump_whenADragBegins() = runComposeUiTest {
    var pixelsPerDp = 1f
    setContent {
      Box(Modifier.fillMaxSize().background(Color.White).testTag(WindowTag)) {
        pixelsPerDp = LocalDensity.current.density
        val sheetState = rememberFlexibleBottomSheetState(
          isModal = false,
          skipHiddenState = true,
          skipSlightlyExpanded = false,
          initialValue = FlexibleSheetValue.SlightlyExpanded,
          flexibleSheetSize = FlexibleSheetSize(
            fullyExpanded = 1.0f,
            intermediatelyExpanded = 0.5f,
            slightlyExpanded = 0.1f,
          ),
        )
        FlexibleBottomSheet(
          onDismissRequest = {},
          sheetState = sheetState,
          containerColor = SheetColor,
          dragHandle = null,
        ) {
          Box(Modifier.fillMaxWidth().height(80.dp))
        }
      }
    }
    waitForIdle()

    val image = onNodeWithTag(WindowTag).captureToImage()
    val topBeforeDrag = image.paintedSheetTop()
    assertTrue(
      topBeforeDrag in 1 until image.height,
      "The sheet was not painted before the drag (top row = $topBeforeDrag)",
    )

    // `draggable` only reports a drag once the touch slop is cleared, and the container only
    // grows on that report - a one pixel move would leave the sheet untouched and make this
    // test vacuous.
    val dragDistance = 60f
    onNode(sheetMatcher).performTouchInput {
      down(center)
      moveBy(Offset(0f, -dragDistance))
    }
    waitForIdle()

    val topAtDragStart = onNodeWithTag(WindowTag).captureToImage().paintedSheetTop()
    val tolerance = 4f * pixelsPerDp

    assertTrue(
      topAtDragStart <= topBeforeDrag + tolerance,
      "The sheet jumped down when the drag started: top went from $topBeforeDrag to " +
        "$topAtDragStart",
    )
    assertTrue(
      topAtDragStart < topBeforeDrag,
      "The sheet did not follow the upward drag: top stayed at $topAtDragStart",
    )
    assertTrue(
      topBeforeDrag - topAtDragStart <= dragDistance + tolerance,
      "The sheet overshot the drag: top went from $topBeforeDrag to $topAtDragStart",
    )

    onNode(sheetMatcher).performTouchInput { up() }
    waitForIdle()
  }

  /**
   * The topmost row of the captured frame that is painted with the sheet's container color, or `-1`
   * when the sheet is not on screen at all.
   */
  private fun ImageBitmap.paintedSheetTop(): Int {
    val pixels = toPixelMap()
    val x = pixels.width / 2
    for (y in 0 until pixels.height) {
      val color = pixels[x, y]
      if (color.blue > 0.5f && color.red < 0.5f && color.green < 0.5f) return y
    }
    return -1
  }

  private companion object {
    const val WindowTag = "window"

    /** An opaque color that cannot be confused with the white background behind the sheet. */
    val SheetColor = Color.Blue
  }
}
