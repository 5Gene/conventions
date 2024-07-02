import wing.publishMavenCentral

plugins {
    alias(libs.plugins.kotlin.jvm)
}

buildscript {
    dependencies {
        classpath(wings.conventions)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_18
    targetCompatibility = JavaVersion.VERSION_18
}

sourceSets.main {
    java.srcDirs(
//        """$rootDir\surgery-doctor-tryfinally\src\main\java""",
//        """$rootDir\surgery-doctor-arouter\src\main\java"""
    )
}

publishMavenCentral("lib-java-test", "java")