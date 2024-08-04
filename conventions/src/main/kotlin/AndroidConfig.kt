import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.internal.artifacts.repositories.DefaultMavenArtifactRepository
import org.gradle.api.plugins.PluginManager
import org.gradle.kotlin.dsl.DependencyHandlerScope
import org.gradle.kotlin.dsl.buildscript
import org.gradle.kotlin.dsl.repositories
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmCompilerOptions
import wing.AndroidCommonExtension
import wing.AndroidComponentsExtensions
import wing.chinaRepos
import wing.log
import wing.red
import java.io.File

open class AndroidConfig : AbsAndroidConfig() {

    private var androidConfig: Android? = null

    context(Project) override fun onProject() {
        androidConfig = AndroidBase()
        if (findProperty("config.android.room") == "true") {
            androidConfig = AndroidRoom(androidConfig)
        }
        buildCacheDir()
        repoConfig()
    }

    context(Project) override fun pluginConfigs(): PluginManager.(VersionCatalog) -> Unit = {
        androidConfig?.pluginConfigs()?.invoke(this, it)
    }


    context(Project) override fun androidExtensionConfig(): AndroidCommonExtension.(VersionCatalog) -> Unit = {
        androidConfig?.androidExtensionConfig()?.invoke(this, it)
    }

    context(Project) override fun androidComponentsExtensionConfig(): AndroidComponentsExtensions.(VersionCatalog) -> Unit = {
        androidConfig?.androidComponentsExtensionConfig()?.invoke(this, it)
    }

    context(Project) override fun kotlinOptionsConfig(): KotlinJvmCompilerOptions.() -> Unit = {
        androidConfig?.kotlinOptionsConfig()?.invoke(this)
    }

    context(Project) override fun dependenciesConfig(): DependencyHandlerScope.(VersionCatalog) -> Unit = {
        androidConfig?.dependenciesConfig()?.invoke(this, it)
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
            repositories.removeAll { true }
            repositories {
                chinaRepos()
            }
            repositories.forEach {
                log("> Project.buildscript repositories ${it.name} >  =========================")
            }
        }

        repositories.forEach {
            log("> Project.repositories ${it.name} > ${it.javaClass} =========================")
        }
        try {
            repositories {
                chinaRepos()
            }
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
            log("> Project.repositories ${it.name} > ${(it as DefaultMavenArtifactRepository).url} =========================")
        }
    }
}