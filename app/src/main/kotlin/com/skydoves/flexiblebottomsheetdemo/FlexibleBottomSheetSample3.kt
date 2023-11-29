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

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skydoves.flexible.bottomsheet.material.FlexibleBottomSheet
import com.skydoves.flexible.core.FlexibleSheetSize
import com.skydoves.flexible.core.FlexibleSheetValue
import com.skydoves.flexible.core.rememberFlexibleBottomSheetState
import com.skydoves.flexiblebottomsheetdemo.mocks.MockUtils
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.animation.crossfade.CrossfadePlugin
import com.skydoves.landscapist.components.rememberImageComponent
import com.skydoves.landscapist.glide.GlideImage
import com.skydoves.orbital.Orbital
import com.skydoves.orbital.animateBounds
import com.skydoves.orbital.rememberMovableContentOf

@Composable
fun FlexibleBottomSheetSample3(
  onDismissRequest: () -> Unit,
) {
  var currentSheetTarget by remember { mutableStateOf(FlexibleSheetValue.IntermediatelyExpanded) }

  FlexibleBottomSheet(
    onDismissRequest = onDismissRequest,
    sheetState = rememberFlexibleBottomSheetState(
      flexibleSheetSize = FlexibleSheetSize(
        fullyExpanded = 0.9f,
        slightlyExpanded = 0.18f,
      ),
      isModal = false,
      skipIntermediatelyExpanded = true,
      skipSlightlyExpanded = false,
    ),
    onTargetChanges = { sheetValue ->
      currentSheetTarget = sheetValue
    },
    containerColor = Color.Black,
  ) {
    Orbital(modifier = Modifier.fillMaxSize()) {
      val sizeAnim = spring<IntSize>(stiffness = Spring.StiffnessLow)
      val positionAnim = spring<IntOffset>(stiffness = Spring.StiffnessLow)
      val image = rememberMovableContentOf {
        GlideImage(
          imageModel = { MockUtils.getMockPoster().poster },
          component = rememberImageComponent {
            +CrossfadePlugin()
          },
          modifier = Modifier
            .padding(10.dp)
            .animateBounds(
              modifier = if (currentSheetTarget == FlexibleSheetValue.SlightlyExpanded) {
                Modifier.size(80.dp)
              } else {
                Modifier
                  .size(200.dp)
                  .align(Alignment.CenterHorizontally)
              },
              sizeAnimationSpec = sizeAnim,
              positionAnimationSpec = positionAnim,
            )
            .clip(RoundedCornerShape(12.dp)),
          imageOptions = ImageOptions(requestSize = IntSize(200, 200)),
        )
      }

      val title = rememberMovableContentOf {
        Column(
          modifier = Modifier
            .padding(10.dp)
            .animateBounds(
              modifier = Modifier,
              sizeAnimationSpec = sizeAnim,
              positionAnimationSpec = positionAnim,
            ),
        ) {
          Text(
            text = MockUtils.getMockPoster().name,
            fontSize = if (currentSheetTarget == FlexibleSheetValue.SlightlyExpanded) {
              18.sp
            } else {
              26.sp
            },
            color = Color.White,
            fontWeight = FontWeight.Bold,
          )

          Text(
            text = MockUtils.getMockPoster().description,
            color = Color.LightGray,
            fontSize = 12.sp,
            maxLines = if (currentSheetTarget == FlexibleSheetValue.SlightlyExpanded) {
              3
            } else {
              20
            },
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.Bold,
          )
        }
      }

      Orbital(modifier = Modifier.fillMaxSize()) {
        if (currentSheetTarget == FlexibleSheetValue.SlightlyExpanded) {
          Row(
            modifier = Modifier.fillMaxSize(),
          ) {
            image()
            title()
          }
        } else {
          Column(
            modifier = Modifier.fillMaxSize(),
          ) {
            image()
            title()
          }
        }
      }
    }
  }
}
