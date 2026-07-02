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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.skydoves.flexible.core.FlexibleSheetSize
import com.skydoves.flexible.core.rememberFlexibleBottomSheetState
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class WrapContentNonModalTest {

  private val sheetMatcher =
    SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, "Bottom Sheet")

  /**
   * Regression test for #95: `FlexibleSheetSize.WrapContent` must size a non-modal sheet to its
   * content height instead of leaving it almost hidden.
   */
  @Test
  fun nonModalWrapContent_sizesSheetToContent() = runComposeUiTest {
    val contentHeight = 220.dp
    setContent {
      Box(Modifier.fillMaxSize()) {
        val state = rememberFlexibleBottomSheetState(
          isModal = false,
          skipSlightlyExpanded = true,
          skipIntermediatelyExpanded = true,
          containSystemBars = true,
          flexibleSheetSize = FlexibleSheetSize(fullyExpanded = FlexibleSheetSize.WrapContent),
        )
        FlexibleBottomSheet(
          onDismissRequest = {},
          sheetState = state,
          dragHandle = null,
        ) {
          Box(Modifier.fillMaxWidth().height(contentHeight))
        }
      }
    }
    waitForIdle()

    val bounds = onNode(sheetMatcher).getUnclippedBoundsInRoot()
    val sheetHeight = bounds.bottom - bounds.top
    // The sheet should be roughly as tall as its content (220.dp), not collapsed to a sliver.
    val tallEnough = sheetHeight >= 150.dp
    assertTrue(
      tallEnough,
      "Non-modal WrapContent sheet height was $sheetHeight, expected at least 150.dp",
    )
  }
}
