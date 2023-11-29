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

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.CancellationException

/**
 * State of a sheet composable, such as [FlexibleBottomSheet]
 *
 * Contains states relating to it's swipe position as well as animations between state values.
 *
 * @param skipIntermediatelyExpanded Whether the intermediately expanded state, if the sheet is large
 * enough, should be skipped. If true, the sheet will always expand to the [FlexibleSheetValue.FullyExpanded] state and move
 * @param skipSlightlyExpanded Whether the slightly expanded state, if the sheet is tall enough,
 * should be skipped. If true, the sheet will always expand to the [FlexibleSheetValue.IntermediatelyExpanded] or [FlexibleSheetValue.FullyExpanded] state and move to the
 * to the [FlexibleSheetValue.Hidden] state if available when hiding the sheet, either programmatically or by user
 * interaction.
 * @param flexibleSheetSize FlexibleSheetSize constraints the content size of [FlexibleBottomSheet] based on its states.
 * @param initialValue The initial value of the state.
 * @param confirmValueChange Optional callback invoked to confirm or veto a pending state change.
 * @param skipHiddenState Whether the hidden state should be skipped. If true, the sheet will always
 * @param isModal Determines if the bottom sheet should be modal. If set to true, the sheet will include a scrim overlaying the background and
 * will be dismissed upon touching outside of the sheet. If set to false, the bottom sheet allows interaction with the screen, permitting actions outside of the sheet.
 * expand to the [FlexibleSheetValue.FullyExpanded] state and move to the [FlexibleSheetValue.IntermediatelyExpanded] if available, either
 * programmatically or by user interaction.
 */
