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
import kotlin.test.assertTrue

/**
 * Unit tests for [calculateScrimProgress], the offset-driven scrim alpha that replaced the boolean
 * cross-fade responsible for the scrim flicker in issue #59.
 */
class ScrimProgressTest {

  private val anchors = mapOf(
    FlexibleSheetValue.FullyExpanded to 0f,
    FlexibleSheetValue.IntermediatelyExpanded to 500f,
    FlexibleSheetValue.SlightlyExpanded to 800f,
    FlexibleSheetValue.Hidden to 1000f,
  )

  @Test
  fun `scrim is opaque at every visible anchor`() {
    assertEquals(1f, calculateScrimProgress(anchors, offset = 0f))
    assertEquals(1f, calculateScrimProgress(anchors, offset = 500f))
    assertEquals(1f, calculateScrimProgress(anchors, offset = 800f))
  }

  @Test
  fun `scrim is fully transparent at the hidden anchor`() {
    assertEquals(0f, calculateScrimProgress(anchors, offset = 1000f))
  }

  @Test
  fun `scrim fades only across the dismiss travel`() {
    // Halfway between the slightly expanded anchor (800) and the hidden anchor (1000).
    assertEquals(0.5f, calculateScrimProgress(anchors, offset = 900f))
  }

  @Test
  fun `scrim progress is monotonic across the whole travel`() {
    // A flickering scrim is a non-monotonic one: dragging the sheet down must never make the scrim
    // darker again, at any point of the gesture (#59).
    var previous = Float.MAX_VALUE
    for (offset in 0..1000 step 10) {
      val progress = calculateScrimProgress(anchors, offset.toFloat())
      assertTrue(
        progress <= previous,
        "Scrim progress increased while the sheet moved down: $previous -> $progress at $offset",
      )
      previous = progress
    }
    assertEquals(0f, previous)
  }

  @Test
  fun `scrim progress is clamped outside the anchor range`() {
    assertEquals(1f, calculateScrimProgress(anchors, offset = -200f))
    assertEquals(0f, calculateScrimProgress(anchors, offset = 1500f))
  }

  @Test
  fun `defensive - a sheet that could not be hidden would keep an opaque scrim`() {
    // The library always publishes a hidden anchor, `skipHiddenState` only gates `hide()` and the
    // default `confirmValueChange`, so this configuration is not reachable through the public API.
    // It pins the guard that keeps the formula total.
    val noHiddenAnchor = anchors - FlexibleSheetValue.Hidden

    assertEquals(1f, calculateScrimProgress(noHiddenAnchor, offset = 0f))
    assertEquals(1f, calculateScrimProgress(noHiddenAnchor, offset = 800f))
  }

  @Test
  fun `scrim is transparent before the offset is initialized`() {
    assertEquals(0f, calculateScrimProgress(anchors, offset = null))
  }

  @Test
  fun `scrim is transparent when no anchors exist yet`() {
    assertEquals(0f, calculateScrimProgress(emptyMap(), offset = 0f))
  }

  @Test
  fun `scrim is transparent when only the hidden anchor exists`() {
    val hiddenOnly = mapOf(FlexibleSheetValue.Hidden to 1000f)

    assertEquals(0f, calculateScrimProgress(hiddenOnly, offset = 1000f))
    assertEquals(0f, calculateScrimProgress(hiddenOnly, offset = 0f))
  }

  @Test
  fun `degenerate range reports opaque above the hidden anchor`() {
    val collapsed = mapOf(
      FlexibleSheetValue.FullyExpanded to 1000f,
      FlexibleSheetValue.Hidden to 1000f,
    )

    assertEquals(0f, calculateScrimProgress(collapsed, offset = 1000f))
    assertEquals(1f, calculateScrimProgress(collapsed, offset = 900f))
  }

  @Test
  fun `expanding further never changes the scrim`() {
    // Going from slightly expanded to fully expanded must leave the scrim untouched, which is what
    // makes an offset-driven alpha safe to use for the whole sheet travel.
    val slightly = calculateScrimProgress(anchors, offset = 800f)
    val intermediately = calculateScrimProgress(anchors, offset = 500f)
    val fully = calculateScrimProgress(anchors, offset = 0f)

    assertEquals(slightly, intermediately)
    assertEquals(intermediately, fully)
  }
}
