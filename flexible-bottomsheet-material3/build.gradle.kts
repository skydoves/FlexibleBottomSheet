import com.skydoves.flexible.Configuration
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
  id(libs.plugins.android.library.get().pluginId)
  id(libs.plugins.kotlin.multiplatform.get().pluginId)
  id(libs.plugins.jetbrains.compose.get().pluginId)
  id(libs.plugins.compose.compiler.get().pluginId)
  id(libs.plugins.nexus.plugin.get().pluginId)
  id(libs.plugins.baseline.profile.get().pluginId)
}

apply(from = "${rootDir}/scripts/publish-module.gradle.kts")

mavenPublishing {
  val artifactId = "flexible-bottomsheet-material3"
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
  jvm("desktop")
  iosX64()
  iosArm64()
  iosSimulatorArm64()
  macosX64()
  macosArm64()
  js(IR) {
    browser()
    nodejs()
  }
  @Suppress("OPT_IN_USAGE")
  applyHierarchyTemplate {
    common {
      group("jvm") {
        withAndroidTarget()
        withJvm()
      }
      group("skia") {
        withJvm()
        group("darwin") {
          group("apple") {
            group("ios") {
              withIosX64()
              withIosArm64()
              withIosSimulatorArm64()
            }
            group("macos") {
              withMacosX64()
              withMacosArm64()
            }
          }
          withJs()
        }
      }
    }
  }

  sourceSets {
    all {
      languageSettings.optIn("androidx.compose.material3.ExperimentalMaterial3Api")
      languageSettings.optIn("com.skydoves.flexible.core.InternalFlexibleApi")
    }
    val commonMain by getting {
      dependencies {
        api(project(":flexible-core"))

        implementation(compose.ui)
        implementation(compose.runtime)
        implementation(compose.animation)
        implementation(compose.material3)
      }
    }

    val androidMain by getting {
      dependencies {
        implementation(libs.androidx.lifecycle.viewmodel)
        implementation(libs.androidx.activity.compose)
      }
    }
  }

  targets.configureEach {
    compilations.configureEach {
      compilerOptions.configure {
        // https://youtrack.jetbrains.com/issue/KT-61573
        freeCompilerArgs.add("-Xexpect-actual-classes")
      }
    }
  }

  explicitApi()
}

android {
  compileSdk = Configuration.compileSdk
  namespace = "com.skydoves.flexible.bottomsheet.material3"
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
    include("com.skydoves.flexible.bottomsheet.material3.**")
  }
}

dependencies {
  baselineProfile(project(":baselineprofile"))
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
  compilerOptions {
    jvmTarget.set(JvmTarget.JVM_11)
    freeCompilerArgs.addAll(
      listOf(
        "-Xexplicit-api=strict",
        "-Xopt-in=androidx.compose.material3.ExperimentalMaterial3Api",
        "-Xopt-in=com.skydoves.flexible.core.InternalFlexibleApi",
      )
    )
  }
}
