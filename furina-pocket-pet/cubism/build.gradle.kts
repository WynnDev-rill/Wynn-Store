plugins { id("com.android.library") }
android {
 namespace = "com.live2d.sdk.cubism.framework"
 compileSdk = 36
 defaultConfig { minSdk = 26 }
 sourceSets["main"].java.srcDir("../.deps/CubismSdkForJava-5-r.5/Framework/framework/src/main/java")
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
}
dependencies { compileOnly(files("../.deps/CubismSdkForJava-5-r.5/Core/android/Live2DCubismCore.aar")) }
