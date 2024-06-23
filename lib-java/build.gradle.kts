import wing.publishMavenCentral

plugins {
    alias(libs.plugins.kotlin.jvm)
}

buildscript {
    dependencies {
        classpath("io.github.5hmlA:conventions:2.1.2")
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_18
    targetCompatibility = JavaVersion.VERSION_18
}

publishMavenCentral("lib-java-test", "java")