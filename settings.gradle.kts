import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name


pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

plugins {
    id("io.github.5hmlA.vcl") version "24.10.01"
}

dependencyResolutionManagement {
    versionCatalogs {
        create("wings") {
            from("io.github.5hmla:vcatalog:24.09.29")
        }
    }
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "gradle-conventions"

fun java.nio.file.Path.isGradleProject(): Boolean = if (!isDirectory()) false else listDirectoryEntries().any {
    it.toString().endsWith("build.gradle.kts")
}

rootProject.projectDir.toPath().listDirectoryEntries().forEach {
    println("${it.name} >> ${it.isGradleProject()}")
}

val allGradleProject = rootProject.projectDir.toPath().listDirectoryEntries().filter {
    it.isGradleProject()
}

allGradleProject.forEachIndexed { index, path ->
    println(">>> ${path.name} --> $index")
    if (path.name == "conventions") {
        includeBuild(path) { name = path.name }
    }
}

//添加需要学习的项目,方便看gradle-recipes源码, 把此setting.gradle.kts放到根目录下,选择要看的模块即可
//includeBuild(allGradleProject[0].absolutePathString()) { name = "conventions" }


include(":app")
include(":lib-test")
include(":lib-java")