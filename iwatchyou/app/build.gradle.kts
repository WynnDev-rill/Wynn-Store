plugins {
 id("com.android.application")
 id("org.jetbrains.kotlin.android")
 id("org.jetbrains.kotlin.plugin.compose")
}
android {
 namespace = "id.wynn.iwatchyou"
 compileSdk = 36
 defaultConfig {
  applicationId = "id.wynn.iwatchyou"
  minSdk = 30
  targetSdk = 36
  versionCode = 15
  versionName = "1.4.1"
  testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  resourceConfigurations += listOf("en", "in")
 }
 signingConfigs {
  if (providers.environmentVariable("IW_KEYSTORE_PATH").isPresent) create("production") {
   storeFile = file(providers.environmentVariable("IW_KEYSTORE_PATH").get())
   storePassword = providers.environmentVariable("IW_STORE_PASSWORD").get()
   keyAlias = providers.environmentVariable("IW_KEY_ALIAS").get()
   keyPassword = providers.environmentVariable("IW_KEY_PASSWORD").get()
  }
  if (providers.environmentVariable("IW_LEGACY_KEYSTORE_PATH").isPresent) create("legacyOwner") {
   storeFile = file(providers.environmentVariable("IW_LEGACY_KEYSTORE_PATH").get())
   storePassword = providers.environmentVariable("IW_LEGACY_STORE_PASSWORD").get()
   keyAlias = providers.environmentVariable("IW_LEGACY_KEY_ALIAS").get()
   keyPassword = providers.environmentVariable("IW_LEGACY_KEY_PASSWORD").get()
  }
 }
 buildTypes {
  getByName("release") {
   isMinifyEnabled = true
   isShrinkResources = true
   if (signingConfigs.names.contains("production")) signingConfig = signingConfigs.getByName("production")
   proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
  }
  // The installed Uji app retains its private history under this package.
  create("legacy") {
   initWith(getByName("release"))
   applicationIdSuffix = ".preview"
   signingConfig = signingConfigs.findByName("legacyOwner")
   matchingFallbacks += "release"
  }
 }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
 kotlinOptions { jvmTarget = "17" }
 buildFeatures { compose = true }
}
dependencies {
 val bom = platform("androidx.compose:compose-bom:2025.12.01")
 implementation(bom)
 implementation("androidx.core:core-ktx:1.17.0")
 implementation("androidx.activity:activity-compose:1.12.2")
 implementation("androidx.compose.ui:ui")
 implementation("androidx.compose.foundation:foundation")
 implementation("androidx.compose.animation:animation")
 implementation("androidx.compose.material3:material3")
 implementation("androidx.compose.material:material-icons-core:1.7.8")
 implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
 testImplementation("junit:junit:4.13.2")
 androidTestImplementation("androidx.test.ext:junit:1.3.0")
 androidTestImplementation("androidx.test:runner:1.7.0")
 androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")
 androidTestImplementation(platform("androidx.compose:compose-bom:2025.12.01"))
 androidTestImplementation("androidx.compose.ui:ui-test-junit4")
 debugImplementation("androidx.compose.ui:ui-test-manifest")
}
