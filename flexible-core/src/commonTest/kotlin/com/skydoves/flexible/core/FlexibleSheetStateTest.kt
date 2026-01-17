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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class FlexibleSheetStateTest {

  @Test
  internal fun usePopup_defaults_to_true() {
    val state = FlexibleSheetState(
      skipHiddenState = false,
      skipIntermediatelyExpanded = false,
      skipSlightlyExpanded = false,
      flexibleSheetSize = FlexibleSheetSize(),
      containSystemBars = true,
      allowNestedScroll = true,
      isModal = true,
      animateSpec = SwipeableV2Defaults.AnimationSpec,
    )

    assertTrue(state.usePopup, "usePopup should default to true")
  }

  @Test
  internal fun usePopup_can_be_set_to_false() {
    val state = FlexibleSheetState(
      skipHiddenState = false,
      skipIntermediatelyExpanded = false,
      skipSlightlyExpanded = false,
      flexibleSheetSize = FlexibleSheetSize(),
      containSystemBars = true,
      allowNestedScroll = true,
      isModal = true,
      usePopup = false,
      animateSpec = SwipeableV2Defaults.AnimationSpec,
    )

    assertFalse(state.usePopup, "usePopup should be false when explicitly set")
  }

  @Test
  internal fun usePopup_can_be_set_to_true_explicitly() {
    val state = FlexibleSheetState(
      skipHiddenState = false,
      skipIntermediatelyExpanded = false,
      skipSlightlyExpanded = false,
      flexibleSheetSize = FlexibleSheetSize(),
      containSystemBars = true,
      allowNestedScroll = true,
      isModal = true,
      usePopup = true,
      animateSpec = SwipeableV2Defaults.AnimationSpec,
    )

    assertTrue(state.usePopup, "usePopup should be true when explicitly set")
  }

  @Test
  internal fun usePopup_is_independent_of_isModal() {
    val modalWithPopup = FlexibleSheetState(
      skipHiddenState = false,
      skipIntermediatelyExpanded = false,
      skipSlightlyExpanded = false,
      flexibleSheetSize = FlexibleSheetSize(),
      containSystemBars = true,
      allowNestedScroll = true,
      isModal = true,
      usePopup = true,
      animateSpec = SwipeableV2Defaults.AnimationSpec,
    )

    val modalWithoutPopup = FlexibleSheetState(
      skipHiddenState = false,
      skipIntermediatelyExpanded = false,
      skipSlightlyExpanded = false,
      flexibleSheetSize = FlexibleSheetSize(),
      containSystemBars = true,
      allowNestedScroll = true,
      isModal = true,
      usePopup = false,
      animateSpec = SwipeableV2Defaults.AnimationSpec,
    )

    val nonModalWithPopup = FlexibleSheetState(
      skipHiddenState = false,
      skipIntermediatelyExpanded = false,
      skipSlightlyExpanded = false,
      flexibleSheetSize = FlexibleSheetSize(),
      containSystemBars = true,
      allowNestedScroll = true,
      isModal = false,
      usePopup = true,
      animateSpec = SwipeableV2Defaults.AnimationSpec,
    )

    val nonModalWithoutPopup = FlexibleSheetState(
      skipHiddenState = false,
      skipIntermediatelyExpanded = false,
      skipSlightlyExpanded = false,
      flexibleSheetSize = FlexibleSheetSize(),
      containSystemBars = true,
      allowNestedScroll = true,
      isModal = false,
      usePopup = false,
      animateSpec = SwipeableV2Defaults.AnimationSpec,
    )

    assertTrue(modalWithPopup.isModal)
    assertTrue(modalWithPopup.usePopup)

    assertTrue(modalWithoutPopup.isModal)
    assertFalse(modalWithoutPopup.usePopup)

    assertFalse(nonModalWithPopup.isModal)
    assertTrue(nonModalWithPopup.usePopup)

    assertFalse(nonModalWithoutPopup.isModal)
    assertFalse(nonModalWithoutPopup.usePopup)
  }

  @Test
  internal fun flexibleSheetSize_default_values_are_correct() {
    val defaultSize = FlexibleSheetSize()

    assertEquals(1.0f, defaultSize.fullyExpanded)
    assertEquals(0.5f, defaultSize.intermediatelyExpanded)
    assertEquals(0.25f, defaultSize.slightlyExpanded)
  }
}