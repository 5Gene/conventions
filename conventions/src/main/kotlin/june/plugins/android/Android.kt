package june.plugins.android

import androidx.room.gradle.RoomExtension
import com.google.devtools.ksp.gradle.KspExtension
import june.wing.*
import june.wing.AndroidCommonExtension
import june.wing.AndroidComponentsExtensions
import june.wing.findVersionStr
import june.wing.isAndroidApp
import june.wing.isAndroidLibrary
import june.wing.log
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.logging.LogLevel
import org.gradle.api.plugins.PluginManager
import org.gradle.kotlin.dsl.DependencyHandlerScope
import org.gradle.kotlin.dsl.buildscript
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.repositories
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import kotlin.text.toInt

interface Android {

    /**
     * ```kotlin
     *     override fun pluginConfigs(): PluginManager.() -> Unit = {
     *         //有需要的话执行父类逻辑
     *         super.pluginConfigs().invoke(this)
     *         //执行自己的逻辑
     *         apply("kotlin-android")
     *     }
     * ```
     */
    fun pluginConfigs(project: Project): PluginManager.(VersionCatalog) -> Unit

    /**
     * ```kotlin
     *     override fun androidExtensionConfig(): AndroidExtension.(Project, VersionCatalog) -> Unit {
     *         return { project, versionCatalog ->
     *             //有需要的话执行父类逻辑
     *             super.androidExtensionConfig().invoke(this,project,versionCatalog)
     *             //自己特有的逻辑
     *         }
     *     }
     * ```
     */
    fun androidExtensionConfig(project: Project): AndroidCommonExtension.(VersionCatalog) -> Unit

    fun androidComponentsExtensionConfig(project: Project): AndroidComponentsExtensions.(VersionCatalog) -> Unit

    fun kotlinOptionsConfig(project: Project): KotlinJvmCompilerOptions.() -> Unit

    /**
     * ```kotlin
     *     override fun dependenciesConfig(): DependencyHandlerScope.(VersionCatalog) -> Unit = { vlibs: VersionCatalog ->
     *         //有需要的话执行父类逻辑
     *         super.dependenciesConfig().invoke(this, vlibs)
     *         //自己特有的逻辑
     *     }
     * ```
     */
    fun dependenciesConfig(project: Project): DependencyHandlerScope.(VersionCatalog) -> Unit

}

open class BaseAndroid(val android: Android? = null) : Android {

    /**
     * ```kotlin
     *     override fun pluginConfigs(): PluginManager.() -> Unit = {
     *         //有需要的话执行父类逻辑
     *         super.pluginConfigs().invoke(this)
     *         //执行自己的逻辑
     *         apply("kotlin-android")
     *     }
     * ```
     */
    override fun pluginConfigs(project: Project): PluginManager.(VersionCatalog) -> Unit = {
        project.project.log("pluginConfigs()  ${this@BaseAndroid}".purple)
        android?.pluginConfigs(project)?.invoke(this, it)
    }

    /**
     * ```kotlin
     *     override fun androidExtensionConfig(project: Project): AndroidExtension.(Project, VersionCatalog) -> Unit {
     *         return { project, versionCatalog ->
     *             //有需要的话执行父类逻辑
     *             super.androidExtensionConfig().invoke(this,project,versionCatalog)
     *             //自己特有的逻辑
     *         }
     *     }
     * ```
     */
    override fun androidExtensionConfig(project: Project): AndroidCommonExtension.(VersionCatalog) -> Unit = {
        project.log("androidExtensionConfig()  ${this@BaseAndroid}".purple)
        //有需要的话执行父类逻辑
        android?.androidExtensionConfig(project)?.invoke(this, it)
    }


    override fun androidComponentsExtensionConfig(project: Project): AndroidComponentsExtensions.(VersionCatalog) -> Unit =
        {
            project.log("androidComponentsExtensionConfig()  ${this@BaseAndroid}".purple)
            android?.androidComponentsExtensionConfig(project)?.invoke(this, it)
        }

    override fun kotlinOptionsConfig(project: Project): KotlinJvmCompilerOptions.() -> Unit = {
        project.logger.log(LogLevel.DEBUG, "kotlinOptionsConfig()  ${this@BaseAndroid}".purple)
        android?.kotlinOptionsConfig(project)?.invoke(this)
    }

