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

import androidx.annotation.FloatRange

/**
 * FlexibleSheetSize constraints the content size of [FlexibleBottomSheet] based on its states.
 * These constraints are calculated by multiplying the ratio with the maximum display height.
 *
 * Three expanding states are defined: [FlexibleSheetValue.FullyExpanded], [FlexibleSheetValue.IntermediatelyExpanded], and [FlexibleSheetValue.SlightlyExpanded].
 *
 * @property fullyExpanded The content size of [FlexibleBottomSheet] when in the [FlexibleSheetValue.FullyExpanded] state.
 * @property intermediatelyExpanded The content size of [FlexibleBottomSheet] when in the [FlexibleSheetValue.IntermediatelyExpanded] state.
 * @property slightlyExpanded The content size of [FlexibleBottomSheet] when in the [FlexibleSheetValue.SlightlyExpanded] state.
 */
public data class FlexibleSheetSize(
  @FloatRange(0.0, 1.0) public val fullyExpanded: Float = 1.0f,
  @FloatRange(0.0, 1.0) public val intermediatelyExpanded: Float = 0.5f,
  @FloatRange(0.0, 1.0) public val slightlyExpanded: Float = 0.25f,
)
