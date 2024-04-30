import com.android.build.api.dsl.ApplicationDefaultConfig
import com.android.build.api.dsl.ApplicationExtension
import mega.privacy.android.gradle.configureAndroidCompose
import mega.privacy.android.gradle.configureKotlinAndroidApplication
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * Convention plugin for Android application
 *
 */
class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.application")
                apply("kotlin-android")
            }

            extensions.configure<ApplicationExtension> {
                configureKotlinAndroidApplication(this)
                configureAndroidCompose(this)

                buildFeatures {
                    dataBinding = true
                    viewBinding = true
                    buildConfig = true
                }

                defaultConfig {
                    multiDexEnabled = true

                    ndk {
                        abiFilters += listOf("armeabi-v7a", "x86", "x86_64", "arm64-v8a")
                    }
                    setupLanguageResources()
                }
                setupSourceSet()
                setupJniLibsPackaging()
            }
        }
    }

    private fun ApplicationExtension.setupJniLibsPackaging() {
        packaging {
            jniLibs.pickFirsts.add("lib/arm64-v8a/libc++_shared.so")
            jniLibs.pickFirsts.add("lib/arm64-v8a/libmega.so")
            jniLibs.pickFirsts.add("lib/arm64-v8a/libjniPdfium.so")
            jniLibs.pickFirsts.add("lib/arm64-v8a/libmodpdfium.so")
            jniLibs.pickFirsts.add("lib/arm64-v8a/libmodft2.so")
            jniLibs.pickFirsts.add("lib/arm64-v8a/libmodpng.so")

            jniLibs.pickFirsts.add("lib/armeabi-v7a/libc++_shared.so")
            jniLibs.pickFirsts.add("lib/armeabi-v7a/libmega.so")
            jniLibs.pickFirsts.add("lib/armeabi-v7a/libjniPdfium.so")
            jniLibs.pickFirsts.add("lib/armeabi-v7a/libmodpdfium.so")
            jniLibs.pickFirsts.add("lib/armeabi-v7a/libmodft2.so")
            jniLibs.pickFirsts.add("lib/armeabi-v7a/libmodpng.so")

            jniLibs.pickFirsts.add("lib/x86/libc++_shared.so")
            jniLibs.pickFirsts.add("lib/x86/libmega.so")
            jniLibs.pickFirsts.add("lib/x86/libjniPdfium.so")
            jniLibs.pickFirsts.add("lib/x86/libmodpdfium.so")
            jniLibs.pickFirsts.add("lib/x86/libmodft2.so")
            jniLibs.pickFirsts.add("lib/x86/libmodpng.so")

            jniLibs.pickFirsts.add("lib/x86_64/libc++_shared.so")
            jniLibs.pickFirsts.add("lib/x86_64/libmega.so")
            jniLibs.pickFirsts.add("lib/x86_64/libjniPdfium.so")
            jniLibs.pickFirsts.add("lib/x86_64/libmodpdfium.so")
            jniLibs.pickFirsts.add("lib/x86_64/libmodft2.so")
            jniLibs.pickFirsts.add("lib/x86_64/libmodpng.so")
        }
    }

    private fun ApplicationExtension.setupSourceSet() {
        sourceSets {
            getByName("debug") {
                res {
                    srcDirs("src/main/res")
                }
            }

            register("qa") {
                java {
                    srcDirs("src/qa/java")
                }
                res {
                    srcDirs("src/qa/res")
                }
            }
        }
    }

    private fun ApplicationDefaultConfig.setupLanguageResources() {
        resourceConfigurations += listOf(
            "en",
            "ar",
            "de",
            "es",
            "fr",
            "in",
            "it",
            "ja",
            "ko",
            "nl",
            "pl",
            "pt",
            "ro",
            "ru",
            "th",
            "vi",
            "zh-rCN",
            "zh-rTW"
        )
    }
}