@Stable
public class FlexibleSheetState(
  public val skipIntermediatelyExpanded: Boolean,
  public val skipSlightlyExpanded: Boolean,
  public val flexibleSheetSize: FlexibleSheetSize,
  public val containSystemBars: Boolean,
  public val allowNestedScroll: Boolean,
  public val isModal: Boolean,
  public val animateSpec: AnimationSpec<Float>,
  initialValue: FlexibleSheetValue = FlexibleSheetValue.Hidden,
  confirmValueChange: (FlexibleSheetValue) -> Boolean = { true },
  public val skipHiddenState: Boolean = false,
) {
  init {
    if (skipIntermediatelyExpanded) {
      require(initialValue != FlexibleSheetValue.IntermediatelyExpanded) {
        "The initial value must not be set to IntermediatelyExpanded if " +
          "skipIntermediatelyExpanded is set to true."
      }
    }
    if (skipHiddenState) {
      require(initialValue != FlexibleSheetValue.Hidden) {
        "The initial value must not be set to Hidden if skipHiddenState is set to true."
      }
    }
  }

  /**
   * The current value of the state.
   *
   * If no swipe or animation is in progress, this corresponds to the state the bottom sheet is
   * currently in. If a swipe or an animation is in progress, this corresponds the state the sheet
   * was in before the swipe or animation started.
   */

  public val currentValue: FlexibleSheetValue get() = swipeableState.currentValue

  /**
   * The target value of the bottom sheet state.
   *
   * If a swipe is in progress, this is the value that the sheet would animate to if the
   * swipe finishes. If an animation is running, this is the target value of that animation.
   * Finally, if no swipe or animation is in progress, this is the same as the [currentValue].
   */
  public val targetValue: FlexibleSheetValue get() = swipeableState.targetValue

  /**
   * Whether the flexible bottom sheet is visible.
   */
  public val isVisible: Boolean
    get() = swipeableState.currentValue != FlexibleSheetValue.Hidden

  /**
   * Require the current offset (in pixels) of the bottom sheet.
   *
   * The offset will be initialized during the first measurement phase of the provided sheet
   * content.
   *
   * These are the phases:
   * Composition { -> Effects } -> Layout { Measurement -> Placement } -> Drawing
   *
   * During the first composition, an [IllegalStateException] is thrown. In subsequent
   * compositions, the offset will be derived from the anchors of the previous pass. Always prefer
   * accessing the offset from a LaunchedEffect as it will be scheduled to be executed the next
   * frame, after layout.
   *
   * @throws IllegalStateException If the offset has not been initialized yet
   */
  public fun requireOffset(): Float = swipeableState.requireOffset()

  /**
   * Whether the sheet has an expanded state defined.
   */
  public val hasFullyExpandedState: Boolean
    get() = swipeableState.hasAnchorForValue(FlexibleSheetValue.FullyExpanded)

  /**
   * Whether the flexible bottom sheet has a intermediately expanded state defined.
   */
  public val hasIntermediatelyExpandedState: Boolean
    get() = swipeableState.hasAnchorForValue(FlexibleSheetValue.IntermediatelyExpanded)

  /**
   * Whether the flexible bottom sheet has a slightly expanded state defined.
   */
  public val hasSlightlyExpandedState: Boolean
    get() = swipeableState.hasAnchorForValue(FlexibleSheetValue.SlightlyExpanded)

  private val _isAnimatingContent: MutableState<Boolean> = mutableStateOf(false)
  public val isAnimatingContent: State<Boolean> = _isAnimatingContent

  /**
   * Fully expand the bottom sheet with animation and suspend until it is fully expanded or
   * animation has been cancelled.
   * *
   * @throws [CancellationException] if the animation is interrupted
   */
  public suspend fun fullyExpand() {
    if (!isModal) {
      animateTo(targetValue = FlexibleSheetValue.FullyExpanded, animSpec = animateSpec)
      animateTo(targetValue = FlexibleSheetValue.FullyExpanded, animSpec = animateSpec)
    } else {
      animateTo(targetValue = FlexibleSheetValue.FullyExpanded, animSpec = animateSpec)
    }
  }

  /**
   * Animate the bottom sheet and suspend until it is intermediately expanded or animation has been
   * cancelled.
   * @throws [CancellationException] if the animation is interrupted
   * @throws [IllegalStateException] if [skipIntermediatelyExpanded] is set to true
   */
  public suspend fun intermediatelyExpand() {
    check(!skipIntermediatelyExpanded) {
      "Attempted to animate to intermediately expanded when skipIntermediatelyExpanded " +
        "was enabled. Set skipIntermediatelyExpanded to false to use this function."
    }
    if (!isModal) {
      animateTo(targetValue = FlexibleSheetValue.IntermediatelyExpanded, animSpec = animateSpec)
      animateTo(targetValue = FlexibleSheetValue.IntermediatelyExpanded, animSpec = animateSpec)
    } else {
      animateTo(targetValue = FlexibleSheetValue.IntermediatelyExpanded, animSpec = animateSpec)
    }
  }

  /**
   * Animate the bottom sheet and suspend until it is intermediately expanded or animation has been
   * cancelled.
   * @throws [CancellationException] if the animation is interrupted
   * @throws [IllegalStateException] if [skipIntermediatelyExpanded] is set to true
   */
  public suspend fun slightlyExpand() {
    check(!skipSlightlyExpanded) {
      "Attempted to animate to slightly expanded when skipSlightlyExpanded was enabled. Set" +
        " skipIntermediatelyExpanded to false to use this function."
    }
    if (!isModal) {
      animateTo(targetValue = FlexibleSheetValue.SlightlyExpanded, animSpec = animateSpec)
      animateTo(targetValue = FlexibleSheetValue.SlightlyExpanded, animSpec = animateSpec)
    } else {
      animateTo(targetValue = FlexibleSheetValue.SlightlyExpanded, animSpec = animateSpec)
    }
  }

  /**
   * Expand the bottom sheet with animation and suspend until it is [FlexibleSheetValue.IntermediatelyExpanded] if defined
   * else [FlexibleSheetValue.FullyExpanded].
   * @throws [CancellationException] if the animation is interrupted
   */
  public suspend fun show(target: FlexibleSheetValue? = null) {
    if (!isModal) {
      val targetValue1 = when {
        hasIntermediatelyExpandedState -> FlexibleSheetValue.IntermediatelyExpanded
        hasSlightlyExpandedState -> FlexibleSheetValue.SlightlyExpanded
        else -> FlexibleSheetValue.FullyExpanded
      }
      animateTo(targetValue = targetValue1, animSpec = animateSpec)

      val targetValue2 = when {
        hasIntermediatelyExpandedState -> FlexibleSheetValue.IntermediatelyExpanded
        hasSlightlyExpandedState -> FlexibleSheetValue.SlightlyExpanded
        else -> FlexibleSheetValue.FullyExpanded
      }
      animateTo(targetValue = target ?: targetValue2, animSpec = animateSpec)
    } else {
      val targetValue = when {
        hasIntermediatelyExpandedState -> FlexibleSheetValue.IntermediatelyExpanded
        hasSlightlyExpandedState -> FlexibleSheetValue.SlightlyExpanded
        else -> FlexibleSheetValue.FullyExpanded
      }
      animateTo(targetValue)
    }
  }

  /**
   * Hide the bottom sheet with animation and suspend until it is fully hidden or animation has
   * been cancelled.
   * @throws [CancellationException] if the animation is interrupted
   */
  public suspend fun hide() {
    check(!skipHiddenState) {
      "Attempted to animate to hidden when skipHiddenState was enabled. Set skipHiddenState" +
        " to false to use this function."
    }
    animateTo(targetValue = FlexibleSheetValue.Hidden, animSpec = animateSpec)
  }

  /**
   * Animate to a [targetValue].
   * If the [targetValue] is not in the set of anchors, the [currentValue] will be updated to the
   * [targetValue] without updating the offset.
   *
   * @throws CancellationException if the interaction interrupted by another interaction like a
   * gesture interaction or another programmatic interaction like a [animateTo] or [snapTo] call.
   *
   * @param targetValue The target value of the animation
   */
  public suspend fun animateTo(
    targetValue: FlexibleSheetValue,
    velocity: Float = swipeableState.lastVelocity,
    animSpec: AnimationSpec<Float> = animateSpec,
  ) {
    swipeableState.animateTo(
      targetValue = targetValue,
      velocity = velocity,
      animSpec = animSpec,
    )
  }

  /**
   * Snap to a [targetValue] without any animation.
   *
   * @throws CancellationException if the interaction interrupted by another interaction like a
   * gesture interaction or another programmatic interaction like a [animateTo] or [snapTo] call.
   *
   * @param targetValue The target value of the animation
   */
  public suspend fun snapTo(targetValue: FlexibleSheetValue) {
    swipeableState.snapTo(targetValue)
  }

  /**
   * Attempt to snap synchronously. Snapping can happen synchronously when there is no other swipe
   * transaction like a drag or an animation is progress. If there is another interaction in
   * progress, the suspending [snapTo] overload needs to be used.
   *
   * @return true if the synchronous snap was successful, or false if we couldn't snap synchronous
   */
  public fun trySnapTo(targetValue: FlexibleSheetValue): Boolean =
    swipeableState.trySnapTo(targetValue)

  /**
   * Find the closest anchor taking into account the velocity and settle at it with an animation.
   */
  public suspend fun settle(velocity: Float) {
    swipeableState.settle(velocity)
  }

  public var swipeableState: SwipeableV2State<FlexibleSheetValue> = SwipeableV2State(
    initialValue = initialValue,
    animationSpec = animateSpec,
    confirmValueChange = confirmValueChange,
  )

  internal companion object {
    /**
     * The default [Saver] implementation for [FlexibleSheetState].
     */
    fun Saver(
      skipIntermediatelyExpanded: Boolean,
      skipSlightlyExpanded: Boolean,
      flexibleSheetSize: FlexibleSheetSize,
      containSystemBars: Boolean,
      allowNestedScroll: Boolean,
      isModal: Boolean,
      animateSpec: AnimationSpec<Float>,
      confirmValueChange: (FlexibleSheetValue) -> Boolean,
    ) = Saver<FlexibleSheetState, FlexibleSheetValue>(
      save = { it.currentValue },
      restore = { savedValue ->
        FlexibleSheetState(
          skipIntermediatelyExpanded = skipIntermediatelyExpanded,
          skipSlightlyExpanded = skipSlightlyExpanded,
          isModal = isModal,
          initialValue = savedValue,
          animateSpec = animateSpec,
          flexibleSheetSize = flexibleSheetSize,
          containSystemBars = containSystemBars,
          allowNestedScroll = allowNestedScroll,
          confirmValueChange = confirmValueChange,
        )
      },
    )
  }
}

