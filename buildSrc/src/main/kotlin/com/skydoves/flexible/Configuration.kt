package com.skydoves.flexible

object Configuration {
  const val compileSdk = 36
  const val targetSdk = 36
  const val minSdk = 21
  const val majorVersion = 0
  const val minorVersion = 1
  const val patchVersion = 5
  const val versionName = "$majorVersion.$minorVersion.$patchVersion"
  const val versionCode = 6
  const val snapshotVersionName = "$majorVersion.$minorVersion.${patchVersion + 1}-SNAPSHOT"
  const val artifactGroup = "com.github.skydoves"
}
