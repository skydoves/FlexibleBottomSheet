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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.skydoves.flexible.core.FlexibleSheetSize
import com.skydoves.flexible.core.FlexibleSheetState
import com.skydoves.flexible.core.FlexibleSheetValue
import com.skydoves.flexible.core.Scrim
import com.skydoves.flexible.core.rememberFlexibleBottomSheetState
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Regression tests for a dismissed-but-still-composed sheet swallowing touches (issue #15).
 *
 * A sheet that has animated to [FlexibleSheetValue.Hidden] is still composed and its scrim still
 * fills the whole window. Until the scrim stopped installing a pointer input once the sheet is
 * hidden, that invisible full-screen scrim kept eating the taps meant for the content behind it.
 *
 * The first two tests drive the production [Scrim] directly over a clickable background, which is
 * the only way on this target to observe the tap actually reaching the content behind: the desktop
 * `Popup` that hosts a modal sheet is focusable and keeps its window-wide touch region whatever the
 * scrim does (the Android popup drops `FLAG_NOT_TOUCHABLE`/`FLAG_NOT_FOCUSABLE` for that, which has
 * no desktop equivalent). The last test then checks the same gate through a real
 * [FlexibleBottomSheet]: a visible sheet turns a tap above it into a dismiss request, a hidden one
 * ignores that very same tap, and showing the sheet again brings the interception back.
 */
@OptIn(ExperimentalTestApi::class)
class HiddenSheetTouchPassThroughTest {

  @Test
  fun hiddenSheetScrim_letsClicksReachTheContentBehind() = runComposeUiTest {
    var clicksBehind = 0
    var dismissRequests = 0
    setContent {
      Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().testTag(BehindTag).clickable { clicksBehind++ })
        val sheetState = rememberFlexibleBottomSheetState(
          isModal = true,
          initialValue = FlexibleSheetValue.Hidden,
        )
        Scrim(
          color = Color.Red,
          onDismissRequest = { dismissRequests++ },
          sheetState = sheetState,
        )
      }
    }
    waitForIdle()

    onNodeWithTag(BehindTag).performClick()
    waitForIdle()

    assertEquals(1, clicksBehind, "A hidden sheet's scrim swallowed the click behind it")
    assertEquals(0, dismissRequests, "A hidden sheet's scrim requested a dismiss")
  }

  @Test
  fun visibleSheetScrim_interceptsClicks() = runComposeUiTest {
    var clicksBehind = 0
    var dismissRequests = 0
    setContent {
      Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().testTag(BehindTag).clickable { clicksBehind++ })
        val sheetState = rememberFlexibleBottomSheetState(
          isModal = true,
          initialValue = FlexibleSheetValue.IntermediatelyExpanded,
        )
        Scrim(
          color = Color.Red,
          onDismissRequest = { dismissRequests++ },
          sheetState = sheetState,
        )
      }
    }
    waitForIdle()

    onNodeWithTag(BehindTag).performClick()
    waitForIdle()

    assertEquals(0, clicksBehind, "A visible sheet's scrim let the click through")
    assertEquals(1, dismissRequests, "A visible sheet's scrim did not request a dismiss")
  }

  @Test
  fun dismissedSheet_stopsInterceptingTaps_untilShownAgain() = runComposeUiTest {
    var dismissRequests = 0
    val showAgain = mutableStateOf(false)
    lateinit var state: FlexibleSheetState
    setContent {
      Box(Modifier.fillMaxSize()) {
        val sheetState = rememberFlexibleBottomSheetState(
          isModal = true,
          skipHiddenState = false,
          initialValue = FlexibleSheetValue.Hidden,
          flexibleSheetSize = FlexibleSheetSize(
            fullyExpanded = 0.9f,
            intermediatelyExpanded = 0.5f,
          ),
        )
        state = sheetState
        val shouldShowAgain by showAgain
        LaunchedEffect(shouldShowAgain) {
          if (shouldShowAgain) sheetState.show()
        }
        FlexibleBottomSheet(
          onDismissRequest = { dismissRequests++ },
          sheetState = sheetState,
          scrimColor = Color.Red,
          dragHandle = null,
        ) {
          Box(Modifier.fillMaxWidth().height(80.dp))
        }
      }
    }
    waitForIdle()
    waitUntil(timeoutMillis = 5_000) { state.isVisible }
    waitForIdle()

    // A tap on the scrim, well above the sheet, dismisses the visible sheet.
    tapAboveTheSheet()
    waitUntil(timeoutMillis = 5_000) { dismissRequests == 1 }
    waitUntil(timeoutMillis = 5_000) {
      state.currentValue == FlexibleSheetValue.Hidden &&
        state.targetValue == FlexibleSheetValue.Hidden
    }
    waitForIdle()

    // The very same tap must do nothing at all now that the sheet is hidden.
    tapAboveTheSheet()
    waitForIdle()
    assertEquals(1, dismissRequests, "A hidden sheet still reacted to a tap meant for the app")

    showAgain.value = true
    waitForIdle()
    waitUntil(timeoutMillis = 5_000) { state.isVisible }
    waitForIdle()

    tapAboveTheSheet()
    waitUntil(timeoutMillis = 5_000) { dismissRequests == 2 }
    assertEquals(2, dismissRequests, "A re-shown sheet stopped intercepting taps")
  }

  /**
   * Taps near the top of the sheet's popup window. Every state used here keeps the sheet in
   * the lower half of the screen, so this position always lands on the scrim, never on the
   * sheet.
   */
  private fun ComposeUiTest.tapAboveTheSheet() {
    onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.IsPopup))
      .onFirst()
      .performTouchInput { click(Offset(width / 2f, 50f)) }
  }

  private companion object {
    const val BehindTag = "behind"
  }
}