/**
 * Possible values of [FlexibleSheetValue].
 */
public enum class FlexibleSheetValue {
  /**
   * The sheet is not visible.
   */
  Hidden,

  /**
   * The sheet is visible at full height.
   */
  FullyExpanded,

  /**
   * The sheet is intermediately visible.
   */
  IntermediatelyExpanded,

  /**
   * The sheet is slightly visible.
   */
  SlightlyExpanded,
}

@InternalFlexibleApi
public fun emptySwipeWithinBottomSheetBoundsNestedScrollConnection(): NestedScrollConnection =
  object : NestedScrollConnection {
    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
      return Velocity.Zero
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
      return super.onPreFling(available)
    }
  }

@InternalFlexibleApi
public fun consumeSwipeWithinBottomSheetBoundsNestedScrollConnection(
  sheetState: FlexibleSheetState,
  orientation: Orientation,
  onFling: (velocity: Float) -> Unit,
  onDragging: (isDragging: Boolean) -> Unit = {},
): NestedScrollConnection = object : NestedScrollConnection {
  override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
    val delta = available.toFloat()
    return if (delta < 0 && source == NestedScrollSource.Drag) {
      onDragging.invoke(true)
      sheetState.swipeableState.dispatchRawDelta(delta).toOffset()
    } else if (delta > 0 && source == NestedScrollSource.Fling &&
      sheetState.currentValue == FlexibleSheetValue.FullyExpanded && !sheetState.isModal
    ) {
      onDragging.invoke(false)
      Offset.Zero
    } else if (delta > 0 &&
      sheetState.currentValue != FlexibleSheetValue.FullyExpanded && !sheetState.isModal
    ) {
      onDragging.invoke(true)
      sheetState.swipeableState.dispatchRawDelta(delta).toOffset()
    } else {
      onDragging.invoke(false)
      Offset.Zero
    }
  }

  override fun onPostScroll(
    consumed: Offset,
    available: Offset,
    source: NestedScrollSource,
  ): Offset {
    return if (source == NestedScrollSource.Drag) {
      onDragging.invoke(true)
      sheetState.swipeableState.dispatchRawDelta(available.toFloat()).toOffset()
    } else if (sheetState.currentValue != FlexibleSheetValue.Hidden && !sheetState.isModal) {
      sheetState.swipeableState.dispatchRawDelta(available.toFloat()).toOffset()
    } else {
      onDragging.invoke(false)
      Offset.Zero
    }
  }

  override suspend fun onPreFling(available: Velocity): Velocity {
    val toFling = available.toFloat()
    val currentOffset = sheetState.requireOffset()
    return if (toFling < 0 && currentOffset > sheetState.swipeableState.minOffset) {
      onFling(toFling)
      available
    } else {
      Velocity.Zero
    }
  }

  override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
    onDragging.invoke(false)
    onFling(available.toFloat())
    return available
  }

  private fun Float.toOffset(): Offset = Offset(
    x = if (orientation == Orientation.Horizontal) this else 0f,
    y = if (orientation == Orientation.Vertical) this else 0f,
  )

  @JvmName("velocityToFloat")
  private fun Velocity.toFloat(): Float = if (orientation == Orientation.Horizontal) x else y

  @JvmName("offsetToFloat")
  private fun Offset.toFloat(): Float = if (orientation == Orientation.Horizontal) x else y
}

