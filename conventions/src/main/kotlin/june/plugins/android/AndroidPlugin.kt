package june.plugins.android

import june.wing.*
import june.wing.AndroidCommonExtension
import june.wing.AndroidComponentsExtensions
import june.wing.chinaRepos
import june.wing.log
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.internal.artifacts.repositories.DefaultMavenArtifactRepository
import org.gradle.api.plugins.PluginManager
import org.gradle.kotlin.dsl.DependencyHandlerScope
import org.gradle.kotlin.dsl.buildscript
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import java.io.File

open class AndroidPlugin : AbsAndroidPlugin() {

    private var androidConfig: Android? = null

    override fun onProject(project: Project) {
        androidConfig = AndroidBase()
        if (project.findProperty("config.android.room") == "true") {
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
        log("========= Project.layout[buildDir] ${layout.buildDirectory.javaClass} ${layout.buildDirectory.asFile.get().absolutePath}")
        log("👉 set『build.cache.root.dir=D』can change build cache dir to D:/0buildCache/")
        //log("========= Project.buildDir ${buildDir} =========================")
        val buildDir = properties["build.cache.root.dir"] ?: System.getenv("build.cache.root.dir")
        buildDir?.let {
            //https://github.com/gradle/gradle/issues/20210
            //https://docs.gradle.org/current/userguide/upgrading_version_8.html#deprecations
            layout.buildDirectory.set(File("$it:/0buildCache/${rootProject.name}/${project.name}"))
            log("👉『${project.name}』buildDir is relocated to -> ${project.layout.buildDirectory.asFile.get()} 🥱")
            //buildDir = File("E:/0buildCache/${rootProject.name}/${project.name}")
        }
    }

    private fun Project.repoConfig() {
        buildscript {
            try {
                repositories.chinaRepos()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            repositories.forEach {
                log("> Project.buildscript repositories ${it.name} >  =========================")
            }
        }

        try {
            repositories.chinaRepos()
        } catch (e: Exception) {
            log(
                """
                        ${e.message}\n
                        报错原因是 repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS) 导致的
                        修改为如下设置:
                            dependencyResolutionManagement {
                                repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
                            }
                        """.trimIndent().red
            )
        }
        repositories.forEach {
            log("🔔> Project.repositories ${it.name} > ${(it as DefaultMavenArtifactRepository).url} =========================")
        }
    }
}