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

class FlexibleSheetSizeTest {

  @Test
  fun wrapContent_sentinel_isDetected() {
    assertTrue(FlexibleSheetSize.WrapContent.isWrapContent())
    assertFalse(0.5f.isWrapContent())
    assertFalse(1.0f.isWrapContent())
  }

  @Test
  fun hasWrapContent_reflects_anyStateUsingWrapContent() {
    assertFalse(FlexibleSheetSize().hasWrapContent)
    assertTrue(
      FlexibleSheetSize(fullyExpanded = FlexibleSheetSize.WrapContent).hasWrapContent,
    )
    assertTrue(
      FlexibleSheetSize(intermediatelyExpanded = FlexibleSheetSize.WrapContent).hasWrapContent,
    )
    assertTrue(
      FlexibleSheetSize(slightlyExpanded = FlexibleSheetSize.WrapContent).hasWrapContent,
    )
  }

  @Test
  fun resolveSheetSize_returnsRatio_whenNotWrapContent() {
    assertEquals(0.5f, 0.5f.resolveSheetSize(screenHeight = 2000f, contentHeight = 300f))
    assertEquals(1.0f, 1.0f.resolveSheetSize(screenHeight = 2000f, contentHeight = 300f))
  }

  @Test
  fun resolveSheetSize_usesContentRatio_whenWrapContentAndMeasured() {
    // content 500 of screen 2000 -> 0.25
    val resolved = FlexibleSheetSize.WrapContent.resolveSheetSize(
      screenHeight = 2000f,
      contentHeight = 500f,
    )
    assertEquals(0.25f, resolved)
  }

  @Test
  fun resolveSheetSize_isClampedToScreen_whenContentLargerThanScreen() {
    val resolved = FlexibleSheetSize.WrapContent.resolveSheetSize(
      screenHeight = 1000f,
      contentHeight = 5000f,
    )
    assertEquals(1.0f, resolved)
  }

  @Test
  fun resolveSheetSize_usesTinyPlaceholder_whenWrapContentNotMeasured() {
    val resolved = FlexibleSheetSize.WrapContent.resolveSheetSize(
      screenHeight = 2000f,
      contentHeight = 0f,
    )
    assertEquals(0.01f, resolved)
  }
}