/**
 * Create and [remember] a [FlexibleSheetState] for [FlexibleBottomSheet].
 *
 * @param skipIntermediatelyExpanded Whether the intermediately expanded state, if the sheet is tall enough,
 * should be skipped. If true, the sheet will always expand to the [FlexibleSheetValue.FullyExpanded] state and move to the
 * @param skipSlightlyExpanded Whether the slightly expanded state, if the sheet is tall enough,
 * should be skipped. If true, the sheet will always expand to the [FlexibleSheetValue.IntermediatelyExpanded] or [FlexibleSheetValue.FullyExpanded] state and move to the
 * [FlexibleSheetValue.Hidden] state when hiding the sheet, either programmatically or by user interaction.
 * @param containSystemBars Determines if the bottom sheet sizes should consider containing system bars (status + navigation).
 * @param allowNestedScroll Whether the bottom sheet should allow the content to implement nested scrolling.
 * @param isModal Determines if the bottom sheet should be modal. If set to true, the sheet will include a scrim overlaying the background and
 * will be dismissed upon touching outside of the sheet. If set to false, the bottom sheet allows interaction with the screen, permitting actions outside of the sheet.
 * @param flexibleSheetSize FlexibleSheetSize constraints the content size of [FlexibleBottomSheet] based on its states.
 * @param confirmValueChange Optional callback invoked to confirm or veto a pending state change.
 */
