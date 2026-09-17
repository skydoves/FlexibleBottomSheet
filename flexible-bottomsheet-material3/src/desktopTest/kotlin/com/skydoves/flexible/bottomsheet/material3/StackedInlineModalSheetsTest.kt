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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.skydoves.flexible.core.FlexibleSheetHost
import com.skydoves.flexible.core.FlexibleSheetSize
import com.skydoves.flexible.core.FlexibleSheetState
import com.skydoves.flexible.core.FlexibleSheetValue
import com.skydoves.flexible.core.rememberFlexibleBottomSheetState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Regression test for stacking two modal sheets on top of each other (issue #70).
 *
 * Two inline modal sheets composed in the same layout are two ordinary composables, so the second
 * one and its scrim draw over the first one and its scrim. Showing a second sheet therefore has to
 * dim the screen further, exactly as a second scrim composited over the first would: a second sheet
 * whose scrim replaced the first one, or was drawn under it, or was skipped because the container
 * already had one, would leave the screen exactly as dark as it already was.
 *
 * A pixel well above both sheets is sampled with one sheet and then with two. The scrim is an
 * opaque black at `0.32` alpha over a white background, so one scrim leaves `0.68` of the
 * background and two leave `0.68 * 0.68`; both the "strictly darker" and the "composited, not
 * replaced" halves of the claim are pinned against that.
 *
 * Note: this does not contrast the two hosts. Two window hosted sheets are two stacked popup layers
 * and their scrims composite the same way on this target; the test guards that hosting both sheets
 * in one composition does not collapse the two scrims into one.
 */
@OptIn(ExperimentalTestApi::class)
class StackedInlineModalSheetsTest {

  @Test
  fun aSecondInlineModalSheet_compositesItsScrimOverTheFirst() = runComposeUiTest {
    val showSecondSheet = mutableStateOf(false)
    var secondSheetState: FlexibleSheetState? = null
    setContent {
      Box(Modifier.fillMaxSize().background(Color.White).testTag(RootTag)) {
        StackedSheet(intermediatelyExpanded = 0.5f, containerColor = Color.Blue)

        val showSecond by showSecondSheet
        if (showSecond) {
          secondSheetState = StackedSheet(
            intermediatelyExpanded = 0.4f,
            containerColor = Color.Magenta,
          )
        }
      }
    }
    waitForIdle()

    val withOneSheet = scrimPixelAboveTheSheets().red
    assertTrue(
      withOneSheet < 0.99f,
      "The first sheet's scrim did not dim the background at all: $withOneSheet",
    )

    showSecondSheet.value = true
    waitForIdle()
    waitUntil(timeoutMillis = 5_000) {
      secondSheetState?.swipeableState?.anchors?.isNotEmpty() == true
    }
    waitForIdle()

    val withTwoSheets = scrimPixelAboveTheSheets().red
    assertTrue(
      withTwoSheets < withOneSheet,
      "A second modal sheet did not dim the screen any further: $withOneSheet -> $withTwoSheets",
    )
    assertEquals(
      withOneSheet * withOneSheet,
      withTwoSheets,
      absoluteTolerance = 0.02f,
      message = "The second scrim did not composite over the first one: $withOneSheet -> " +
        "$withTwoSheets",
    )
  }

  /**
   * Samples a row near the top of the window. Neither sheet ever reaches above the middle of the
   * window, so the pixel is the scrims composited over the white background and nothing else.
   */
  private fun ComposeUiTest.scrimPixelAboveTheSheets(): Color {
    val pixels = onNodeWithTag(RootTag).captureToImage().toPixelMap()
    return pixels[pixels.width / 2, pixels.height / 10]
  }
}

/** One inline modal sheet resting in the lower half of the window. */
@Composable
private fun StackedSheet(
  intermediatelyExpanded: Float,
  containerColor: Color,
): FlexibleSheetState {
  val sheetState = rememberFlexibleBottomSheetState(
    isModal = true,
    skipHiddenState = true,
    skipSlightlyExpanded = true,
    initialValue = FlexibleSheetValue.IntermediatelyExpanded,
    flexibleSheetSize = FlexibleSheetSize(
      fullyExpanded = 1f,
      intermediatelyExpanded = intermediatelyExpanded,
    ),
    sheetHost = FlexibleSheetHost.Inline,
  )
  FlexibleBottomSheet(
    onDismissRequest = {},
    sheetState = sheetState,
    containerColor = containerColor,
    scrimColor = ScrimColor,
    shape = RectangleShape,
    dragHandle = null,
  ) {
    Box(Modifier.fillMaxWidth().height(80.dp))
  }
  return sheetState
}

private const val RootTag = "root"

/** The Material scrim: an opaque black at a known alpha, so the composite is arithmetic. */
private val ScrimColor = Color.Black.copy(alpha = 0.32f)
