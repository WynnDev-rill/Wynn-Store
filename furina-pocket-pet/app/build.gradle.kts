plugins {
 id("com.android.application")
 id("org.jetbrains.kotlin.android")
 id("org.jetbrains.kotlin.plugin.compose")
}
android {
 namespace = "id.wynn.furina"
 compileSdk = 36
 defaultConfig {
  applicationId = "id.wynn.furina"
  minSdk = 26
  targetSdk = 36
  versionCode = 3
  versionName = "1.1.0"
  testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  ndk { abiFilters += listOf("arm64-v8a", "x86_64") }
 }
 signingConfigs {
  create("stable") {
   storeFile = file(System.getenv("FP_KEYSTORE") ?: "../signing/furina-release.jks")
   storePassword = System.getenv("FP_STORE_PASSWORD")
   keyAlias = System.getenv("FP_KEY_ALIAS") ?: "furina"
   keyPassword = System.getenv("FP_KEY_PASSWORD") ?: System.getenv("FP_STORE_PASSWORD")
  }
 }
 buildTypes {
  release {
   isMinifyEnabled = true
   isShrinkResources = true
   signingConfig = signingConfigs.getByName("stable")
   proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
  }
 }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
 kotlinOptions { jvmTarget = "17" }
 buildFeatures { compose = true; buildConfig = true }
 sourceSets["main"].assets.srcDir("../.deps/assets")
 sourceSets["debug"].assets.srcDir("../.deps/test-assets")
 packaging { jniLibs.useLegacyPackaging = false; resources.excludes += "META-INF/DEPENDENCIES" }
}
dependencies {
 implementation(project(":cubism"))
 implementation(files("../.deps/CubismSdkForJava-5-r.5/Core/android/Live2DCubismCore.aar"))
 implementation(platform("androidx.compose:compose-bom:2025.12.01"))
 implementation("androidx.core:core-ktx:1.17.0")
 implementation("androidx.activity:activity-compose:1.12.2")
 implementation("androidx.compose.ui:ui")
 implementation("androidx.compose.foundation:foundation")
 implementation("androidx.compose.material3:material3")
 implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
 testImplementation("junit:junit:4.13.2")
 testImplementation("org.json:json:20250517")
 androidTestImplementation("androidx.test.ext:junit:1.3.0")
 androidTestImplementation("androidx.test:runner:1.7.0")
 androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")
}
