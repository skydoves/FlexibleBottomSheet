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

import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import kotlin.math.min

/**
 * FlexibleSheetSize constraints the content size of [FlexibleBottomSheet] based on its states.
 * These constraints are calculated by multiplying the ratio with the maximum display height.
 *
 * Three expanding states are defined: [FlexibleSheetValue.FullyExpanded], [FlexibleSheetValue.IntermediatelyExpanded], and [FlexibleSheetValue.SlightlyExpanded].
 *
 * Use [FlexibleSheetSize.WrapContent] to make a state wrap its content height instead of using a fixed ratio.
 */
@Immutable
public data class FlexibleSheetSize(
  public val fullyExpanded: Float = 1.0f,
  public val intermediatelyExpanded: Float = 0.5f,
  public val slightlyExpanded: Float = 0.25f,
) {
  /**
   * Returns true if any of the size values use wrap content mode.
   */
  public val hasWrapContent: Boolean
    get() = fullyExpanded.isWrapContent() ||
      intermediatelyExpanded.isWrapContent() ||
      slightlyExpanded.isWrapContent()

  public companion object {
    /**
     * Special value indicating that the sheet should wrap its content height.
     * When used, the sheet will size itself to fit its content, up to the screen height.
     */
    public const val WrapContent: Float = -1f
  }
}

/**
 * Checks if the given size value represents wrap content mode.
 */
@InternalFlexibleApi
public fun Float.isWrapContent(): Boolean = this == FlexibleSheetSize.WrapContent

/**
 * Calculates the actual size for a given state, considering wrap content mode.
 *
 * @param screenHeight The maximum screen height in pixels.
 * @param contentHeight The measured content height in pixels (used when wrap content is enabled).
 * @return The calculated size ratio (0.0 to 1.0) relative to screen height.
 */
@InternalFlexibleApi
public fun Float.resolveSheetSize(screenHeight: Float, contentHeight: Float): Float {
  return if (this.isWrapContent()) {
    if (contentHeight <= 0f) {
      // Use a small value as fallback when content is not yet measured.
      // This ensures the anchor is created, and will be updated once content is measured.
      // Using 0.01 (1% of screen) as a minimal placeholder.
      0.01f
    } else {
      min(contentHeight, screenHeight) / screenHeight
    }
  } else {
    this
  }
}

/**
 * A modifier that removes the minimum height constraint while keeping the maximum height constraint.
 * This allows content to be smaller than the parent's minimum constraint, which is needed for
 * wrap content mode when Surface propagates min constraints.
 *
 * Unlike wrapContentHeight(unbounded = true), this modifier preserves the max constraint,
 * which is required for scrollable content like LazyColumn to work properly.
 */
@InternalFlexibleApi
public fun Modifier.removeMinHeightConstraint(): Modifier = this.layout { measurable, constraints ->
  val adjustedConstraints = constraints.copy(minHeight = 0)
  val placeable = measurable.measure(adjustedConstraints)
  layout(placeable.width, placeable.height) {
    placeable.place(0, 0)
  }
}
