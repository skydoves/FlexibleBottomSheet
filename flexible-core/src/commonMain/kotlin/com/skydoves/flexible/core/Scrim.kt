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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.clearAndSetSemantics

/**
 * Draws the modal scrim behind a bottom sheet and fades it with the sheet's own movement.
 *
 * The alpha is read from the live sheet offset inside the draw phase, so dragging the sheet only
 * triggers a redraw and the scrim can never lag behind, overshoot, or blink when a fast fling makes
 * the sheet re-evaluate its target value several times (#59).
 *
 * @param color Color of the scrim. Nothing is drawn when the color is unspecified.
 * @param onDismissRequest Invoked when the scrim is tapped.
 * @param sheetState The state of the sheet this scrim belongs to.
 */
@Composable
public fun Scrim(
  color: Color,
  onDismissRequest: () -> Unit,
  sheetState: FlexibleSheetState,
) {
  if (!color.isSpecified) return

  // Invoked from the draw phase only, so the offset and anchor reads land in the draw scope: the
  // sheet moving repaints the scrim without recomposing anything.
  val scrimProgress = {
    calculateScrimProgress(
      anchors = sheetState.swipeableState.anchors,
      offset = sheetState.swipeableState.offsetOrNull,
    )
  }

  val currentOnDismissRequest by rememberUpdatedState(onDismissRequest)
  val dismissible = sheetState.targetValue != FlexibleSheetValue.Hidden
  val dismissSheet = if (dismissible) {
    Modifier
      .pointerInput(Unit) {
        detectTapGestures {
          currentOnDismissRequest()
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
    drawRect(color = color, alpha = scrimProgress())
  }
}

/**
 * Draws a scrim that cross-fades between fully opaque and fully transparent.
 *
 * Prefer the [Scrim] overload that takes a [FlexibleSheetState]: it follows the sheet offset instead
 * of animating a boolean, which keeps the scrim in sync with the gesture.
 *
 * @param color Color of the scrim. Nothing is drawn when the color is unspecified.
 * @param onDismissRequest Invoked when the scrim is tapped.
 * @param visible Whether the scrim should be shown.
 */
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
