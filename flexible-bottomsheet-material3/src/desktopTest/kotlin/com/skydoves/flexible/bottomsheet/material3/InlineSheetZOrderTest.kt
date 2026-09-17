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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.skydoves.flexible.core.FlexibleSheetHost
import com.skydoves.flexible.core.FlexibleSheetSize
import com.skydoves.flexible.core.FlexibleSheetValue
import com.skydoves.flexible.core.rememberFlexibleBottomSheetState
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Regression tests for how a sheet layers against the rest of the app (issues #75 / #71 / #37).
 *
 * A sheet hosted in a platform window always draws above every composable of the app, which is why
 * a navigation drawer, a bottom app bar or a sticky footer could never be shown on top of it. An
 * inline sheet is an ordinary composable of the layout that declared it, so it layers by
 * composition order: whatever is composed after it draws over it.
 *
 * Both tests render the very same tree - a sheet, then an opaque bar composed after it - and only
 * change the host. The bar is sampled at the bottom of the window, where it overlaps the sheet, so
 * the pixel says which of the two won. The sheet's own color is sampled just below its top edge
 * first, so a sheet that failed to render could never make the inline expectation true by accident.
 */
@OptIn(ExperimentalTestApi::class)
class InlineSheetZOrderTest {

  @Test
  fun inlineSheet_isCoveredByASiblingComposedAfterIt() = runComposeUiTest {
    setContent { SheetUnderABar(FlexibleSheetHost.Inline) }
    waitForIdle()

    // The sheet really is part of this composition rather than a window of its own.
    assertTrue(popupNodeCount() == 0, "An inline sheet was still hosted in a popup window")
    assertSheetIsPainted()

    val bar = bottomPixel()
    assertTrue(
      bar.isBarColor(),
      "An inline sheet drew over the bar composed after it: $bar",
    )
  }

  @Test
  fun windowHostedSheet_drawsOverASiblingComposedAfterIt() = runComposeUiTest {
    setContent { SheetUnderABar(FlexibleSheetHost.Window) }
    waitForIdle()

    // The sheet lives in a popup window, which is exactly what puts it above the app content.
    assertTrue(popupNodeCount() > 0, "A window hosted sheet was not put in a popup window")
    assertSheetIsPainted()

    val bar = bottomPixel()
    assertTrue(
      bar.isSheetColor(),
      "A window hosted sheet was covered by a sibling composed after it: $bar",
    )
  }

  /**
   * The number of popup nodes in the tree. A window hosted sheet contributes more than one: the
   * platform popup marks its own root and the library marks the box it puts inside it.
   */
  private fun ComposeUiTest.popupNodeCount(): Int =
    onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.IsPopup))
      .fetchSemanticsNodes()
      .size

  /**
   * Fails unless the sheet is painted a little below its top edge, where the bar never reaches.
   * Without this, a sheet that never rendered would satisfy the inline expectation for free.
   */
  private fun ComposeUiTest.assertSheetIsPainted() {
    val pixels = onNodeWithTag(RootTag).captureToImage().toPixelMap()
    val sheet = pixels[pixels.width / 2, pixels.height / 2 + 20]
    assertTrue(sheet.isSheetColor(), "The sheet was not painted below its top edge: $sheet")
  }

  /** The bottom row of the window, where the bar overlaps the sheet. */
  private fun ComposeUiTest.bottomPixel(): Color {
    val pixels = onNodeWithTag(RootTag).captureToImage().toPixelMap()
    return pixels[pixels.width / 2, pixels.height - 2]
  }
}

/**
 * A non-modal sheet covering the lower half of the window with an opaque bar composed after it, the
 * way an app composes a bottom app bar, a navigation drawer or a sticky footer over its content.
 */
@Composable
private fun SheetUnderABar(sheetHost: FlexibleSheetHost) {
  Box(Modifier.fillMaxSize().background(Color.White).testTag(RootTag)) {
    val sheetState = rememberFlexibleBottomSheetState(
      isModal = false,
      skipHiddenState = true,
      skipSlightlyExpanded = true,
      initialValue = FlexibleSheetValue.IntermediatelyExpanded,
      flexibleSheetSize = FlexibleSheetSize(
        fullyExpanded = 1f,
        intermediatelyExpanded = 0.5f,
      ),
      sheetHost = sheetHost,
    )
    FlexibleBottomSheet(
      onDismissRequest = {},
      sheetState = sheetState,
      containerColor = SheetColor,
      // A square sheet, so the sampled pixels cannot fall outside a rounded corner.
      shape = RectangleShape,
      dragHandle = null,
    ) {
      Box(Modifier.fillMaxWidth().height(80.dp))
    }

    Box(
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .fillMaxWidth()
        .height(60.dp)
        .background(BarColor),
    )
  }
}

private const val RootTag = "root"

/** Two opaque colors that cannot be confused with each other or with the white background. */
private val SheetColor = Color.Blue
private val BarColor = Color.Green

private fun Color.isSheetColor(): Boolean = blue > 0.5f && red < 0.5f && green < 0.5f

private fun Color.isBarColor(): Boolean = green > 0.5f && red < 0.5f && blue < 0.5f
