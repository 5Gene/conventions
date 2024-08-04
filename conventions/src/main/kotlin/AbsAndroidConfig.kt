import com.android.build.gradle.BasePlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.plugins.PluginManager
import org.gradle.kotlin.dsl.DependencyHandlerScope
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile
import wing.AndroidCommonExtension
import wing.AndroidComponentsExtensions
import wing.androidExtensionComponent
import wing.log
import wing.vlibs


/**
 *
 * 学习如何使用 agp api
 * https://github.com/android/gradle-recipes/tree/agp-8.4
 *
 * https://developer.android.google.cn/build/extend-agp?hl=zh-cn#variant-api-artifacts-tasks
 *
 * https://docs.gradle.org/current/userguide/writing_plugins.html
 *
 * https://medium.com/androiddevelopers/gradle-and-agp-build-apis-taking-your-plugin-to-the-next-step-95e7bd1cd4c9
 *
 * https://medium.com/androiddevelopers/new-apis-in-the-android-gradle-plugin-f5325742e614
 *
 */
open class AbsAndroidConfig : Plugin<Project> {


    context(Project)
    open fun onProject() {
    }

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
    context(Project)
    open fun pluginConfigs(): PluginManager.(VersionCatalog) -> Unit = {}

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
    context(Project)
    open fun androidExtensionConfig(): AndroidCommonExtension.(VersionCatalog) -> Unit = { _ -> }

    context(Project)
    open fun androidComponentsExtensionConfig(): AndroidComponentsExtensions.(VersionCatalog) -> Unit = { _ -> }


    context(Project)
    open fun kotlinOptionsConfig(): KotlinJvmCompilerOptions.() -> Unit = { }

    /**
     * ```kotlin
     *     override fun dependenciesConfig(): DependencyHandlerScope.(VersionCatalog) -> Unit = { vlibs: VersionCatalog ->
     *         //有需要的话执行父类逻辑
     *         super.dependenciesConfig().invoke(this, vlibs)
     *         //自己特有的逻辑
     *     }
     * ```
     */
    context(Project)
    open fun dependenciesConfig(): DependencyHandlerScope.(VersionCatalog) -> Unit = { }

    override fun apply(target: Project) {
        // Registers a callback on the application of the Android Application plugin.
        // This allows the CustomPlugin to work whether it's applied before or after
        // the Android Application plugin.
        target.plugins.withType(BasePlugin::class.java) {
            //application or library
            with(target) {
                log("=========================== START【${this@AbsAndroidConfig}】 =========================")
                log("常见构建自定义的即用配方，展示如何使用Android Gradle插件的公共API和DSL:")
                log("https://github.com/android/gradle-recipes")
                onProject()
                val catalog = vlibs
                with(pluginManager) {
                    pluginConfigs()(catalog)
                }
                androidExtensionComponent?.apply {
                    finalizeDsl { android ->
                        with(android) {
                            androidExtensionConfig()(catalog)
                        }
                    }
                    androidComponentsExtensionConfig()(catalog)
                }

                //https://kotlinlang.org/docs/gradle-compiler-options.html#target-the-jvm
                tasks.withType<KotlinJvmCompile>().configureEach {
                    compilerOptions {
                        kotlinOptionsConfig()()
                    }
                }
                //和上面等效
                //tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask::class.java) {
                //    compilerOptions {
                //        androidConfig.kotlinOptionsConfig()(target)
                //        kotlinOptionsConfig()(target)
                //    }
                //}
                dependencies {
                    dependenciesConfig()(catalog)
                }
                log("=========================== END【${this@AbsAndroidConfig}】 =========================")
                //生成apk地址
                //https://github.com/android/gradle-recipes/blob/agp-8.4/allProjectsApkAction/README.md
                //com.android.build.gradle.internal.variant.VariantPathHelper.getApkLocation
                //com.android.build.gradle.internal.variant.VariantPathHelper.getDefaultApkLocation
                //com.android.build.gradle.tasks.PackageApplication

                //layout.buildDirectory.set(f.absolutePath)
                //修改as生成缓存的地址

                //transform
                //https://github.com/android/gradle-recipes/blob/agp-8.4/transformAllClasses/README.md
            }
        }
    }
}