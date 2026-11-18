import java.security.MessageDigest

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "id.wynn.vocamemo"
    compileSdk = 36
    buildToolsVersion = "36.0.0"
    defaultConfig {
        applicationId = "id.wynn.vocamemo"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "1.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    val releaseStore =
        providers.environmentVariable("VOCA_KEYSTORE_PATH").filter { it.isNotBlank() }
    signingConfigs {
        if (releaseStore.isPresent)
            create("production") {
                storeFile = file(releaseStore.get())
                storePassword = providers.environmentVariable("VOCA_STORE_PASSWORD").get()
                keyAlias = providers.environmentVariable("VOCA_KEY_ALIAS").get()
                keyPassword = providers.environmentVariable("VOCA_KEY_PASSWORD").get()
            }
    }
    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            if (releaseStore.isPresent) signingConfig = signingConfigs.getByName("production")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    lint { abortOnError = true }
    testOptions { animationsDisabled = true }
}

ksp { arg("room.schemaLocation", "$projectDir/schemas") }

dependencies {
    val bom = platform("androidx.compose:compose-bom:2025.12.01")
    implementation(bom)
    androidTestImplementation(bom)
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-compose:1.12.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-savedstate:2.9.4")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    ksp("androidx.room:room-compiler:2.8.4")
    implementation("androidx.work:work-runtime-ktx:2.10.4")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

// The staged signed APK must describe exactly these production inputs.
val stampInputs =
    files(
        fileTree("src/main"),
        rootProject.file("build.gradle.kts"),
        rootProject.file("settings.gradle.kts"),
        rootProject.file("gradle.properties"),
        rootProject.file("gradle/wrapper/gradle-wrapper.properties"),
        file("build.gradle.kts"),
        file("proguard-rules.pro"),
    )
val stampDirectory = layout.buildDirectory.dir("generated/source-stamp")
val generateSourceStamp by
    tasks.registering {
        inputs.files(stampInputs)
        outputs.dir(stampDirectory)
        doLast {
            val digest = MessageDigest.getInstance("SHA-256")
            stampInputs.files
                .filter { it.isFile }
                .sortedBy { it.relativeTo(rootDir).invariantSeparatorsPath }
                .forEach {
                    digest.update(
                        it.relativeTo(rootDir).invariantSeparatorsPath.toByteArray(Charsets.UTF_8)
                    )
                    digest.update(0.toByte())
                    it.inputStream().use { input ->
                        val buffer = ByteArray(32768)
                        while (true) {
                            val n = input.read(buffer)
                            if (n < 0) break
                            digest.update(buffer, 0, n)
                        }
                    }
                }
            val directory = stampDirectory.get().asFile.apply { mkdirs() }
            directory
                .resolve("source-fingerprint.txt")
                .writeText(digest.digest().joinToString("") { "%02x".format(it) } + "\n")
        }
    }

android.sourceSets.getByName("main").assets.srcDir(stampDirectory)

tasks.named("preBuild") { dependsOn(generateSourceStamp) }
