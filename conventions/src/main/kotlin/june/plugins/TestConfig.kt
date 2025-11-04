package june.plugins

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import june.wing.androidExtension
import june.wing.androidExtensionComponent
import june.wing.logDebug
import june.wing.logInfo
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.findByType

//https://medium.com/androiddevelopers/new-apis-in-the-android-gradle-plugin-f5325742e614
//class CustomSettings: Plugin<Settings> {
class TestConfig : Plugin<Project> {
    override fun apply(target: Project) {
        // Registers a callback on the application of the june.plugins.android.Android Application plugin.
        // This allows the CustomPlugin to work whether it's applied before or after
        // the june.plugins.android.Android Application plugin.
//        target.plugins.withType(AppPlugin::class.java){
//            //application
//            println("================AppPlugin=============================")
//        }
//        target.plugins.withType(BasePlugin::class.java){
//            //application or library
//            println("================BasePlugin=============================")
//        }

        with(target) {
            logDebug("=========================== START【${this@TestConfig}】 =========================")

            logInfo("常见构建自定义的即用配方，展示如何使用Android Gradle插件的公共API和DSL:")
            logInfo("https://github.com/android/gradle-recipes")

            val projectName = name
//            ApplicationAndroidComponentsExtension -> ApplicationExtension
//            findByType 不存在返回空 getByType 不存在抛异常
            println("$projectName ApplicationExtension ===================== ${extensions.findByType<ApplicationExtension>()}")
            println("$projectName LibraryExtension ========================= ${extensions.findByType<LibraryExtension>()}")
            println("$projectName ApplicationAndroidComponentsExtension ==== ${extensions.findByType<ApplicationAndroidComponentsExtension>()}")
            println("$projectName LibraryAndroidComponentsExtension ======== ${extensions.findByType<LibraryAndroidComponentsExtension>()}")
//            println("$projectName BaseAppModuleExtension =================== ${extensions.findByType<BaseAppModuleExtension>()}")
            println("$projectName getByName android ======================== ${extensions.findByName("android")}")
            println("$projectName getByName android ======================== ${androidExtension?.javaClass}")
            androidExtensionComponent?.apply {
                onVariants { variant ->
                    println("variant.buildType = ${variant.buildType}")
//                    println("variant.buildConfigFields = ${variant.buildConfigFields.keySet().get().toStr()}")
//                    println("variant.applicationId = ${variant.applicationId.get()}")
                    println("variant.name = ${variant.name}")
                    logDebug("------variant class--------------${variant.javaClass}")
//                    if (it is ApplicationVariantImpl) {
//                        log("---------ApplicationVariantImpl--------- ${it.name}")
//                    }
                }
            }
            logDebug("=========================== END【${this@TestConfig}】 =========================")
        }
    }
}