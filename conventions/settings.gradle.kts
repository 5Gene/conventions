pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
    versionCatalogs {
        create("libs") {
            from("io.github.5hmla:vcatalog:24.09.29")
        }
    }
}

rootProject.name = "conventions"



//include(":conventions")

//https://developer.android.google.cn/build/publish-library/upload-library?hl=zh-cn#kts