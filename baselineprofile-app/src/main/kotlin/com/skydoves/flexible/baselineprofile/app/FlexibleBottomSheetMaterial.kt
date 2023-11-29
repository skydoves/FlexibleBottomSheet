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
package com.skydoves.flexible.baselineprofile.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.skydoves.flexible.bottomsheet.material.FlexibleBottomSheet
import com.skydoves.flexible.core.FlexibleSheetSize
import com.skydoves.flexible.core.rememberFlexibleBottomSheetState

@Composable
fun FlexibleBottomSheetMaterial() {
  FlexibleBottomSheet(
    onDismissRequest = { },
    sheetState = rememberFlexibleBottomSheetState(
      flexibleSheetSize = FlexibleSheetSize(
        fullyExpanded = 0.9f,
        intermediatelyExpanded = 0.5f,
        slightlyExpanded = 0.15f,
      ),
      isModal = false,
      skipSlightlyExpanded = false,
    ),
    onTargetChanges = {},
    containerColor = Color.Black,
    shape = RoundedCornerShape(
      topStart = 16.dp,
      topEnd = 16.dp,
    ),
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(150.dp),
    ) {
      Text(
        modifier = Modifier.align(Alignment.TopCenter),
        text = "Hello, this is FlexibleBottomSheet",
        color = Color.White,
      )
    }
  }
}
