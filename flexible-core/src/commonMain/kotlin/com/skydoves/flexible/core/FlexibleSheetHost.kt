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

/**
 * Determines where a [FlexibleBottomSheet] is rendered.
 *
 * The host decides whether the sheet lives in a platform window of its own or inside the
 * composition that declared it, which in turn decides how it layers against the rest of the app,
 * whose text selection and popup coordinate space it shares, and which touches it receives.
 */
public enum class FlexibleSheetHost {
  /**
   * Renders the sheet in a dedicated platform window above all app content: a
   * `TYPE_APPLICATION_PANEL` window on Android and a `Popup` on the other targets.
   *
   * The sheet always draws above everything in the app, including navigation drawers, app bars and
   * other sheets, and it cannot be covered by them. This is the default so that existing behavior
   * is unchanged.
   */
  Window,

  /**
   * Renders the sheet inside the composition that declared it, as a normal bottom aligned overlay
   * of its parent layout.
   *
   * The sheet then behaves like any other composable: it layers by composition order, so anything
   * composed after it (a navigation drawer, a bottom app bar, a sticky footer) draws above it; it
   * is confined to its parent's bounds, so a sheet placed inside `Scaffold` content stops above the
   * bottom bar; it shares the host window's text selection toolbar and popup coordinate space; and
   * a non modal sheet only receives the touches that actually land on it.
   *
   * Prefer this host when the sheet is part of a screen rather than an overlay on top of the app.
   */
  Inline,
}
