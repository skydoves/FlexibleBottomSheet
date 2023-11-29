import com.skydoves.flexible.Configuration

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
  id(libs.plugins.android.library.get().pluginId)
  id(libs.plugins.kotlin.multiplatform.get().pluginId)
  id(libs.plugins.jetbrains.compose.get().pluginId)
  id(libs.plugins.nexus.plugin.get().pluginId)
  id(libs.plugins.baseline.profile.get().pluginId)
}

apply(from = "${rootDir}/scripts/publish-module.gradle.kts")

mavenPublishing {
  val artifactId = "flexible-bottomsheet-material"
  coordinates(
    Configuration.artifactGroup,
    artifactId,
    rootProject.extra.get("libVersion").toString()
  )

  pom {
    name.set(artifactId)
    description.set("Advanced Jetpack Compose bottom sheet for segmented sizing and non-modal type, similar to Google Maps.")
  }
}

kotlin {
  androidTarget { publishLibraryVariants("release") }

  sourceSets {
    all {
      languageSettings.optIn("com.skydoves.flexible.core.InternalFlexibleApi")
    }
    val commonMain by getting {
      dependencies {
        api(project(":flexible-core"))

        implementation(compose.ui)
        implementation(compose.runtime)
        implementation(compose.animation)
        implementation(compose.material)
      }
    }

    val androidMain by getting {
      dependencies {
        implementation(libs.androidx.lifecycle.viewmodel)
        implementation(libs.androidx.activity.compose)
      }
    }
  }

  explicitApi()
}

android {
  compileSdk = Configuration.compileSdk
  namespace = "com.skydoves.flexible.bottomsheet.material"
  defaultConfig {
    minSdk = Configuration.minSdk
  }

  buildFeatures {
    compose = true
    buildConfig = false
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
  }

  composeOptions {
    kotlinCompilerExtensionVersion = libs.versions.androidxComposeCompiler.get()
  }

  packaging {
    resources {
      excludes.add("/META-INF/{AL2.0,LGPL2.1}")
    }
  }

  lint {
    abortOnError = false
  }
}

baselineProfile {
  baselineProfileOutputDir = "../../src/androidMain"
  filter {
    include("com.skydoves.flexible.bottomsheet.material.**")
    exclude("com.skydoves.flexible.bottomsheet.material3.**")
  }
}

dependencies {
  baselineProfile(project(":baselineprofile"))
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
  kotlinOptions {
    jvmTarget = "1.8"
    freeCompilerArgs += listOf(
      "-Xexplicit-api=strict",
      "-Xopt-in=com.skydoves.flexible.core.InternalFlexibleApi",
    )
  }
}
