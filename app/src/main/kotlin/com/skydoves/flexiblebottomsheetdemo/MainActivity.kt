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

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.skydoves.flexiblebottomsheetdemo.ui.theme.FlexibleBottomSheetDemoTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    setContent {
      var isShowingBottomSheet1 by remember { mutableStateOf(false) }
      var isShowingBottomSheet2 by remember { mutableStateOf(false) }
      var isShowingBottomSheet3 by remember { mutableStateOf(false) }

      FlexibleBottomSheetDemoTheme {
        Box(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.systemBars)) {
          Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally,
          ) {
            TestButton(title = "Show a Toast 1")

            Spacer(modifier = Modifier.height(30.dp))

            Button(
              onClick = {
                isShowingBottomSheet1 = !isShowingBottomSheet1
              },
            ) {
              Text(text = "Show Flexible Modal Sheet")
            }

            Button(
              onClick = {
                isShowingBottomSheet2 = !isShowingBottomSheet2
              },
            ) {
              Text(text = "Show Flexible Non Modal Sheet")
            }

            Button(
              onClick = {
                isShowingBottomSheet3 = !isShowingBottomSheet3
              },
            ) {
              Text(text = "Show Dynamic Content Sheet")
            }

            TestButton(title = "Show a Toast 3")

            TestButton(title = "Show a Toast 4")

            TestButton(title = "Show a Toast 5")
          }

          if (isShowingBottomSheet1) {
            FlexibleBottomSheetSample1 {
              isShowingBottomSheet1 = false
            }
          }

          if (isShowingBottomSheet2) {
            FlexibleBottomSheetSample2 {
              isShowingBottomSheet2 = false
            }
          }

          if (isShowingBottomSheet3) {
            FlexibleBottomSheetSample3 {
              isShowingBottomSheet3 = false
            }
          }
        }
      }
    }
  }
}

@Composable
private fun TestButton(title: String) {
  val context = LocalContext.current

  Spacer(modifier = Modifier.height(30.dp))

  Button(
    onClick = {
      Toast.makeText(context, title, Toast.LENGTH_SHORT).show()
    },
  ) {
    Text(text = title)
  }

  Spacer(modifier = Modifier.height(30.dp))
}
