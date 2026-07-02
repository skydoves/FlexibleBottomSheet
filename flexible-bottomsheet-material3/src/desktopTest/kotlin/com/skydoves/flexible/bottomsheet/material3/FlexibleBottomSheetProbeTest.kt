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
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.skydoves.flexible.core.FlexibleSheetSize
import com.skydoves.flexible.core.FlexibleSheetValue
import com.skydoves.flexible.core.rememberFlexibleBottomSheetState
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class FlexibleBottomSheetProbeTest {

  @Test
  fun modalSheetContent_isComposed_andReachable_inPopup() = runComposeUiTest {
    setContent {
      Box(Modifier.fillMaxSize()) {
        val state = rememberFlexibleBottomSheetState(
          isModal = true,
          skipSlightlyExpanded = true,
          initialValue = FlexibleSheetValue.IntermediatelyExpanded,
          flexibleSheetSize = FlexibleSheetSize(
            fullyExpanded = 1f,
            intermediatelyExpanded = 0.5f,
          ),
        )
        FlexibleBottomSheet(
          onDismissRequest = {},
          sheetState = state,
        ) {
          Box(Modifier.height(200.dp)) {
            Text("probe-sheet-content")
          }
        }
      }
    }
    waitForIdle()
    // If this passes, popup content is part of the testable semantics tree on desktop.
    onNodeWithText("probe-sheet-content").assertExists()
  }
}
