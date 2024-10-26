import june.wing.publish5hmlA
import june.wing.publishGradlePluginSet
import org.gradle.kotlin.dsl.assign

plugins {
    alias(vcl.plugins.kotlin.jvm)
    id("io.github.5hmlA.protobuf")
    id("com.gradle.plugin-publish") version "1.3.0"
}

sourceSets.main {
    java.srcDirs(
//        """$rootDir\surgery-doctor-tryfinally\src\main\java""",
//        """$rootDir\surgery-doctor-arouter\src\main\java"""
    )
}

group = "test.test"
//publishKotlinMavenCentral("lib-java-test")
publish5hmlA("test", "kotlin", false)
//publishJavaMavenCentral("lib-java-test")

publishGradlePluginSet {
    register("plugin-test") {
        id = "${group}.test"
        displayName = "gracle version catalog"
        description = "gracle version catalog"
        tags = listOf("config", "versionCatalog", "convention")
        implementationClass = "june.VCatalogPlugin"
    }
}