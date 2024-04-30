import groovy.lang.Closure
import mega.privacy.android.build.shouldApplyDefaultConfiguration

plugins {
    alias(convention.plugins.mega.android.library)
    alias(convention.plugins.mega.android.test)
    id("kotlin-android")
    id("kotlin-kapt")
    id("de.mannodermaus.android-junit5")
}

android {
    namespace = "mega.privacy.android.shared.sync"

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = androidx.versions.compose.compiler.get()
    }

    lint {
        abortOnError = false
        xmlOutput = file("build/reports/lint-results.xml")
    }
    dependencies {
        lintChecks(project(":lint"))
        implementation(project(":core-ui"))

        implementation(platform(androidx.compose.bom))
        implementation(androidx.bundles.compose.bom)
    }
}
dependencies {
    implementation(project(":domain"))
    implementation(project(":shared:resources"))
    implementation(google.hilt.android)

    if (shouldApplyDefaultConfiguration(project)) {
        apply(plugin = "dagger.hilt.android.plugin")

        kapt(google.hilt.android.compiler)
        kapt(androidx.hilt.compiler)
    }

    testImplementation(testlib.junit)
    testImplementation(testlib.junit.test.ktx)
    testImplementation(testlib.bundles.ui.test)
    testImplementation(testlib.bundles.unit.test)
    testImplementation(testlib.arch.core.test)
    testImplementation(testlib.test.core.ktx)
    testImplementation(testlib.mockito)
    testImplementation(testlib.mockito.kotlin)
    testImplementation(testlib.mockito.android)

    testRuntimeOnly(testlib.junit.jupiter.engine)
    testImplementation(platform(testlib.junit5.bom))
    testImplementation(testlib.bundles.junit5.api)
}
