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
package com.skydoves.flexiblebottomsheetdemo

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.skydoves.flexible.bottomsheet.material3.FlexibleBottomSheet
import com.skydoves.flexible.core.FlexibleSheetSize
import com.skydoves.flexible.core.FlexibleSheetState
import com.skydoves.flexible.core.FlexibleSheetValue
import com.skydoves.flexible.core.rememberFlexibleBottomSheetState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Device level regression test for issue #16: a `TextField` inside the sheet never raised the
 * software keyboard, and once it did the keyboard covered the field.
 *
 * The sheet lives in a `WindowManager` window of its own, so the assertion here is that the IME
 * inset actually reaches that window's composition. `Modifier.imePadding()` inside the sheet can
 * only work if `WindowInsets.ime` is non zero there.
 */
@RunWith(AndroidJUnit4::class)
class SheetImeInsetsTest {

  @get:Rule
  val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun imeInsetReachesTheSheetWindow() {
    var imeBottomPx = 0
    lateinit var sheetState: FlexibleSheetState

    composeTestRule.setContent {
      sheetState = rememberFlexibleBottomSheetState(
        isModal = true,
        skipSlightlyExpanded = true,
        skipIntermediatelyExpanded = true,
      )
      var text by remember { mutableStateOf("") }

      Box(modifier = Modifier.fillMaxSize()) {
        FlexibleBottomSheet(
          onDismissRequest = { },
          sheetState = sheetState,
        ) {
          val density = LocalDensity.current
          imeBottomPx = WindowInsets.ime.getBottom(density)

          OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier
              .fillMaxWidth()
              .testTag(TEXT_FIELD_TAG),
          )
        }
      }
    }

    composeTestRule.waitUntil(TIMEOUT_MILLIS) { sheetState.isVisible }

    composeTestRule.onNodeWithTag(TEXT_FIELD_TAG).performClick()
    composeTestRule.waitUntil(TIMEOUT_MILLIS) { imeBottomPx > 0 }

    assertTrue("The IME inset never reached the sheet window", imeBottomPx > 0)

    // The sheet window covers the display, so its window coordinates are display coordinates.
    val textFieldBottom = composeTestRule.onNodeWithTag(TEXT_FIELD_TAG)
      .fetchSemanticsNode()
      .boundsInWindow
      .bottom
    val keyboardTop = device.displayHeight - imeBottomPx
    assertTrue(
      "The keyboard covers the text field: field bottom $textFieldBottom, top of IME $keyboardTop",
      textFieldBottom <= keyboardTop,
    )
  }

  private val device: UiDevice
    get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

  @Test
  fun sheetKeepsItsShareOfTheContainerWhenTheKeyboardOpens() {
    var imeBottomPx = 0
    lateinit var sheetState: FlexibleSheetState

    composeTestRule.setContent {
      sheetState = rememberFlexibleBottomSheetState(
        isModal = true,
        skipSlightlyExpanded = true,
        skipIntermediatelyExpanded = true,
        flexibleSheetSize = FlexibleSheetSize(fullyExpanded = FULLY_EXPANDED_RATIO),
      )
      var text by remember { mutableStateOf("") }

      Box(modifier = Modifier.fillMaxSize()) {
        FlexibleBottomSheet(
          onDismissRequest = { },
          sheetState = sheetState,
          dragHandle = null,
        ) {
          val density = LocalDensity.current
          imeBottomPx = WindowInsets.ime.getBottom(density)

          OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier
              .fillMaxWidth()
              .testTag(TEXT_FIELD_TAG),
          )
        }
      }
    }

    composeTestRule.waitUntil(TIMEOUT_MILLIS) { sheetState.isVisible }
    composeTestRule.waitForIdle()
    val shareWithoutKeyboard = sheetState.containerShare()

    composeTestRule.onNodeWithTag(TEXT_FIELD_TAG).performClick()
    composeTestRule.waitUntil(TIMEOUT_MILLIS) { imeBottomPx > 0 }
    composeTestRule.waitForIdle()
    val shareWithKeyboard = sheetState.containerShare()

    // The keyboard shortens the sheet's container, and the sheet has to shrink with it. The anchors
    // used to be sized against the full screen while the container was already shortened, so the
    // sheet lost the whole keyboard height on top of its own share: it collapsed away from the
    // content instead of resizing with it (#16).
    assertEquals(
      "The sheet's share of its container changed when the keyboard opened: " +
        "$shareWithoutKeyboard without it, $shareWithKeyboard with it (ime $imeBottomPx px)",
      shareWithoutKeyboard.toDouble(),
      shareWithKeyboard.toDouble(),
      0.05,
    )
  }

  /**
   * How much of its container the sheet currently occupies. The modal hidden anchor is the
   * container height and the offset is how far the sheet sits below its top, so their difference
   * is the visible sheet height.
   */
  private fun FlexibleSheetState.containerShare(): Float {
    val containerHeight = swipeableState.anchors.getValue(FlexibleSheetValue.Hidden)
    return (containerHeight - requireOffset()) / containerHeight
  }

  private companion object {
    const val TEXT_FIELD_TAG = "sheetTextField"
    const val TIMEOUT_MILLIS = 10_000L
    const val FULLY_EXPANDED_RATIO = 0.5f
  }
}
