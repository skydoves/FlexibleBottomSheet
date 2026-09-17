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
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.skydoves.flexible.bottomsheet.material3.FlexibleBottomSheet
import com.skydoves.flexible.core.FlexibleSheetSize
import com.skydoves.flexible.core.FlexibleSheetState
import com.skydoves.flexible.core.FlexibleSheetValue
import com.skydoves.flexible.core.rememberFlexibleBottomSheetState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Device level regression tests for issue #92: pressing back with a sheet open did nothing on the
 * first screen of the back stack, while swiping the sheet down worked.
 *
 * The back press is injected with [UiDevice] so it travels the real path, through the focused
 * window's back dispatcher, rather than being simulated against a compose root.
 */
@RunWith(AndroidJUnit4::class)
class SheetBackPressTest {

  @get:Rule
  val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private val device: UiDevice
    get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

  @Test
  fun backPressDismissesTheSheetOnTheFirstScreen() {
    var dismissals = 0
    lateinit var sheetState: FlexibleSheetState

    composeTestRule.setContent {
      sheetState = rememberFlexibleBottomSheetState(
        isModal = true,
        skipSlightlyExpanded = true,
        skipIntermediatelyExpanded = true,
      )

      Box(modifier = Modifier.fillMaxSize()) {
        FlexibleBottomSheet(
          onDismissRequest = { dismissals++ },
          sheetState = sheetState,
        ) {
          Text(text = "Sheet content")
        }
      }
    }

    composeTestRule.waitUntil(TIMEOUT_MILLIS) { sheetState.isVisible }

    device.pressBack()
    device.waitForIdle()

    composeTestRule.waitUntil(TIMEOUT_MILLIS) {
      sheetState.currentValue == FlexibleSheetValue.Hidden
    }
    assertEquals(1, dismissals)
    assertFalse(
      "The back press must be consumed by the sheet, not finish the activity",
      composeTestRule.activity.isFinishing,
    )
  }

  @Test
  fun backPressStepsDownOneStateAtATime() {
    lateinit var sheetState: FlexibleSheetState

    composeTestRule.setContent {
      sheetState = rememberFlexibleBottomSheetState(
        isModal = true,
        skipSlightlyExpanded = false,
        skipIntermediatelyExpanded = false,
        initialValue = FlexibleSheetValue.FullyExpanded,
        flexibleSheetSize = FlexibleSheetSize(
          fullyExpanded = 0.9f,
          intermediatelyExpanded = 0.5f,
          slightlyExpanded = 0.25f,
        ),
      )

      Box(modifier = Modifier.fillMaxSize()) {
        FlexibleBottomSheet(
          onDismissRequest = { },
          sheetState = sheetState,
        ) {
          Text(text = "Sheet content")
        }
      }
    }

    composeTestRule.waitUntil(TIMEOUT_MILLIS) {
      sheetState.currentValue == FlexibleSheetValue.FullyExpanded
    }

    // Exactly one state per back press: overlapping back handlers used to fire twice and skip a
    // state, or not fire at all.
    device.pressBack()
    composeTestRule.waitUntil(TIMEOUT_MILLIS) {
      sheetState.currentValue == FlexibleSheetValue.IntermediatelyExpanded
    }

    device.pressBack()
    composeTestRule.waitUntil(TIMEOUT_MILLIS) {
      sheetState.currentValue == FlexibleSheetValue.SlightlyExpanded
    }
  }

  @Test
  fun backPressDoesNotCrashASheetThatCannotHide() {
    var dismissals = 0
    lateinit var sheetState: FlexibleSheetState

    composeTestRule.setContent {
      sheetState = rememberFlexibleBottomSheetState(
        isModal = true,
        skipHiddenState = true,
        skipSlightlyExpanded = true,
        skipIntermediatelyExpanded = true,
      )

      Box(modifier = Modifier.fillMaxSize()) {
        FlexibleBottomSheet(
          onDismissRequest = { dismissals++ },
          sheetState = sheetState,
        ) {
          Text(text = "Sheet content")
        }
      }
    }

    composeTestRule.waitUntil(TIMEOUT_MILLIS) { sheetState.isVisible }

    // `hide()` throws when `skipHiddenState` is set, so a back press that routed to it crashed the
    // composition coroutine. The sheet must simply stay where it is.
    device.pressBack()
    device.waitForIdle()
    composeTestRule.waitForIdle()

    assertTrue(sheetState.isVisible)
    assertEquals(0, dismissals)
  }

  private companion object {
    const val TIMEOUT_MILLIS = 5_000L
  }
}
