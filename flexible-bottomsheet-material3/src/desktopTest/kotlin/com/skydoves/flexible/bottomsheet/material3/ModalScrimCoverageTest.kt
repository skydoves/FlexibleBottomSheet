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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.skydoves.flexible.core.FlexibleSheetSize
import com.skydoves.flexible.core.FlexibleSheetValue
import com.skydoves.flexible.core.rememberFlexibleBottomSheetState
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Screenshot regression test for the modal scrim coverage (issues #98 / #15).
 *
 * The modal scrim must fill the whole popup window edge-to-edge, including behind the top system bar
 * area, rather than being clipped to the (bottom-aligned, wrap-content) sheet container. This test
 * renders a modal wrap-content sheet with an opaque scrim over a white background and verifies the
 * top corners of the popup are painted by the scrim, not the background.
 *
 * Note: on the desktop/skia target `screenHeight()` equals the window size, so this specific
 * edge-to-edge gap only reproduces on Android (where the config height excludes the system bars). The
 * regression itself was verified visually on an Android emulator; this test guards the invariant that
 * the scrim covers the full popup so a future re-nesting of the scrim under an inset/shorter container
 * is caught.
 */
@OptIn(ExperimentalTestApi::class)
class ModalScrimCoverageTest {

  @Test
  fun modalScrim_coversTopCornersOfWindow() = runComposeUiTest {
    setContent {
      Box(Modifier.fillMaxSize().background(Color.White)) {
        val state = rememberFlexibleBottomSheetState(
          isModal = true,
          initialValue = FlexibleSheetValue.SlightlyExpanded,
          skipSlightlyExpanded = false,
          flexibleSheetSize = FlexibleSheetSize(
            fullyExpanded = 0.85f,
            slightlyExpanded = FlexibleSheetSize.WrapContent,
          ),
        )
        FlexibleBottomSheet(
          onDismissRequest = {},
          sheetState = state,
          scrimColor = Color.Red,
          dragHandle = null,
        ) {
          Box(Modifier.fillMaxWidth().height(120.dp))
        }
      }
    }
    waitForIdle()

    val image = onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.IsPopup))
      .onFirst()
      .captureToImage()
    val pixels = image.toPixelMap()

    val topLeft = pixels[1, 1]
    val topRight = pixels[pixels.width - 2, 1]

    // Scrim is opaque red; if the top were uncovered it would be the white background.
    val topLeftCovered = topLeft.red > 0.5f && topLeft.blue < 0.5f
    val topRightCovered = topRight.red > 0.5f && topRight.blue < 0.5f
    assertTrue(topLeftCovered, "Top-left corner not covered by scrim: $topLeft")
    assertTrue(topRightCovered, "Top-right corner not covered by scrim: $topRight")
  }
}
