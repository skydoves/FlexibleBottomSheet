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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.skydoves.flexible.core.FlexibleSheetHost
import com.skydoves.flexible.core.FlexibleSheetSize
import com.skydoves.flexible.core.FlexibleSheetValue
import com.skydoves.flexible.core.rememberFlexibleBottomSheetState
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Regression tests for the touch pass-through of an inline non-modal sheet (issues #71 / #37).
 *
 * The inline container installs no pointer input of its own, and a layout node without pointer
 * input is not a hit test target in Compose. That, and nothing else, is what gives a non-modal
 * inline sheet real pass-through: the app behind it stays usable everywhere the sheet is not
 * painted, the way a Google Maps style sheet has to behave, without any window level flag. Giving
 * the container a `clickable`, a `pointerInput` or a full-size scrim for a non-modal sheet would
 * break it, so both halves are pinned: a click that misses the sheet reaches the content behind,
 * and a click that lands on it does not.
 *
 * Note: this does not contrast the two hosts. A non-modal desktop `Popup` is non-focusable and only
 * as large as the sheet, so a window hosted sheet passes touches through on this target as well;
 * the contrast only shows on Android, where the popup window owns the touch region. What the test
 * guards is that the inline container never grows a hit test target of its own.
 */
@OptIn(ExperimentalTestApi::class)
class InlineNonModalTouchPassThroughTest {

  @Test
  fun clickBesideAnInlineNonModalSheet_reachesTheContentBehind() = runComposeUiTest {
    var clicksBehind = 0
    setContent { NonModalInlineSheetOverClickableContent { clicksBehind++ } }
    waitForIdle()

    // A sheet that never rendered would let every click through, which would make this vacuous.
    onNode(SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, "Bottom Sheet"))
      .assertIsDisplayed()

    clickAt(verticalFraction = 0.1f)
    waitForIdle()

    assertEquals(1, clicksBehind, "The inline sheet swallowed a click that never touched it")
  }

  @Test
  fun clickOnAnInlineNonModalSheet_doesNotReachTheContentBehind() = runComposeUiTest {
    var clicksBehind = 0
    setContent { NonModalInlineSheetOverClickableContent { clicksBehind++ } }
    waitForIdle()

    clickAt(verticalFraction = 0.95f)
    waitForIdle()

    assertEquals(0, clicksBehind, "A click on the inline sheet fell through to the content behind")
  }

  /** Clicks the window at [verticalFraction] of its height, horizontally centered. */
  private fun ComposeUiTest.clickAt(verticalFraction: Float) {
    onNodeWithTag(RootTag).performTouchInput {
      click(Offset(width / 2f, height * verticalFraction))
    }
  }
}

/**
 * An inline non-modal sheet covering the lower half of the window, over a full-size clickable node.
 */
@Composable
private fun NonModalInlineSheetOverClickableContent(onClickBehind: () -> Unit) {
  Box(Modifier.fillMaxSize().testTag(RootTag)) {
    Box(Modifier.fillMaxSize().background(Color.White).clickable { onClickBehind() })

    val sheetState = rememberFlexibleBottomSheetState(
      isModal = false,
      skipHiddenState = true,
      skipSlightlyExpanded = true,
      initialValue = FlexibleSheetValue.IntermediatelyExpanded,
      flexibleSheetSize = FlexibleSheetSize(
        fullyExpanded = 1f,
        intermediatelyExpanded = 0.5f,
      ),
      sheetHost = FlexibleSheetHost.Inline,
    )
    FlexibleBottomSheet(
      onDismissRequest = {},
      sheetState = sheetState,
      containerColor = Color.Blue,
      shape = RectangleShape,
      dragHandle = null,
    ) {
      Box(Modifier.fillMaxWidth().height(80.dp))
    }
  }
}

private const val RootTag = "root"
