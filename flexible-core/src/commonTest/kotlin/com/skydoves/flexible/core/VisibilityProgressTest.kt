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

/**
 * Unit tests for [calculateVisibilityProgress], which backs [FlexibleSheetState.visibilityProgress]
 * (feature request #57).
 *
 * Offsets increase downward: 0f == fully expanded (top), larger == pushed toward the bottom/hidden.
 */
class VisibilityProgressTest {

  // Hidden at the bottom (offset 1000), FullyExpanded at the top (offset 0).
  private val anchors = mapOf(
    FlexibleSheetValue.Hidden to 1000f,
    FlexibleSheetValue.IntermediatelyExpanded to 500f,
    FlexibleSheetValue.FullyExpanded to 0f,
  )

  @Test
  fun returnsZero_whenOffsetNotInitialized() {
    assertEquals(0f, calculateVisibilityProgress(anchors, offset = null))
  }

  @Test
  fun returnsZero_whenAnchorsEmpty() {
    assertEquals(0f, calculateVisibilityProgress(emptyMap(), offset = 500f))
  }

  @Test
  fun returnsZero_atHiddenOffset() {
    assertEquals(0f, calculateVisibilityProgress(anchors, offset = 1000f))
  }

  @Test
  fun returnsOne_atFullyExpandedOffset() {
    assertEquals(1f, calculateVisibilityProgress(anchors, offset = 0f))
  }

  @Test
  fun returnsHalf_atMidpointOffset() {
    assertEquals(0.5f, calculateVisibilityProgress(anchors, offset = 500f))
  }

  @Test
  fun isClamped_whenOffsetBeyondHidden() {
    assertEquals(0f, calculateVisibilityProgress(anchors, offset = 1500f))
  }

  @Test
  fun isClamped_whenOffsetAboveFullyExpanded() {
    assertEquals(1f, calculateVisibilityProgress(anchors, offset = -200f))
  }

  @Test
  fun usesMinMaxFallback_whenNoHiddenAnchor() {
    // skipHiddenState style: no Hidden anchor. Least-visible endpoint becomes the max anchor (600).
    val noHidden = mapOf(
      FlexibleSheetValue.SlightlyExpanded to 600f,
      FlexibleSheetValue.FullyExpanded to 0f,
    )
    assertEquals(0f, calculateVisibilityProgress(noHidden, offset = 600f))
    assertEquals(1f, calculateVisibilityProgress(noHidden, offset = 0f))
    assertEquals(0.5f, calculateVisibilityProgress(noHidden, offset = 300f))
  }

  @Test
  fun returnsOne_whenSingleFullyExpandedAnchor() {
    // Degenerate range with an explicit FullyExpanded anchor -> fully expanded.
    val single = mapOf(FlexibleSheetValue.FullyExpanded to 0f)
    assertEquals(1f, calculateVisibilityProgress(single, offset = 0f))
  }

  @Test
  fun returnsZero_whenSingleHiddenAnchor() {
    // Reachable on the first layout pass (skipSlightlyExpanded defaults to true, FullyExpanded not
    // yet measured). A lone Hidden anchor must report 0f, not 1f, so content bound to
    // visibilityProgress does not flash fully-opaque on open (#57 regression).
    val single = mapOf(FlexibleSheetValue.Hidden to 1000f)
    assertEquals(0f, calculateVisibilityProgress(single, offset = 1000f))
  }

  @Test
  fun degenerateRange_prefersFullyExpandedPresence() {
    // All-equal offsets: with a FullyExpanded anchor present -> 1f; without one -> 0f.
    val withFully = mapOf(
      FlexibleSheetValue.Hidden to 500f,
      FlexibleSheetValue.FullyExpanded to 500f,
    )
    assertEquals(1f, calculateVisibilityProgress(withFully, offset = 500f))

    val withoutFully = mapOf(
      FlexibleSheetValue.Hidden to 500f,
      FlexibleSheetValue.SlightlyExpanded to 500f,
    )
    assertEquals(0f, calculateVisibilityProgress(withoutFully, offset = 500f))
  }
}
