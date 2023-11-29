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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skydoves.flexible.bottomsheet.material3.FlexibleBottomSheet
import com.skydoves.flexible.core.FlexibleSheetSize
import com.skydoves.flexible.core.FlexibleSheetState
import com.skydoves.flexible.core.rememberFlexibleBottomSheetState
import com.skydoves.flexiblebottomsheetdemo.mocks.MockUtils
import com.skydoves.landscapist.glide.GlideImage

@Composable
fun FlexibleBottomSheetSample2(
  onDismissRequest: () -> Unit,
) {
  val sheetState = rememberFlexibleBottomSheetState(
    flexibleSheetSize = FlexibleSheetSize(
      fullyExpanded = 0.9f,
      intermediatelyExpanded = 0.5f,
      slightlyExpanded = 0.18f,
    ),
    isModal = false,
    skipSlightlyExpanded = false,
    containSystemBars = false,
  )

  FlexibleBottomSheet(
    onDismissRequest = onDismissRequest,
    sheetState = sheetState,
    containerColor = Color.Black,
  ) {
    BottomSheetContent(sheetState = sheetState)
  }
}

@Composable
private fun BottomSheetContent(
  sheetState: FlexibleSheetState,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(12.dp),
  ) {
    GlideImage(
      modifier = Modifier
        .size(64.dp)
        .clip(CircleShape),
      imageModel = { MockUtils.getMockPoster().poster },
    )

    Spacer(modifier = Modifier.width(12.dp))

    Column(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f),
      verticalArrangement = Arrangement.Center,
    ) {
      Text(
        modifier = Modifier.padding(bottom = 1.dp),
        text = "New York",
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White,
      )

      Text(
        modifier = Modifier.padding(vertical = 1.dp),
        text = "100K+",
        fontSize = 14.sp,
        color = Color(0xFF03A9F4),
      )

      Text(
        modifier = Modifier.padding(vertical = 1.dp),
        text = "New York, USA",
        fontSize = 14.sp,
        color = Color(0xFF868A8E),
      )
    }
  }

  Spacer(modifier = Modifier.height(12.dp))

  LazyVerticalGrid(
    modifier = Modifier
      .fillMaxSize(),
    columns = GridCells.Fixed(2),
  ) {
    items(items = MockUtils.getMockPosters(), key = { it.name }) {
      GlideImage(
        modifier = Modifier
          .fillMaxWidth()
          .height(230.dp)
          .padding(2.dp)
          .clip(RoundedCornerShape(8.dp))
          .border(BorderStroke(2.dp, Color.Green)),
        imageModel = { it.poster },
      )
    }
  }
}
