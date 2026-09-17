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
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

/**
 * Anchor math of [calculateFlexibleSheetAnchor], with an emphasis on the non-modal anchors that
 * caused the drag jump in issue #68 and the drag flicker in issue #67.
 */
class FlexibleSheetAnchorTest {

  private val screenHeight = 2000f

  private fun nonModalAnchors(
    measuredSheetHeight: Int,
    flexibleSheetSize: FlexibleSheetSize,
    skipIntermediatelyExpanded: Boolean = false,
    skipSlightlyExpanded: Boolean = false,
  ): Map<FlexibleSheetValue, Float?> = FlexibleSheetValue.entries.associateWith { value ->
    calculateFlexibleSheetAnchor(
      value = value,
      isModal = false,
      measuredSheetHeight = measuredSheetHeight,
      sheetFullHeight = screenHeight * flexibleSheetSize.fullyExpanded,
      sheetConstraintHeight = screenHeight,
      screenMaxHeight = screenHeight,
      flexibleSheetSize = flexibleSheetSize,
      contentHeight = 0f,
      skipIntermediatelyExpanded = skipIntermediatelyExpanded,
      skipSlightlyExpanded = skipSlightlyExpanded,
    )
  }

  @Test
  fun `non-modal fully expanded anchor is the top of the sheet travel`() {
    val anchors = nonModalAnchors(
      measuredSheetHeight = 200,
      flexibleSheetSize = FlexibleSheetSize(
        fullyExpanded = 1.0f,
        intermediatelyExpanded = 0.5f,
        slightlyExpanded = 0.1f,
      ),
    )

    assertEquals(0f, anchors[FlexibleSheetValue.FullyExpanded])
  }

  @Test
  fun `non-modal anchors do not move when the sheet container grows on drag`() {
    val size = FlexibleSheetSize(
      fullyExpanded = 1.0f,
      intermediatelyExpanded = 0.5f,
      slightlyExpanded = 0.1f,
    )

    // Idle at the slightly expanded state: the container is only as tall as the visible sheet.
    val whileIdle = nonModalAnchors(measuredSheetHeight = 200, flexibleSheetSize = size)
    // A drag starts and the container grows to the fully expanded height.
    val whileDragging = nonModalAnchors(measuredSheetHeight = 2000, flexibleSheetSize = size)

    // Regression for #68: anchors that change mid-gesture make the anchor change handler re-target
    // the sheet, which is seen as the sheet jumping down and animating back.
    assertEquals(whileIdle, whileDragging)
  }

  @Test
  fun `non-modal fully expanded anchor never collides with the slightly expanded anchor`() {
    val anchors = nonModalAnchors(
      measuredSheetHeight = 200,
      flexibleSheetSize = FlexibleSheetSize(
        fullyExpanded = 1.0f,
        intermediatelyExpanded = 0.5f,
        slightlyExpanded = 0.1f,
      ),
    )

    assertNotEquals(
      anchors[FlexibleSheetValue.FullyExpanded],
      anchors[FlexibleSheetValue.SlightlyExpanded],
    )
  }

  @Test
  fun `non-modal anchors are ordered from fully expanded to hidden`() {
    val anchors = nonModalAnchors(
      measuredSheetHeight = 2000,
      flexibleSheetSize = FlexibleSheetSize(
        fullyExpanded = 1.0f,
        intermediatelyExpanded = 0.5f,
        slightlyExpanded = 0.1f,
      ),
    )

    val fully = anchors.getValue(FlexibleSheetValue.FullyExpanded)!!
    val intermediately = anchors.getValue(FlexibleSheetValue.IntermediatelyExpanded)!!
    val slightly = anchors.getValue(FlexibleSheetValue.SlightlyExpanded)!!
    val hidden = anchors.getValue(FlexibleSheetValue.Hidden)!!

    assertEquals(listOf(0f, 1000f, 1800f, 2000f), listOf(fully, intermediately, slightly, hidden))
  }

  @Test
  fun `non-modal anchors are not published before the first layout pass`() {
    val anchors = nonModalAnchors(
      measuredSheetHeight = 0,
      flexibleSheetSize = FlexibleSheetSize(fullyExpanded = 1.0f),
    )

    assertNull(anchors[FlexibleSheetValue.FullyExpanded])
  }

  @Test
  fun `non-modal skipped states have no anchor`() {
    val anchors = nonModalAnchors(
      measuredSheetHeight = 2000,
      flexibleSheetSize = FlexibleSheetSize(fullyExpanded = 1.0f),
      skipIntermediatelyExpanded = true,
      skipSlightlyExpanded = true,
    )

    assertNull(anchors[FlexibleSheetValue.IntermediatelyExpanded])
    assertNull(anchors[FlexibleSheetValue.SlightlyExpanded])
    assertEquals(0f, anchors[FlexibleSheetValue.FullyExpanded])
  }

  @Test
  fun `non-modal fully expanded anchor is stable for a partially expanded sheet`() {
    val size = FlexibleSheetSize(
      fullyExpanded = 0.5f,
      intermediatelyExpanded = 0.3f,
      slightlyExpanded = 0.1f,
    )

    val whileIdle = nonModalAnchors(measuredSheetHeight = 200, flexibleSheetSize = size)
    val whileDragging = nonModalAnchors(measuredSheetHeight = 1000, flexibleSheetSize = size)

    assertEquals(whileIdle, whileDragging)
    assertEquals(0f, whileIdle[FlexibleSheetValue.FullyExpanded])
    // sheetFullHeight (1000) - slightly expanded height (200)
    assertEquals(800f, whileIdle[FlexibleSheetValue.SlightlyExpanded])
  }

  @Test
  fun `modal anchors are measured from the top of the screen`() {
    val size = FlexibleSheetSize(
      fullyExpanded = 1.0f,
      intermediatelyExpanded = 0.5f,
      slightlyExpanded = 0.1f,
    )
    val anchors = FlexibleSheetValue.entries.associateWith { value ->
      calculateFlexibleSheetAnchor(
        value = value,
        isModal = true,
        measuredSheetHeight = 2000,
        sheetFullHeight = screenHeight,
        sheetConstraintHeight = screenHeight,
        screenMaxHeight = screenHeight,
        flexibleSheetSize = size,
        contentHeight = 0f,
        skipIntermediatelyExpanded = false,
        skipSlightlyExpanded = false,
      )
    }

    assertEquals(0f, anchors[FlexibleSheetValue.FullyExpanded])
    assertEquals(1000f, anchors[FlexibleSheetValue.IntermediatelyExpanded])
    assertEquals(1800f, anchors[FlexibleSheetValue.SlightlyExpanded])
    assertEquals(2000f, anchors[FlexibleSheetValue.Hidden])
  }
}
