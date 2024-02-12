import groovy.lang.Closure

plugins {
    id("kotlin")
    id("com.android.lint")
    id("kotlin-kapt")
    kotlin("plugin.serialization") version "1.9.21"
}

lint {
    abortOnError = false
    xmlOutput = file("build/reports/lint-results.xml")
}

apply(plugin = "jacoco")
apply(from = "${project.rootDir}/tools/jacoco.gradle")

/**
 * Service to set jvmToolchain
 */
val service = project.extensions.getByType<JavaToolchainService>()

/**
 * Custom Launcher to set jvmToolchain
 */
val customLauncher = service.launcherFor {
    val jdk: String by rootProject.extra
    languageVersion.set(JavaLanguageVersion.of(jdk.toInt()))
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        val jdk: String by rootProject.extra
        jvmTarget = jdk
        val shouldSuppressWarnings: Boolean by rootProject.extra
        suppressWarnings = shouldSuppressWarnings
        freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
    }
    kotlinJavaToolchain.toolchain.use(customLauncher)
}

tasks.test {
    useJUnitPlatform()
    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).takeIf { it > 0 } ?: 1
}

dependencies {
    lintChecks(project(":lint"))
    implementation(lib.coroutines.core)
    implementation(lib.javax.inject)

    implementation(google.hilt.core)
    implementation(androidx.paging.core)

    implementation(lib.kotlin.serialisation)

    val shouldApplyDefaultConfiguration: Closure<Boolean> by rootProject.extra
    if (shouldApplyDefaultConfiguration()) {
        kapt(google.hilt.android.compiler)
    }

    // Testing dependencies
    testImplementation(testlib.bundles.unit.test)
    testImplementation(lib.bundles.unit.test)
    testImplementation(platform(testlib.junit5.bom))
    testImplementation(testlib.bundles.junit5.api)
    testRuntimeOnly(testlib.junit.jupiter.engine)
}