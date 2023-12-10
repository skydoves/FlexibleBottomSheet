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
package com.skydoves.flexible.core

import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics

@Composable
public fun Scrim(
  color: Color,
  onDismissRequest: () -> Unit,
  visible: Boolean,
) {
  if (color.isSpecified) {
    val alpha by animateFloatAsState(
      targetValue = if (visible) 1f else 0f,
      animationSpec = TweenSpec(),
      label = "Bottom Sheet Scrim",
    )
    val dismissSheet = if (visible) {
      Modifier
        .pointerInput(onDismissRequest) {
          detectTapGestures {
            onDismissRequest()
          }
        }
        .clearAndSetSemantics {}
    } else {
      Modifier
    }
    Canvas(
      Modifier
        .fillMaxSize()
        .then(dismissSheet),
    ) {
      drawRect(color = color, alpha = alpha)
    }
  }
}
