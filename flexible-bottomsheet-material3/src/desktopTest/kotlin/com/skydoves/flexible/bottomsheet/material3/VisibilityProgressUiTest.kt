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
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.skydoves.flexible.core.FlexibleSheetSize
import com.skydoves.flexible.core.FlexibleSheetState
import com.skydoves.flexible.core.FlexibleSheetValue
import com.skydoves.flexible.core.rememberFlexibleBottomSheetState
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * End-to-end tests for [FlexibleSheetState.visibilityProgress] (feature request #57), verifying the
 * value is produced correctly from a real composition (not just the pure helper).
 */
@OptIn(ExperimentalTestApi::class)
class VisibilityProgressUiTest {

  @Test
  fun visibilityProgress_isAboutHalf_atIntermediateState() = runComposeUiTest {
    var state: FlexibleSheetState? = null
    setContent {
      Box(Modifier.fillMaxSize()) {
        val sheetState = rememberFlexibleBottomSheetState(
          isModal = true,
          skipSlightlyExpanded = true,
          initialValue = FlexibleSheetValue.IntermediatelyExpanded,
          flexibleSheetSize = FlexibleSheetSize(
            fullyExpanded = 1f,
            intermediatelyExpanded = 0.5f,
          ),
        )
        state = sheetState
        FlexibleBottomSheet(onDismissRequest = {}, sheetState = sheetState) {
          Box(Modifier.fillMaxWidth().height(200.dp))
        }
      }
    }
    waitForIdle()

    val progress = state!!.visibilityProgress
    val aboutHalf = progress in 0.3f..0.7f
    assertTrue(
      aboutHalf,
      "Expected about 0.5 visibility progress at IntermediatelyExpanded, was $progress",
    )
  }

  @Test
  fun visibilityProgress_isNearOne_atFullyExpandedState() = runComposeUiTest {
    var state: FlexibleSheetState? = null
    setContent {
      Box(Modifier.fillMaxSize()) {
        val sheetState = rememberFlexibleBottomSheetState(
          isModal = true,
          skipSlightlyExpanded = true,
          skipIntermediatelyExpanded = true,
          initialValue = FlexibleSheetValue.FullyExpanded,
          flexibleSheetSize = FlexibleSheetSize(fullyExpanded = 1f),
        )
        state = sheetState
        FlexibleBottomSheet(onDismissRequest = {}, sheetState = sheetState) {
          Box(Modifier.fillMaxWidth().height(200.dp))
        }
      }
    }
    waitForIdle()

    val progress = state!!.visibilityProgress
    val nearOne = progress >= 0.9f
    assertTrue(nearOne, "Expected visibility progress near 1.0 at FullyExpanded, was $progress")
  }
}
