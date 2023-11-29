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
package com.skydoves.flexiblebottomsheetdemo

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skydoves.flexible.bottomsheet.material.FlexibleBottomSheet
import com.skydoves.flexible.core.FlexibleSheetSize
import com.skydoves.flexible.core.FlexibleSheetValue
import com.skydoves.flexible.core.rememberFlexibleBottomSheetState

@Composable
fun FlexibleBottomSheetSample1(
  onDismissRequest: () -> Unit,
) {
  var currentSheetTarget by remember { mutableStateOf(FlexibleSheetValue.IntermediatelyExpanded) }

  FlexibleBottomSheet(
    onDismissRequest = onDismissRequest,
    sheetState = rememberFlexibleBottomSheetState(
      flexibleSheetSize = FlexibleSheetSize(
        fullyExpanded = 0.9f,
        intermediatelyExpanded = 0.5f,
        slightlyExpanded = 0.15f,
      ),
      isModal = false,
      skipSlightlyExpanded = false,
    ),
    onTargetChanges = { sheetValue ->
      currentSheetTarget = sheetValue
    },
    containerColor = Color.Black,
  ) {
    Text(
      modifier = Modifier
        .fillMaxWidth()
        .padding(8.dp),
      text = "This is Flexible Bottom Sheet",
      textAlign = TextAlign.Center,
      color = Color.White,
      fontSize = when (currentSheetTarget) {
        FlexibleSheetValue.IntermediatelyExpanded -> 16.sp
        FlexibleSheetValue.FullyExpanded -> 23.sp
        else -> 12.sp
      },
    )
  }
}
