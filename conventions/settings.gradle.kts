pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

plugins {
    id("io.github.5hmlA.vcl") version "2025.08.05"
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "conventions"



//include(":conventions")

//https://developer.android.google.cn/build/publish-library/upload-library?hl=zh-cn#kts