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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import com.skydoves.flexible.core.FlexibleSheetSize
import com.skydoves.flexible.core.rememberFlexibleBottomSheetState
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class DynamicSheetSizeTest {

  private val sheetMatcher =
    SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, "Bottom Sheet")

  private fun DpRect.height() = bottom - top

  /**
   * Regression test for #93: changing the [FlexibleSheetSize] at runtime should recompute the
   * anchors and resize the (already displayed) sheet instead of ignoring the new value.
   */
  @Test
  fun sheetSize_updatesDynamically_whenRatioChanges() = runComposeUiTest {
    val ratio = mutableStateOf(0.6f)
    setContent {
      Box(Modifier.fillMaxSize()) {
        val currentRatio by ratio
        val state = rememberFlexibleBottomSheetState(
          isModal = false,
          skipSlightlyExpanded = true,
          containSystemBars = true,
          flexibleSheetSize = FlexibleSheetSize(
            fullyExpanded = 1f,
            intermediatelyExpanded = currentRatio,
          ),
        )
        FlexibleBottomSheet(
          onDismissRequest = {},
          sheetState = state,
          dragHandle = null,
        ) {
          Box(Modifier.fillMaxWidth().height(40.dp))
        }
      }
    }
    waitForIdle()

    val initialHeight = onNode(sheetMatcher).getUnclippedBoundsInRoot().height()

    // Shrink the intermediately-expanded ratio at runtime.
    ratio.value = 0.25f
    waitForIdle()

    val updatedHeight = onNode(sheetMatcher).getUnclippedBoundsInRoot().height()

    val shrank = updatedHeight < initialHeight * 0.7f
    assertTrue(
      shrank,
      "Sheet did not resize dynamically: initial=$initialHeight updated=$updatedHeight",
    )
  }
}