    /**
     * ```kotlin
     *     override fun dependenciesConfig(project: Project): DependencyHandlerScope.(VersionCatalog) -> Unit = { vlibs: VersionCatalog ->
     *         //有需要的话执行父类逻辑
     *         super.dependenciesConfig().invoke(this, vlibs)
     *         //自己特有的逻辑
     *     }
     * ```
     */
    override fun dependenciesConfig(project: Project): DependencyHandlerScope.(VersionCatalog) -> Unit = {
        project.log("dependenciesConfig()  ${this@BaseAndroid}".purple)
        android?.dependenciesConfig(project)?.invoke(this, it)
    }
}

class AndroidBase(pre: Android? = null) : BaseAndroid(pre) {

    override fun pluginConfigs(project: Project): PluginManager.(VersionCatalog) -> Unit = {
        super.pluginConfigs(project).invoke(this, it)
//        println("xxxxxxxxxxxxxxxxxx ${buildscript.dependencies::class.qualifiedName}  ${buildscript.dependencies}")
//        buildscript.dependencies.add("classpath", "org.jetbrains.kotlin:kotlin-gradle-plugin:2.0.0")
        val ktVersion = it.findVersionStr("kotlin") ?: "2.0.0"
        project.buildscript {
            repositories {
                maven {
                    url = project.uri("https://plugins.gradle.org/m2/")
                }
            }
            dependencies {
                //
                classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$ktVersion")
            }
        }
        //<editor-fold desc="android project default plugin">
        //如果根build.gradle没在plugins中apply的话这里无法依赖，之后补充自动依赖
        apply("kotlin-android")
        //apply("org.jetbrains.kotlin.android")
        apply("kotlin-parcelize")
        //</editor-fold>
    }

    override fun androidExtensionConfig(project: Project): AndroidCommonExtension.(VersionCatalog) -> Unit = { catalog ->
        super.androidExtensionConfig(project).invoke(this, catalog)
        //<editor-fold desc="android project default config">
        compileSdk = catalog.findVersion("android-compileSdk").get().requiredVersion.toInt()
        defaultConfig {
            minSdk = catalog.findVersion("android-minSdk").get().requiredVersion.toInt()
            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            vectorDrawables {
                useSupportLibrary = true
            }
        }
//        buildFeatures {
//            buildConfig = true
//        }
        compileOptions {
            // Up to Java 11 APIs are available through desugaring
            // https://developer.android.com/studio/write/java11-minimal-support-table
            //配置 config.project.java.version=17 对应 JavaVersion.VERSION_17
            val version = project.property("config.project.java.version", 17).toInt()
            val javaVersion = JavaVersion.values()[version - 1]
            println("compileOptions -> javaVersion: ${javaVersion.name}")
            //17 --> 16
            sourceCompatibility = javaVersion
            targetCompatibility = javaVersion

            encoding = "UTF-8"
            //isCoreLibraryDesugaringEnabled = true
        }
        //</editor-fold>
    }

    override fun kotlinOptionsConfig(project: Project): KotlinJvmCompilerOptions.() -> Unit = {
        super.kotlinOptionsConfig(project).invoke(this)
        //配置 config.project.java.version=17 对应 JVM_17
        val javaVersion = project.property("config.project.java.version", 17).toInt()
        //17 --> 9
        //jvmTarget.set(JvmTarget.JVM_17)
        val jvmTargetVersion = JvmTarget.values()[javaVersion - 8]
//        println("KotlinJvmCompilerOptions -> javaVersion: ${jvmTargetVersion.name}")
        jvmTarget.set(jvmTargetVersion)

        //apiVersion
        // 作用：在编译期间，编译器会根据指定的 apiVersion 来检查使用的 API。如果代码中使用了超出该版本的标准库 API，编译器将会报错。
        // 示例：如果设置为 1.9，则不允许使用 2.0 中引入的 API；如果尝试使用，将导致编译错误。
        // 例如，如果设置为 2.2 那么项目配置的kotlin版本必须高于等于2.2。
        //apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_2)

        //languageVersion
        // 作用：在使用支持 Kotlin 的集成开发环境（IDE）时，IDE 将根据 languageVersion 的设置提供代码提示、语法高亮和智能提示。这意味着 IDE 只会在代码补全时提供适合指定语言版本的特性，避免提示不支持的语法。
        // 示例：如果设置为 1.9，IDE 可能不会提示 2.0 中的新特性。
        // 例如，如果设置为 1.9，则可以使用 1.9 中的所有特性，但不能使用 2.0 中的新特性。
        languageVersion.set(KotlinVersion.KOTLIN_2_2)
        //总结：
        //languageVersion 主要影响语言特性的使用，决定了你可以使用哪些 Kotlin 语法和结构。
        //apiVersion 主要影响可用的标准库 API，确保代码兼容特定版本的库功能。

        //版本关系
        // 通常，apiVersion 不应该高于 languageVersion。
        // 因为 API 版本代表了库的功能，而语言版本则是对语法的支持。
        // 因此，如果 languageVersion 较低，编译器可能无法解析更高版本 API 的语法或特性。
    }

