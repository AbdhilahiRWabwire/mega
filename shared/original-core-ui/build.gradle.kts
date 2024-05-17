plugins {
    alias(convention.plugins.mega.android.library)
    alias(convention.plugins.mega.android.test)
    id("kotlin-android")
    id("kotlin-kapt")
    alias(convention.plugins.mega.android.library.jacoco)
}

android {
    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = androidx.versions.compose.compiler.get()
    }

    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }

    lint {
        abortOnError = false
        xmlOutput = file("build/reports/lint-results.xml")
    }
    namespace = "mega.privacy.android.core"
}

dependencies {
    lintChecks(project(":lint"))

    implementation(project(":icon-pack"))
    testImplementation(project(":core-ui-test"))

    implementation(platform(androidx.compose.bom))
    implementation(androidx.constraintlayout.compose)
    implementation(androidx.bundles.compose.bom)
    implementation(lib.kotlin.ktx)
    implementation(androidx.appcompat)
    implementation(google.material)
    implementation(google.accompanist.systemui)
    implementation(google.accompanist.permissions)
    implementation(google.accompanist.navigationmaterial)
    implementation(androidx.compose.activity)
    implementation(androidx.lifecycle.runtime)
    implementation(androidx.lifecycle.runtime.compose)
    implementation(lib.compose.state.events)
    implementation(androidx.emojiPicker)
    implementation(lib.coil.compose)
    implementation(lib.balloon)
    implementation(google.accompanist.placeholder)

    testImplementation(testlib.bundles.ui.test)
    testImplementation(testlib.bundles.unit.test)
    testImplementation(testlib.compose.junit)

    debugImplementation(lib.kotlinpoet)
    debugImplementation(google.gson)
}