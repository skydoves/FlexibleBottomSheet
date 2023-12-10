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

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed

/**
 * Adds paddings to accommodate the fully expanded size excluding the window insets.
 *
 * If you set [sheetState.flexibleSheetSize.fullyExpanded] to a value less than 1.0f,
 * the full content size may be hidden under the screen space and window insets (status + navigation) bars.
 * In such cases, you may need to add calculated paddings when using fully served content inside the bottom sheet.
 */
public fun Modifier.sheetPaddings(sheetState: FlexibleSheetState): Modifier = composed {
  val systemBarsPadding = WindowInsets.systemBars.asPaddingValues()
  val paddings =
    systemBarsPadding.calculateBottomPadding() + systemBarsPadding.calculateTopPadding()
  val availableHeight = screenHeight() * (1 - sheetState.flexibleSheetSize.fullyExpanded)
  val padding = availableHeight - paddings

  if (sheetState.currentValue == FlexibleSheetValue.FullyExpanded && padding.toPx() > 0) {
    Modifier.padding(bottom = availableHeight - paddings)
  } else {
    this
  }
}
