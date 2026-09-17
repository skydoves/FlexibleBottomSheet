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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.skydoves.flexible.bottomsheet.material3.FlexibleBottomSheet
import com.skydoves.flexible.core.FlexibleSheetHost
import com.skydoves.flexible.core.FlexibleSheetSize
import com.skydoves.flexible.core.FlexibleSheetValue
import com.skydoves.flexible.core.rememberFlexibleBottomSheetState

/**
 * A non-modal sheet hosted inline, inside the content slot of a [Scaffold] that has a bottom bar.
 *
 * Because the sheet is part of the composition rather than a window of its own, it is confined to
 * the content padding and stops above the navigation bar, and the navigation bar stays interactive
 * and stays on top while the sheet is dragged.
 */
@Composable
fun FlexibleBottomSheetSample4(
  onDismissRequest: () -> Unit,
) {
  var selectedTab by remember { mutableIntStateOf(1) }

  val sheetState = rememberFlexibleBottomSheetState(
    sheetHost = FlexibleSheetHost.Inline,
    isModal = false,
    skipHiddenState = true,
    skipSlightlyExpanded = false,
    initialValue = FlexibleSheetValue.SlightlyExpanded,
    flexibleSheetSize = FlexibleSheetSize(
      fullyExpanded = 1.0f,
      intermediatelyExpanded = 0.5f,
      slightlyExpanded = 0.2f,
    ),
  )

  Scaffold(
    bottomBar = {
      NavigationBar {
        listOf(
          "Home" to Icons.Default.Home,
          "Places" to Icons.Default.Place,
          "Settings" to Icons.Default.Settings,
        ).forEachIndexed { index, (label, icon) ->
          NavigationBarItem(
            selected = selectedTab == index,
            onClick = { selectedTab = index },
            icon = { Icon(imageVector = icon, contentDescription = label) },
            label = { Text(text = label) },
          )
        }
      }
    },
  ) { contentPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(contentPadding)
        .background(Color(0xFFE8EAF6)),
    ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Text(
          text = "Map content",
          fontSize = 22.sp,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = "Everything here stays interactive, and the navigation bar below is never " +
            "covered by the sheet.",
          modifier = Modifier.padding(top = 8.dp),
        )
        Button(
          onClick = onDismissRequest,
          modifier = Modifier.padding(top = 16.dp),
        ) {
          Text(text = "Back to the samples")
        }
      }

      FlexibleBottomSheet(
        onDismissRequest = { },
        sheetState = sheetState,
        containerColor = Color.White,
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        ) {
          Text(
            text = "Nearby places",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
          )
          repeat(12) { index ->
            Text(
              text = "Place ${index + 1}",
              modifier = Modifier.padding(vertical = 12.dp),
            )
          }
        }
      }
    }
  }
}
