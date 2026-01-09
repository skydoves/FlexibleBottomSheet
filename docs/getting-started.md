# Getting Started

## Download

[![Maven Central](https://img.shields.io/maven-central/v/com.github.skydoves/flexible-core.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22com.github.skydoves%22%20AND%20a:%22flexible-core%22)

### Gradle

Add the dependency below to your **module**'s `build.gradle` file:

=== "Material 3"

    ```gradle
    dependencies {
        implementation("com.github.skydoves:flexible-bottomsheet-material3:$version")
    }
    ```

=== "Material"

    ```gradle
    dependencies {
        implementation("com.github.skydoves:flexible-bottomsheet-material:$version")
    }
    ```

### Kotlin Multiplatform

For Kotlin Multiplatform, add the dependency below to your **module**'s `build.gradle.kts` file:

=== "Material 3"

    ```kotlin
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("com.github.skydoves:flexible-bottomsheet-material3:$version")
            }
        }
    }
    ```

=== "Material"

    ```kotlin
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("com.github.skydoves:flexible-bottomsheet-material:$version")
            }
        }
    }
    ```

## Basic Usage

You can implement a flexible bottom sheet with `FlexibleBottomSheet`, similar to the `ModalBottomSheet` provided by Compose Material 3. Essentially, you can achieve the same behavior as `ModalBottomSheet` by not altering any properties.

```kotlin
FlexibleBottomSheet(
  onDismissRequest = onDismissRequest,
  sheetState = rememberFlexibleBottomSheetState(
    flexibleSheetSize = FlexibleSheetSize(
      fullyExpanded = 0.9f,
      intermediatelyExpanded = 0.5f,
      slightlyExpanded = 0.15f,
    ),
    isModal = true,
    skipSlightlyExpanded = false,
  ),
  containerColor = Color.Black,
) {
  Text(
    modifier = Modifier
      .fillMaxWidth()
      .padding(8.dp),
    text = "This is Flexible Bottom Sheet",
    textAlign = TextAlign.Center,
    color = Color.White,
  )
}
```

## Supported Platforms

FlexibleBottomSheet supports the following platforms:

| Platform | Support |
|----------|---------|
| Android | ✅ |
| iOS | ✅ |
| Desktop (JVM) | ✅ |
| Web (JavaScript) | ✅ |
| Web (WASM) | ✅ |
