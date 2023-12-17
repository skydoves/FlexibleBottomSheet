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
@file:OptIn(ExperimentalComposeUiApi::class)

package com.skydoves.flexible.core

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.popup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

/**
 * Popup specific for flexible bottom sheet.
 */
@Composable
@InternalFlexibleApi
public actual fun FlexibleBottomSheetPopup(
  onDismissRequest: () -> Unit,
  windowInsets: WindowInsets,
  sheetState: FlexibleSheetState,
  content: @Composable BoxScope.() -> Unit,
) {
  Popup(
    alignment = Alignment.BottomCenter,
    onDismissRequest = onDismissRequest,
    properties = PopupProperties(
      focusable = false,
      clippingEnabled = false,
      usePlatformInsets = false,
      dismissOnClickOutside = false,
    ),
  ) {
    Box(
      modifier = Modifier
        .semantics { this.popup() }
        .imePadding(),
    ) {
      content()
    }
  }
}
