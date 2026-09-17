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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.skydoves.flexible.core.FlexibleSheetHost
import com.skydoves.flexible.core.FlexibleSheetSize
import com.skydoves.flexible.core.FlexibleSheetState
import com.skydoves.flexible.core.FlexibleSheetValue
import com.skydoves.flexible.core.rememberFlexibleBottomSheetState
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Regression test for an inline *modal* sheet blocking the content behind it (issues #71 / #37).
 *
 * The inline container deliberately installs no pointer input, so the modal scrim is the only thing
 * standing between a tap and the app behind a modal sheet. That makes the scrim load bearing in a
 * way it never was for a window hosted sheet, whose window swallowed everything by itself: if the
 * scrim stopped filling the container, or stopped installing its tap gesture, a modal inline sheet
 * would silently become non-modal.
 *
 * The exact same layout as [InlineNonModalTouchPassThroughTest] is used with `isModal = true`, so
 * the two tests read as the two halves of one claim: without a scrim the clicks get through, with
 * one they do not.
 *
 * Note: this does not contrast the two hosts either. A modal window hosted sheet blocks and
 * dismisses the same way, by its focusable popup window; the point here is that the inline host
 * reaches the same outcome with the scrim alone.
 */
@OptIn(ExperimentalTestApi::class)
class InlineModalTouchBlockingTest {

  @Test
  fun inlineModalSheet_takesEveryClickOutsideItself_andDismisses() = runComposeUiTest {
    var clicksBehind = 0
    var dismissRequests = 0
    lateinit var state: FlexibleSheetState
    setContent {
      Box(Modifier.fillMaxSize().testTag(RootTag)) {
        Box(Modifier.fillMaxSize().background(Color.White).clickable { clicksBehind++ })

        val sheetState = rememberFlexibleBottomSheetState(
          isModal = true,
          skipHiddenState = false,
          skipSlightlyExpanded = true,
          initialValue = FlexibleSheetValue.IntermediatelyExpanded,
          flexibleSheetSize = FlexibleSheetSize(
            fullyExpanded = 1f,
            intermediatelyExpanded = 0.5f,
          ),
          sheetHost = FlexibleSheetHost.Inline,
        )
        state = sheetState
        FlexibleBottomSheet(
          onDismissRequest = { dismissRequests++ },
          sheetState = sheetState,
          containerColor = Color.Blue,
          scrimColor = Color.Black.copy(alpha = 0.32f),
          shape = RectangleShape,
          dragHandle = null,
        ) {
          Box(Modifier.fillMaxWidth().height(80.dp))
        }
      }
    }
    waitForIdle()

    // A click on the sheet itself is neither a dismissal nor a click on the app behind it. This
    // also proves the sheet is really on screen, so the click below cannot miss it by accident.
    clickAt(verticalFraction = 0.95f)
    waitForIdle()
    assertEquals(0, clicksBehind, "A click on the modal sheet fell through to the content behind")
    assertEquals(0, dismissRequests, "A click on the modal sheet itself dismissed it")
    assertEquals(
      FlexibleSheetValue.IntermediatelyExpanded,
      state.currentValue,
      "The sheet left IntermediatelyExpanded without being asked to",
    )

    // Everything outside the sheet belongs to the scrim: it dismisses, and it never lets go.
    clickAt(verticalFraction = 0.1f)
    waitUntil(timeoutMillis = 5_000) { dismissRequests == 1 }
    waitUntil(timeoutMillis = 5_000) { state.currentValue == FlexibleSheetValue.Hidden }
    waitForIdle()

    assertEquals(0, clicksBehind, "The modal scrim let a click reach the content behind it")
    assertEquals(1, dismissRequests, "The modal scrim did not request a dismiss")
  }

  /** Clicks the window at [verticalFraction] of its height, horizontally centered. */
  private fun ComposeUiTest.clickAt(verticalFraction: Float) {
    onNodeWithTag(RootTag).performTouchInput {
      click(Offset(width / 2f, height * verticalFraction))
    }
  }
}

private const val RootTag = "root"
