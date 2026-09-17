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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.skydoves.flexible.bottomsheet.material3.FlexibleBottomSheet
import com.skydoves.flexible.core.FlexibleSheetHost
import com.skydoves.flexible.core.FlexibleSheetSize
import com.skydoves.flexible.core.FlexibleSheetState
import com.skydoves.flexible.core.FlexibleSheetValue
import com.skydoves.flexible.core.rememberFlexibleBottomSheetState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Device level test for [FlexibleSheetHost.Inline], the layering claim behind issues #75, #71, #37,
 * #20 and #98.
 *
 * A window hosted sheet is a separate platform window and is therefore always above every
 * composable in the app, whatever the composition order. An inline sheet is part of the composition
 * and a sibling composed after it draws, and receives touches, on top of it.
 *
 * Taps go through [UiDevice] so they travel the real window dispatch rather than being injected into
 * a chosen compose root, which is the only way the two hosts can differ here.
 */
@RunWith(AndroidJUnit4::class)
class InlineSheetHostTest {

  @get:Rule
  val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private val device: UiDevice
    get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

  @Test
  fun aFooterComposedAfterAnInlineSheetReceivesTheTap() {
    val footerTaps = runFooterOverSheet(FlexibleSheetHost.Inline)

    assertEquals(1, footerTaps)
  }

  @Test
  fun aFooterComposedAfterAWindowHostedSheetDoesNotReceiveTheTap() {
    val footerTaps = runFooterOverSheet(FlexibleSheetHost.Window)

    assertEquals(0, footerTaps)
  }

  /**
   * Lays a footer over a sheet that is expanded far enough to sit underneath it, taps the footer
   * through the real input pipeline and reports how many taps it received.
   */
  private fun runFooterOverSheet(sheetHost: FlexibleSheetHost): Int {
    var footerTaps = 0
    lateinit var sheetState: FlexibleSheetState

    composeTestRule.setContent {
      sheetState = rememberFlexibleBottomSheetState(
        sheetHost = sheetHost,
        isModal = false,
        skipHiddenState = true,
        skipSlightlyExpanded = false,
        initialValue = FlexibleSheetValue.IntermediatelyExpanded,
        flexibleSheetSize = FlexibleSheetSize(
          fullyExpanded = 1.0f,
          intermediatelyExpanded = 0.5f,
          slightlyExpanded = 0.2f,
        ),
      )

      Box(modifier = Modifier.fillMaxSize()) {
        FlexibleBottomSheet(
          onDismissRequest = { },
          sheetState = sheetState,
        ) {
          Text(text = "Sheet content")
        }

        Footer(onClick = { footerTaps++ })
      }
    }

    composeTestRule.waitUntil(TIMEOUT_MILLIS) {
      sheetState.currentValue == FlexibleSheetValue.IntermediatelyExpanded
    }
    composeTestRule.waitForIdle()

    val footer = composeTestRule.onNodeWithTag(FOOTER_TAG).fetchSemanticsNode().boundsInWindow
    device.click(footer.center.x.toInt(), footer.center.y.toInt())
    device.waitForIdle()
    composeTestRule.waitForIdle()

    return footerTaps
  }

  private companion object {
    const val FOOTER_TAG = "footer"
    const val TIMEOUT_MILLIS = 5_000L
  }
}

/**
 * A bar pinned to the bottom of its parent, composed after the sheet.
 */
@Composable
private fun androidx.compose.foundation.layout.BoxScope.Footer(onClick: () -> Unit) {
  Box(
    modifier = Modifier
      .align(Alignment.BottomCenter)
      .fillMaxWidth()
      .height(72.dp)
      .testTag("footer")
      .clickable { onClick() },
  )
}