    override fun dependenciesConfig(project: Project): DependencyHandlerScope.(VersionCatalog) -> Unit = { catalog ->
        super.dependenciesConfig(project).invoke(this, catalog)
        catalog.findLibrary("test-junit").ifPresent { jUnit ->
            add("testImplementation", jUnit)
        }
        if (project.isAndroidApp) {
            catalog.findBundle("androidx-benchmark").ifPresent { androidxBenchmark ->
                //包括 androidx-test-ext-junit , androidx-test-espresso-core
                add("androidTestImplementation", androidxBenchmark)
            }
            catalog.findBundle("android-basic").ifPresent({ androidBasic ->
                project.project.log("implementation(android-basic)")
                add("implementation", androidBasic)
            })
        } else if (project.isAndroidLibrary) {
            catalog.findBundle("android-basic").ifPresent({ androidBasic ->
                project.project.log("implementation(android-basic)")
                add("implementation", androidBasic)
            })
        }
    }
}

class AndroidRoom(pre: Android? = null) : BaseAndroid(pre) {
    private fun Project.ksp(config: KspExtension.() -> Unit) = extensions.getByType<KspExtension>().config()
    private fun Project.room(config: RoomExtension.() -> Unit) = extensions.getByType<RoomExtension>().config()
    override fun pluginConfigs(project: Project): PluginManager.(VersionCatalog) -> Unit = {
        super.pluginConfigs(project).invoke(this, it)
        apply("androidx.room")
        apply("com.google.devtools.ksp")

        //https://kotlinlang.org/docs/ksp-quickstart.html#create-a-processor-of-your-own
        project.ksp {
            //room 配置 生成 Kotlin 源文件，而非 Java 代码。需要 KSP。默认值为 false。 有关详情，请参阅版本 2.6.0 的说明
            arg("room.generateKotlin", "true")
        }

        //room 指南
        //对于非 june.plugins.android.Android 库（即仅支持 Java 或 Kotlin 的 Gradle 模块），您可以依赖 androidx.room:room-common 来使用 Room 注解
        //https://developer.android.google.cn/training/data-storage/room?hl=zh-cn
        project.room {
            //使用 Room Gradle 插件时需要设置 schemaDirectory。这会配置 Room 编译器以及各种编译任务及其后端（javac、KAPT、KSP），
            //以将架构文件输出到变种文件夹（例如 schemas/flavorOneDebug/com.package.MyDatabase/1.json）中。这些文件应签入代码库中，以用于验证和自动迁移。
            schemaDirectory("${project.projectDir}/schemas")
        }
    }

    override fun androidExtensionConfig(project: Project): AndroidCommonExtension.(VersionCatalog) -> Unit = {
        super.androidExtensionConfig(project)(this, it)
        kspSourceSets()
    }

    override fun dependenciesConfig(project: Project): DependencyHandlerScope.(VersionCatalog) -> Unit = { catalog ->
        super.dependenciesConfig(project).invoke(this, catalog)
        catalog.findBundle("androidx-room").ifPresent {
            add("implementation", it)
        }
        catalog.findLibrary("androidx-room-compiler").ifPresent {
            add("ksp", it)
        }
    }
}