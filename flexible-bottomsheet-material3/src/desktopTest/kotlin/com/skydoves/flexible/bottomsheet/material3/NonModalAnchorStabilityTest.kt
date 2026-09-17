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
package com.skydoves.flexible.bottomsheet.material3

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.skydoves.flexible.core.FlexibleSheetSize
import com.skydoves.flexible.core.FlexibleSheetState
import com.skydoves.flexible.core.FlexibleSheetValue
import com.skydoves.flexible.core.rememberFlexibleBottomSheetState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Regression test for the non-modal swipe anchors (issues #68 / #67).
 *
 * A non-modal sheet is only as tall as its currently visible part while it sits idle, and
 * the [FlexibleSheetValue.FullyExpanded] anchor used to be derived from that measured
 * height. Resting at [FlexibleSheetValue.SlightlyExpanded] therefore collapsed the fully
 * expanded anchor right onto the slightly expanded one, and the moment a drag started the
 * container grew to the full sheet height and every derived anchor moved at once - the
 * sheet visibly jumped and animated back (#68), and the anchor change handler could
 * re-target the sheet mid-gesture (#67).
 *
 * The fully expanded anchor is now the constant `0f`: the top of the sheet's own travel.
 * These tests pin both halves of that invariant - the anchor is `0f` and distinct from the
 * resting state's anchor, and the whole anchor map survives a drag unchanged.
 */
@OptIn(ExperimentalTestApi::class)
class NonModalAnchorStabilityTest {

  private val sheetMatcher =
    SemanticsMatcher.expectValue(SemanticsProperties.PaneTitle, "Bottom Sheet")

  @Test
  fun nonModalFullyExpandedAnchor_isZero_andDistinctFromRestingAnchor() = runComposeUiTest {
    lateinit var state: FlexibleSheetState
    setContent {
      NonModalSheet { state = it }
    }
    waitForIdle()

    val anchors = state.swipeableState.anchors
    val fullyExpanded = anchors[FlexibleSheetValue.FullyExpanded]
    val slightlyExpanded = anchors[FlexibleSheetValue.SlightlyExpanded]

    assertNotNull(fullyExpanded, "No FullyExpanded anchor was produced: $anchors")
    assertNotNull(slightlyExpanded, "No SlightlyExpanded anchor was produced: $anchors")
    assertEquals(
      0f,
      fullyExpanded,
      absoluteTolerance = 0.01f,
      message = "FullyExpanded anchor must be the top of the sheet travel, was $anchors",
    )
    assertTrue(
      fullyExpanded != slightlyExpanded,
      "FullyExpanded collapsed onto the SlightlyExpanded anchor: $anchors",
    )
  }

  @Test
  fun nonModalAnchors_areUnchanged_byADrag() = runComposeUiTest {
    lateinit var state: FlexibleSheetState
    setContent {
      NonModalSheet { state = it }
    }
    waitForIdle()

    val anchorsBeforeDrag = state.swipeableState.anchors.toMap()
    assertTrue(
      anchorsBeforeDrag.containsKey(FlexibleSheetValue.FullyExpanded),
      "Anchors were not initialized before the drag: $anchorsBeforeDrag",
    )

    // The drag has to clear the touch slop, otherwise `draggable` never reports a drag
    // start and the container never grows - the very growth that used to move the anchors.
    onNode(sheetMatcher).performTouchInput {
      down(center)
      moveBy(Offset(0f, -60f))
    }
    waitForIdle()

    assertEquals(
      anchorsBeforeDrag,
      state.swipeableState.anchors,
      "Anchors moved when the drag started",
    )

    onNode(sheetMatcher).performTouchInput { up() }
    waitForIdle()

    assertEquals(
      anchorsBeforeDrag,
      state.swipeableState.anchors,
      "Anchors moved after the drag settled",
    )
  }
}

/**
 * A non-modal sheet resting at [FlexibleSheetValue.SlightlyExpanded] whose fully expanded
 * size is the whole screen - the configuration that grows the container the most when a
 * drag begins.
 */
@Composable
private fun NonModalSheet(onState: (FlexibleSheetState) -> Unit) {
  Box(Modifier.fillMaxSize()) {
    val sheetState = rememberFlexibleBottomSheetState(
      isModal = false,
      skipHiddenState = true,
      skipSlightlyExpanded = false,
      initialValue = FlexibleSheetValue.SlightlyExpanded,
      flexibleSheetSize = FlexibleSheetSize(
        fullyExpanded = 1.0f,
        intermediatelyExpanded = 0.5f,
        slightlyExpanded = 0.1f,
      ),
    )
    onState(sheetState)
    FlexibleBottomSheet(
      onDismissRequest = {},
      sheetState = sheetState,
      dragHandle = null,
    ) {
      Box(Modifier.fillMaxWidth().height(80.dp))
    }
  }
}
