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
 * No-op on the skia targets.
 *
 * Note that a window hosted sheet does get a dismiss key here, because its `Popup` sets
 * `dismissOnBackPress`, which is the Escape key on desktop. An inline sheet has no window to carry
 * that, so on these targets it is dismissed only by gesture or programmatically.
 */
@Composable
@InternalFlexibleApi
public actual fun FlexibleSheetBackHandler(enabled: Boolean, onBack: () -> Unit) {
  // No-op.
}
