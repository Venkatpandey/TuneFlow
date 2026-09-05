import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("io.gitlab.arturbosch.detekt")
    id("org.jlleitschuh.gradle.ktlint")
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

val smartTubeAars = fileTree("libs") { include("*.aar") }

android {
    namespace = "com.tuneflow.core.youtubenative"
    compileSdk = 35

    defaultConfig {
        minSdk = 25
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    // AARs are packaged by :app for every app variant. Keeping them compileOnly here
    // prevents AGP from trying to embed local AARs inside this library AAR.
    compileOnly(smartTubeAars)
    testImplementation(smartTubeAars)

    implementation("androidx.annotation:annotation:1.8.2")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.preference:preference:1.2.1")
    implementation("androidx.webkit:webkit:1.14.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("io.reactivex.rxjava2:rxandroid:2.1.1")
    implementation("io.reactivex.rxjava2:rxjava:2.2.21")

    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:okhttp-dnsoverhttps:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.squareup.okhttp3:okhttp-brotli:4.12.0")
    implementation("com.google.code.gson:gson:2.14.0")
    implementation("com.jayway.jsonpath:json-path:3.0.0")
    implementation("com.google.net.cronet:cronet-okhttp:0.1.1")
    implementation("org.chromium.net:cronet-api:500.0.2")
    implementation("com.localebro:okhttpprofiler:1.0.8")
    implementation("com.jakewharton:disklrucache:2.0.2")
    implementation("info.guardianproject.netcipher:netcipher:2.1.0")
    implementation("dnsjava:dnsjava:2.1.9")
    implementation("com.github.florianingerl.util:regex:1.1.11")
    implementation("com.grack:nanojson:1.10")
    implementation("com.google.protobuf:protobuf-javalite:3.17.3")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
}

detekt {
    buildUponDefaultConfig = true
    allRules = false
    config.setFrom(files("$rootDir/detekt.yml"))
}
