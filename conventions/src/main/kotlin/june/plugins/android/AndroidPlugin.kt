package june.plugins.android

import june.wing.AndroidCommonExtension
import june.wing.AndroidComponentsExtensions
import june.wing.ConventionConfig
import june.wing.chinaRepos
import june.wing.logDebug
import june.wing.logInfo
import june.wing.red
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.initialization.resolve.RepositoriesMode
import org.gradle.api.internal.artifacts.repositories.DefaultMavenArtifactRepository
import org.gradle.api.plugins.PluginManager
import org.gradle.kotlin.dsl.DependencyHandlerScope
import org.gradle.kotlin.dsl.buildscript
import org.gradle.kotlin.dsl.extra
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import java.io.File

open class AndroidPlugin : AbsAndroidPlugin() {

    private var androidConfig: Android? = null

    override fun onProject(project: Project) {
        androidConfig = AndroidBase()
        if (ConventionConfig.isRoomEnabled(project)) {
            androidConfig = AndroidRoom(androidConfig)
        }
        project.buildCacheDir()
        project.repoConfig()
    }

    override fun pluginConfigs(project: Project): PluginManager.(VersionCatalog) -> Unit = {
        androidConfig?.pluginConfigs(project)?.invoke(this, it)
    }


    override fun androidExtensionConfig(project: Project): AndroidCommonExtension.(VersionCatalog) -> Unit = {
        androidConfig?.androidExtensionConfig(project)?.invoke(this, it)
    }

    override fun androidComponentsExtensionConfig(project: Project): AndroidComponentsExtensions.(VersionCatalog) -> Unit = {
        androidConfig?.androidComponentsExtensionConfig(project)?.invoke(this, it)
    }

    override fun kotlinOptionsConfig(project: Project): KotlinJvmCompilerOptions.() -> Unit = {
        androidConfig?.kotlinOptionsConfig(project)?.invoke(this)
    }

    override fun dependenciesConfig(project: Project): DependencyHandlerScope.(VersionCatalog) -> Unit = {
        androidConfig?.dependenciesConfig(project)?.invoke(this, it)
    }

    private fun Project.buildCacheDir() {
        logDebug("========= Project.layout[buildDir] ${layout.buildDirectory.javaClass} ${layout.buildDirectory.asFile.get().absolutePath}")
        logDebug("👉 set『${ConventionConfig.PROP_BUILD_CACHE_DIR}=D』can change build cache dir to D:/0buildCache/")
        val buildDir = ConventionConfig.getBuildCacheRootDir(this)
        buildDir?.let {
            //https://github.com/gradle/gradle/issues/20210
            //https://docs.gradle.org/current/userguide/upgrading_version_8.html#deprecations
            val cachePath = "$it${ConventionConfig.DEFAULT_BUILD_CACHE_PATH}${rootProject.name}/${project.name}"
            layout.buildDirectory.set(File(cachePath))
            logInfo("👉『${project.name}』buildDir is relocated to -> ${project.layout.buildDirectory.asFile.get()} 🥱")
        }
    }

    private fun Project.repoConfig() {
        if (!project.gradle.extra.has("repositoriesMode")) {
            return
        }
        val repositoriesMode = project.gradle.extra["repositoriesMode"]
        if (repositoriesMode == RepositoriesMode.PREFER_SETTINGS || repositoriesMode == RepositoriesMode.FAIL_ON_PROJECT_REPOS) {
            logInfo("【${project.name}】-> repoConfig -> repositoriesMode=$repositoriesMode".red)
            return
        }

        buildscript {
            try {
                repositories.chinaRepos()
            } catch (e: Exception) {
                logger.warn("Failed to configure buildscript repositories", e)
            }
            repositories.forEach {
                logDebug("> Project.buildscript repositories ${it.name} >  =========================")
            }
        }

        try {
            repositories.chinaRepos()
        } catch (e: Exception) {
            logger.warn(
                """
                    ${e.message}
                    报错原因是 repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS) 导致的
                    修改为如下设置:
                        dependencyResolutionManagement {
                            repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
                        }
                """.trimIndent().red,
                e
            )
        }
        repositories.forEach {
            logDebug("🔔> Project.repositories ${it.name} > ${(it as DefaultMavenArtifactRepository).url} =========================")
        }
    }
}