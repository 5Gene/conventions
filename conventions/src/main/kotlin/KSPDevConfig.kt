import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import wing.AndroidCommonExtension
import wing.srcDirs
import wing.vlibs


fun AndroidCommonExtension.kspSourceSets() {
    srcDirs(
        "build/generated/ksp/main/kotlin",
        "build/generated/ksp/main/java"
    )
}

fun Project.kspDevDependencies() {
    val libs = vlibs
    dependencies {
        libs.findLibrary("google-auto-service-anno").ifPresent {
            add("implementation", it)
        }
        libs.findLibrary("gene-auto-service").ifPresent {
            add("implementation", it)
        }
        libs.findLibrary("gene-ksp-poe").ifPresent {
            add("implementation", it)
        }
        libs.findLibrary("ksp-process-api").ifPresent {
            add("implementation", it)
        }
    }
}