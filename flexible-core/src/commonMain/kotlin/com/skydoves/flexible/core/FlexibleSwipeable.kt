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

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.CoroutineScope
import kotlin.math.max
import kotlin.math.roundToInt

@InternalFlexibleApi
public fun Modifier.flexibleBottomSheetSwipeable(
  sheetState: FlexibleSheetState,
  flexibleSheetSize: FlexibleSheetSize,
  anchorChangeHandler: AnchorChangeHandler<FlexibleSheetValue>,
  sheetFullHeight: Float,
  sheetConstraintHeight: Float,
  screenMaxHeight: Float,
  isModal: Boolean,
  contentHeight: Float = 0f,
  onDragStarted: suspend CoroutineScope.(startedPosition: Offset) -> Unit = {},
  onDragStopped: CoroutineScope.(velocity: Float) -> Unit,
): Modifier = draggable(
  state = sheetState.swipeableState.swipeDraggableState,
  orientation = Orientation.Vertical,
  enabled = sheetState.isVisible,
  startDragImmediately = sheetState.swipeableState.isAnimationRunning,
  onDragStarted = onDragStarted,
  onDragStopped = onDragStopped,
)
  .swipeAnchors(
    state = sheetState.swipeableState,
    anchorChangeHandler = anchorChangeHandler,
    possibleValues = setOf(
      FlexibleSheetValue.Hidden,
      FlexibleSheetValue.IntermediatelyExpanded,
      FlexibleSheetValue.SlightlyExpanded,
      FlexibleSheetValue.FullyExpanded,
    ),
  ) { value, sheetSize ->
    // Resolve sizes considering wrap content mode
    val resolvedFullyExpanded = flexibleSheetSize.fullyExpanded
      .resolveSheetSize(screenMaxHeight, contentHeight)
    val resolvedIntermediatelyExpanded = flexibleSheetSize.intermediatelyExpanded
      .resolveSheetSize(screenMaxHeight, contentHeight)
    val resolvedSlightlyExpanded = flexibleSheetSize.slightlyExpanded
      .resolveSheetSize(screenMaxHeight, contentHeight)

    if (isModal) {
      when (value) {
        FlexibleSheetValue.Hidden -> sheetConstraintHeight - 0f

        FlexibleSheetValue.FullyExpanded -> if (sheetSize.height != 0) {
          max(0f, screenMaxHeight - screenMaxHeight * resolvedFullyExpanded)
        } else {
          null
        }

        FlexibleSheetValue.IntermediatelyExpanded -> when {
          sheetSize.height < screenMaxHeight * resolvedIntermediatelyExpanded -> null
          sheetState.skipIntermediatelyExpanded -> null
          else -> screenMaxHeight - screenMaxHeight * resolvedIntermediatelyExpanded
        }

        FlexibleSheetValue.SlightlyExpanded -> when {
          sheetSize.height < screenMaxHeight * resolvedSlightlyExpanded -> null
          sheetState.skipSlightlyExpanded -> null
          else -> screenMaxHeight - screenMaxHeight * resolvedSlightlyExpanded
        }
      }
    } else {
      val expectedSheetSize = when (value) {
        FlexibleSheetValue.Hidden -> 0f

        FlexibleSheetValue.FullyExpanded -> screenMaxHeight * resolvedFullyExpanded

        FlexibleSheetValue.IntermediatelyExpanded ->
          screenMaxHeight * resolvedIntermediatelyExpanded

        FlexibleSheetValue.SlightlyExpanded -> screenMaxHeight * resolvedSlightlyExpanded
      }.roundToInt()

      val expectedSize = when (value) {
        FlexibleSheetValue.Hidden -> sheetFullHeight

        FlexibleSheetValue.FullyExpanded -> if (sheetSize.height != 0) {
          max(0f, sheetFullHeight - sheetSize.height)
        } else {
          null
        }

        FlexibleSheetValue.IntermediatelyExpanded -> {
          when {
            sheetFullHeight < expectedSheetSize -> null
            sheetState.skipIntermediatelyExpanded -> null
            else -> sheetFullHeight - expectedSheetSize
          }
        }

        FlexibleSheetValue.SlightlyExpanded -> {
          when {
            sheetFullHeight < expectedSheetSize -> null
            sheetState.skipSlightlyExpanded -> null
            else -> sheetFullHeight - expectedSheetSize
          }
        }
      }

      expectedSize
    }
  }

@InternalFlexibleApi
public fun flexibleBottomSheetAnchorChangeHandler(
  state: FlexibleSheetState,
  animateTo: (target: FlexibleSheetValue, velocity: Float) -> Unit,
  snapTo: (target: FlexibleSheetValue) -> Unit,
): AnchorChangeHandler<FlexibleSheetValue> =
  AnchorChangeHandler { previousTarget, previousAnchors, newAnchors ->
    val previousTargetOffset = previousAnchors[previousTarget]
    val newTarget = when (previousTarget) {
      FlexibleSheetValue.Hidden -> FlexibleSheetValue.Hidden
      FlexibleSheetValue.IntermediatelyExpanded,
      FlexibleSheetValue.SlightlyExpanded,
      FlexibleSheetValue.FullyExpanded,
      -> {
        // If the previous target (initialValue) is available in new anchors, preserve it
        if (newAnchors.containsKey(previousTarget)) {
          previousTarget
        } else {
          // Fallback to the best available state if previous target is not available
          val hasIntermediatelyExpandedState =
            newAnchors.containsKey(FlexibleSheetValue.IntermediatelyExpanded)
          val hasSlightlyExpandedState = newAnchors.containsKey(FlexibleSheetValue.SlightlyExpanded)
          val hasFullyExpandedState = newAnchors.containsKey(FlexibleSheetValue.FullyExpanded)
          if (hasIntermediatelyExpandedState) {
            FlexibleSheetValue.IntermediatelyExpanded
          } else if (hasSlightlyExpandedState) {
            FlexibleSheetValue.SlightlyExpanded
          } else if (hasFullyExpandedState) {
            FlexibleSheetValue.FullyExpanded
          } else {
            FlexibleSheetValue.Hidden
          }
        }
      }
    }

    val newTargetOffset = newAnchors.getValue(newTarget)
    if (newTargetOffset != previousTargetOffset) {
      if (state.swipeableState.isAnimationRunning) {
        // Re-target the animation to the new offset if it changed
        animateTo(newTarget, state.swipeableState.lastVelocity)
      } else if (previousAnchors.isEmpty() && newTarget == FlexibleSheetValue.Hidden) {
        // Initial anchor setup with Hidden state - use animateTo for non-modal sheet sizing
        animateTo(newTarget, state.swipeableState.lastVelocity)
      } else {
        // Snap to the new offset value without animation
        // This applies when user sets a visible initialValue
        snapTo(newTarget)
      }
    }
  }
