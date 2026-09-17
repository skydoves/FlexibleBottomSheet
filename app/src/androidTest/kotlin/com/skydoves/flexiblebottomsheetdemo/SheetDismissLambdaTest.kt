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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
 * The sheet window is created inside a keyless `remember`, so it used to hold the
 * `onDismissRequest` lambda captured during the very first composition for the rest of its life.
 * Callers whose dismiss lambda changes identity, which is most of them, had their back presses
 * routed to a stale closure.
 */
@RunWith(AndroidJUnit4::class)
class SheetDismissLambdaTest {

  @get:Rule
  val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private val device: UiDevice
    get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

  @Test
  fun backPressInvokesTheCurrentDismissLambdaNotTheFirstOne() {
    val invocations = mutableListOf<String>()
    var useSecondLambda by mutableStateOf(false)
    lateinit var sheetState: FlexibleSheetState

    composeTestRule.setContent {
      sheetState = rememberFlexibleBottomSheetState(
        isModal = true,
        skipSlightlyExpanded = true,
        skipIntermediatelyExpanded = true,
      )

      // Two distinct lambda instances, each capturing a different constant, so this detects a stale
      // capture. A single lambda reading a snapshot value would not: it would read the new value
      // through the state object even when the closure itself is stale.
      val onDismissRequest: () -> Unit = if (useSecondLambda) {
        { invocations += "second" }
      } else {
        { invocations += "first" }
      }

      Box(modifier = Modifier.fillMaxSize()) {
        FlexibleBottomSheet(
          onDismissRequest = onDismissRequest,
          sheetState = sheetState,
        ) {
          Text(text = "Sheet content")
        }
      }
    }

    composeTestRule.waitUntil(TIMEOUT_MILLIS) { sheetState.isVisible }

    composeTestRule.runOnUiThread { useSecondLambda = true }
    composeTestRule.waitForIdle()

    device.pressBack()
    composeTestRule.waitUntil(TIMEOUT_MILLIS) {
      sheetState.currentValue == FlexibleSheetValue.Hidden
    }
    composeTestRule.waitForIdle()

    assertEquals(listOf("second"), invocations)
  }

  private companion object {
    const val TIMEOUT_MILLIS = 5_000L
  }
}
