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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.skydoves.flexible.bottomsheet.material3.FlexibleBottomSheet
import com.skydoves.flexible.core.FlexibleSheetState
import com.skydoves.flexible.core.FlexibleSheetValue
import com.skydoves.flexible.core.rememberFlexibleBottomSheetState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Device level regression test for issue #15: a dismissed sheet is still composed, and its window
 * used to keep covering the screen and holding input focus, so the content behind it never received
 * another touch.
 *
 * Taps are injected through [UiDevice] rather than through the Compose test input, because Compose
 * dispatches its synthetic events straight into a chosen compose root and would bypass the very
 * window flags this test is about.
 */
@RunWith(AndroidJUnit4::class)
class HiddenSheetInputTest {

  @get:Rule
  val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private val device: UiDevice
    get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

  @Test
  fun contentBehindAHiddenSheetReceivesTouchesAgain() {
    var clicksBehind = 0
    var hideSheet by mutableStateOf(false)
    lateinit var sheetState: FlexibleSheetState

    composeTestRule.setContent {
      sheetState = rememberFlexibleBottomSheetState(isModal = true)

      LaunchedEffect(hideSheet) {
        if (hideSheet) {
          sheetState.hide()
        }
      }

      Box(modifier = Modifier.fillMaxSize()) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .testTag(BEHIND_TAG)
            .clickable { clicksBehind++ },
        )

        FlexibleBottomSheet(
          onDismissRequest = { hideSheet = true },
          sheetState = sheetState,
        ) {
          Text(text = "Sheet content")
        }
      }
    }

    composeTestRule.waitUntil(TIMEOUT_MILLIS) { sheetState.isVisible }

    // While the sheet is visible its modal window owns the whole screen, so this tap lands on the
    // scrim: it must not reach the content behind, and it dismisses the sheet.
    tapUpperArea()
    assertEquals(0, clicksBehind)

    composeTestRule.waitUntil(TIMEOUT_MILLIS) {
      sheetState.currentValue == FlexibleSheetValue.Hidden
    }
    composeTestRule.waitForIdle()

    // The sheet is still composed, only hidden. The same tap must now reach the content behind it.
    tapUpperArea()
    assertEquals(1, clicksBehind)
  }

  /**
   * Taps a point in the upper quarter of the display, which is scrim while the sheet is visible and
   * plain content once it is hidden.
   */
  private fun tapUpperArea() {
    device.click(device.displayWidth / 2, device.displayHeight / 4)
    device.waitForIdle()
    composeTestRule.waitForIdle()
  }

  private companion object {
    const val BEHIND_TAG = "behind"
    const val TIMEOUT_MILLIS = 5_000L
  }
}
