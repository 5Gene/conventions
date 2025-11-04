package june.wing

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.findByType
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

/**
 * Project 扩展属性
 * 提供 Android、Java、Kotlin 等扩展的便捷访问
 */

//要兼容 application和library 这里的泛型必须 用*全匹配
typealias AndroidCommonExtension = CommonExtension<*, *, *, *, *, *>

//要兼容 application和library 这里的泛型必须 用*全匹配
typealias AndroidComponentsExtensions = AndroidComponentsExtension<CommonExtension<*, *, *, *, *, *>, *, *>

/**
 * 获取 Version Catalog，带容错处理
 * @return VersionCatalog 如果配置了 vcl catalog，否则返回 null
 */
internal val Project.vlibs: VersionCatalog?
    get() = try {
        extensions.getByType<VersionCatalogsExtension>().named(ConventionConfig.VERSION_CATALOG_NAME)
    } catch (e: Exception) {
        logger.warn(
            "Version catalog '${ConventionConfig.VERSION_CATALOG_NAME}' not found. " +
            "Please configure it in settings.gradle.kts using: " +
            "dependencyResolutionManagement { versionCatalogs { create(\"${ConventionConfig.VERSION_CATALOG_NAME}\") { ... } } }",
            e
        )
        null
    }

/**
 * @deprecated Use {@code androidComponents} instead
 */
val Project.androidExtension
    get(): AndroidCommonExtension? = extensions.findByName("android") as? AndroidCommonExtension

val Project.androidLibExtension
    get(): LibraryExtension? = extensions.findByName("android") as? LibraryExtension

val Project.javaExtension
    get(): JavaPluginExtension? = extensions.findByType(JavaPluginExtension::class.java)

val Project.kotlinExtension
    get(): KotlinJvmProjectExtension? = extensions.findByType(KotlinJvmProjectExtension::class.java)

val Project.androidExtensionComponent
    get(): AndroidComponentsExtensions? = extensions.findByName("androidComponents") as? AndroidComponentsExtensions

val Project.isAndroidApplication
    get(): Boolean = androidExtension is ApplicationExtension

val Project.isAndroidApp
    get(): Boolean = androidExtensionComponent is ApplicationAndroidComponentsExtension

val Project.isAndroidLibrary
    get(): Boolean = androidExtension is LibraryExtension

/**
 * 修改 APK 名称
 */
fun Project.changeApkName(name: String) {
    setProperty("archivesBaseName", name)
}

/**
 * 获取项目 Git URL
 */
fun Project.url(): Lazy<String> = lazy {
    val stdout = java.io.ByteArrayOutputStream()
    exec {
        commandLine("git", "config", "--get", "remote.origin.url")
        standardOutput = stdout
    }
    val remoteUrl = stdout.toString().trim()
    logDebug("Remote URL: ${remoteUrl.removeSuffix(".git")}")
    remoteUrl
}

/**
 * 禁用 KSP 缓存
 */
fun Project.kspNoCache() {
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        //每次都执行ksp
        outputs.upToDateWhen { false }
    }
}

/**
 * 获取项目属性，带默认值
 */
fun Project.property(name: String, def: Any): String {
    return (findProperty(name) ?: def).toString()
}