@Composable
public fun rememberFlexibleBottomSheetState(
  skipIntermediatelyExpanded: Boolean = false,
  skipSlightlyExpanded: Boolean = true,
  isModal: Boolean = false,
  containSystemBars: Boolean = false,
  allowNestedScroll: Boolean = true,
  animateSpec: AnimationSpec<Float> = SwipeableV2Defaults.AnimationSpec,
  flexibleSheetSize: FlexibleSheetSize = FlexibleSheetSize(),
  confirmValueChange: (FlexibleSheetValue) -> Boolean = { true },
): FlexibleSheetState = rememberFlexibleSheetState(
  skipIntermediatelyExpanded = skipIntermediatelyExpanded,
  skipSlightlyExpanded = skipSlightlyExpanded,
  isModal = isModal,
  animateSpec = animateSpec,
  confirmValueChange = confirmValueChange,
  flexibleSheetSize = flexibleSheetSize,
  containSystemBars = containSystemBars,
  allowNestedScroll = allowNestedScroll,
)

@Composable
private fun rememberFlexibleSheetState(
  skipIntermediatelyExpanded: Boolean = false,
  skipSlightlyExpanded: Boolean = false,
  isModal: Boolean = true,
  confirmValueChange: (FlexibleSheetValue) -> Boolean = { true },
  animateSpec: AnimationSpec<Float> = SwipeableV2Defaults.AnimationSpec,
  initialValue: FlexibleSheetValue = FlexibleSheetValue.Hidden,
  flexibleSheetSize: FlexibleSheetSize = FlexibleSheetSize(),
  containSystemBars: Boolean = false,
  allowNestedScroll: Boolean = true,
  skipHiddenState: Boolean = false,
): FlexibleSheetState {
  return rememberSaveable(
    skipIntermediatelyExpanded,
    skipSlightlyExpanded,
    confirmValueChange,
    saver = FlexibleSheetState.Saver(
      skipIntermediatelyExpanded = skipIntermediatelyExpanded,
      skipSlightlyExpanded = skipSlightlyExpanded,
      isModal = isModal,
      animateSpec = animateSpec,
      flexibleSheetSize = flexibleSheetSize,
      containSystemBars = containSystemBars,
      allowNestedScroll = allowNestedScroll,
      confirmValueChange = confirmValueChange,
    ),
  ) {
    FlexibleSheetState(
      skipIntermediatelyExpanded = skipIntermediatelyExpanded,
      skipSlightlyExpanded = skipSlightlyExpanded,
      isModal = isModal,
      initialValue = initialValue,
      animateSpec = animateSpec,
      confirmValueChange = confirmValueChange,
      flexibleSheetSize = flexibleSheetSize,
      containSystemBars = containSystemBars,
      allowNestedScroll = allowNestedScroll,
      skipHiddenState = skipHiddenState,
    )
  }
}
