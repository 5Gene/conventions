import wing.publish5hmlA

plugins {
    alias(libs.plugins.kotlin.jvm)
}

buildscript {
    dependencies {
        classpath(wings.gene.conventions)
    }
}

//java {
//    sourceCompatibility = JavaVersion.VERSION_17
//    targetCompatibility = JavaVersion.VERSION_17
//}

sourceSets.main {
    java.srcDirs(
//        """$rootDir\surgery-doctor-tryfinally\src\main\java""",
//        """$rootDir\surgery-doctor-arouter\src\main\java"""
    )
}

//publishKotlinMavenCentral("lib-java-test")
publish5hmlA("test", "kotlin", false)
//publishJavaMavenCentral("lib-java-test", true)