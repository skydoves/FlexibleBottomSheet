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
    calculateFlexibleSheetAnchor(
      value = value,
      isModal = isModal,
      measuredSheetHeight = sheetSize.height,
      sheetFullHeight = sheetFullHeight,
      sheetConstraintHeight = sheetConstraintHeight,
      screenMaxHeight = screenMaxHeight,
      flexibleSheetSize = flexibleSheetSize,
      contentHeight = contentHeight,
      skipIntermediatelyExpanded = sheetState.skipIntermediatelyExpanded,
      skipSlightlyExpanded = sheetState.skipSlightlyExpanded,
    )
  }

/**
 * Computes the swipe anchor offset for a single [value], or `null` when that state is unavailable.
 *
 * Offsets grow downward: `0` is the most expanded position a sheet can reach and the largest anchor
 * is the least visible one. Modal sheets fill the whole screen and slide within it, so their anchors
 * are measured from the top of the screen. Non-modal sheets are only as tall as their fully expanded
 * size, so their anchors are measured from the top of that sheet-sized travel.
 *
 * @param value The state to compute the anchor for.
 * @param isModal Whether the sheet is modal.
 * @param measuredSheetHeight The measured height of the sheet, in pixels. Used only to tell whether
 * the sheet has been laid out at least once.
 * @param sheetFullHeight The height of the fully expanded sheet, in pixels.
 * @param sheetConstraintHeight The height of the sheet container, in pixels.
 * @param screenMaxHeight The height of the screen, in pixels.
 * @param flexibleSheetSize The size ratios of the sheet.
 * @param contentHeight The measured content height, in pixels, used to resolve wrap content sizes.
 * @param skipIntermediatelyExpanded Whether the intermediately expanded state is disabled.
 * @param skipSlightlyExpanded Whether the slightly expanded state is disabled.
 */
internal fun calculateFlexibleSheetAnchor(
  value: FlexibleSheetValue,
  isModal: Boolean,
  measuredSheetHeight: Int,
  sheetFullHeight: Float,
  sheetConstraintHeight: Float,
  screenMaxHeight: Float,
  flexibleSheetSize: FlexibleSheetSize,
  contentHeight: Float,
  skipIntermediatelyExpanded: Boolean,
  skipSlightlyExpanded: Boolean,
): Float? {
  // Resolve sizes considering wrap content mode
  val resolvedFullyExpanded = flexibleSheetSize.fullyExpanded
    .resolveSheetSize(screenMaxHeight, contentHeight)
  val resolvedIntermediatelyExpanded = flexibleSheetSize.intermediatelyExpanded
    .resolveSheetSize(screenMaxHeight, contentHeight)
  val resolvedSlightlyExpanded = flexibleSheetSize.slightlyExpanded
    .resolveSheetSize(screenMaxHeight, contentHeight)

  return if (isModal) {
    when (value) {
      FlexibleSheetValue.Hidden -> sheetConstraintHeight - 0f

      FlexibleSheetValue.FullyExpanded -> if (measuredSheetHeight != 0) {
        max(0f, screenMaxHeight - screenMaxHeight * resolvedFullyExpanded)
      } else {
        null
      }

      FlexibleSheetValue.IntermediatelyExpanded -> when {
        measuredSheetHeight < screenMaxHeight * resolvedIntermediatelyExpanded -> null
        skipIntermediatelyExpanded -> null
        else -> screenMaxHeight - screenMaxHeight * resolvedIntermediatelyExpanded
      }

      FlexibleSheetValue.SlightlyExpanded -> when {
        measuredSheetHeight < screenMaxHeight * resolvedSlightlyExpanded -> null
        skipSlightlyExpanded -> null
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

    when (value) {
      FlexibleSheetValue.Hidden -> sheetFullHeight

      // The fully expanded anchor is the top of the sheet's own travel, which is 0 by definition:
      // every other anchor is expressed as `sheetFullHeight - visibleHeight`, and at the fully
      // expanded state the visible height *is* sheetFullHeight.
      //
      // This used to be derived from the measured sheet height instead. A non-modal sheet is only
      // as tall as its currently visible part while idle, so the measured height was the visible
      // height of whatever state the sheet was resting in, and at SlightlyExpanded the fully
      // expanded anchor collapsed onto the slightly expanded one. As soon as a drag started, the
      // container grew to the full height, every anchor moved, and the anchor change handler
      // re-targeted the sheet mid-gesture: the downward jump reported in #68 (worst with
      // `fullyExpanded = 1.0f`, where the container grows the most) and part of the drag flicker
      // in #67. The measured height is now only an "already laid out" guard.
      FlexibleSheetValue.FullyExpanded -> if (measuredSheetHeight != 0) {
        0f
      } else {
        null
      }

      FlexibleSheetValue.IntermediatelyExpanded -> when {
        sheetFullHeight < expectedSheetSize -> null
        skipIntermediatelyExpanded -> null
        else -> sheetFullHeight - expectedSheetSize
      }

      FlexibleSheetValue.SlightlyExpanded -> when {
        sheetFullHeight < expectedSheetSize -> null
        skipSlightlyExpanded -> null
        else -> sheetFullHeight - expectedSheetSize
      }
    }
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
