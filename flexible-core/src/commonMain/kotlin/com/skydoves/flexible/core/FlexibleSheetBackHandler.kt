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
package com.skydoves.flexible.core

import androidx.compose.runtime.Composable

/**
 * Handles the platform back gesture for an inline sheet, which has no window of its own to catch it.
 *
 * On Android this registers with the host activity's back dispatcher, so the sheet takes part in the
 * normal back stack and predictive back like any other composable. The other targets have no system
 * back concept and this does nothing there.
 *
 * @param enabled Whether the sheet should currently consume the back gesture.
 * @param onBack Invoked when the back gesture is consumed.
 */
@Composable
@InternalFlexibleApi
public expect fun FlexibleSheetBackHandler(enabled: Boolean, onBack: () -> Unit)
