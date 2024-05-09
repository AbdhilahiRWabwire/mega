import mega.privacy.android.build.preBuiltSdkDependency
import mega.privacy.android.build.shouldApplyDefaultConfiguration

plugins {
    alias(convention.plugins.mega.android.library)
    alias(convention.plugins.mega.android.room)
    alias(convention.plugins.mega.android.test)
    id("kotlin-android")
    id("kotlin-kapt")
    id("de.mannodermaus.android-junit5")

    kotlin("plugin.serialization") version "1.9.21"
}

apply(plugin = "jacoco")
apply(from = "${project.rootDir}/tools/jacoco.gradle")

android {

    defaultConfig {

        val appVersion: String by rootProject.extra
        resValue("string", "app_version", "\"${appVersion}\"")

        consumerProguardFiles("consumer-rules.pro")
    }

    lint {
        abortOnError = false
        xmlOutput = file("build/reports/lint-results.xml")
    }

    tasks.withType<Test> {
        configure<JacocoTaskExtension> {
            isIncludeNoLocationClasses = true
            excludes = listOf("jdk.internal.*")
        }
    }

    flavorDimensions += "service"
    productFlavors {
        create("gms") {
            dimension = "service"
        }
    }

    sourceSets {
        // Adds exported schema location as test app assets.
        getByName("androidTest").assets.srcDir("$projectDir/schemas")
    }
    namespace = "mega.privacy.android.data"
}

android.testVariants.all {
    compileConfiguration.exclude(group = "com.google.guava", module = "listenablefuture")
    runtimeConfiguration.exclude(group = "com.google.guava", module = "listenablefuture")
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":shared:sync"))
    implementation("com.google.guava:guava:31.0.1-jre")
    preBuiltSdkDependency(rootProject.extra)

    implementation(lib.coroutines.core)
    implementation(lib.kotlin.serialisation)
    implementation(google.gson)
    implementation(google.zxing)
    implementation(androidx.java.core)
    implementation(androidx.exifinterface)
    implementation(androidx.datastore.preferences)
    implementation(androidx.preferences)
    implementation(androidx.lifecycle.process)
    implementation(androidx.work.ktx)
    implementation(androidx.hilt.work)
    implementation(google.hilt.android)
    implementation(androidx.concurrent.futures)
    implementation(androidx.paging)
    implementation(androidx.documentfile)

    if (shouldApplyDefaultConfiguration(project)) {
        apply(plugin = "dagger.hilt.android.plugin")

        kapt(google.hilt.android.compiler)
        kapt(androidx.hilt.compiler)
        kapt(google.autovalue)
    }
    implementation(google.autovalue.annotations)

    "gmsImplementation"(lib.billing.client.ktx)

    implementation(platform(google.firebase.bom))
    implementation(google.firebase.perf.ktx)

    // Logging
    implementation(lib.bundles.logging)

    implementation(lib.sqlcipher)
    implementation(androidx.security.crypto)
    implementation(google.tink)

    // Testing dependencies
    testImplementation(testlib.bundles.unit.test)
    testImplementation(testlib.truth.ext)
    testImplementation(testlib.test.core.ktx)
    testImplementation(lib.bundles.unit.test)
    testImplementation(platform(testlib.junit5.bom))
    testImplementation(testlib.bundles.junit5.api)
    testRuntimeOnly(testlib.junit.jupiter.engine)

    androidTestImplementation(testlib.bundles.unit.test)
    androidTestImplementation(lib.bundles.unit.test)
    androidTestImplementation(testlib.junit.test.ktx)
    androidTestImplementation(testlib.runner)
}
