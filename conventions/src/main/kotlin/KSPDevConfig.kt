import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.buildscript
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.repositories
import wing.findVersionStr
import wing.log

/**
 * 插件引入方式
 * ```kotlin
 * apply<ProtobufConfig>()
 * ```
 */

class KSPDevConfig : Plugin<Project> {

    override fun apply(target: Project) {
        with(target) {
            val libs = extensions.findByType<VersionCatalogsExtension>()?.named("libs")
            buildscript {
                repositories {
                    gradlePluginPortal()
                    mavenCentral()
                    maven {
                        url = uri("https://plugins.gradle.org/m2/")
                    }
                }
                dependencies {
                    val kspVersion = libs.findVersionStr("ksp") ?: "2.0.0-1.0.21"
                    classpath("com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin:$kspVersion")
                }
            }

            log("=========================== START【${this@KSPDevConfig}】 =========================")
            with(pluginManager) {
                apply("com.google.devtools.ksp")
            }

            dependencies {
                val wings = extensions.findByType<VersionCatalogsExtension>()?.named("wings")
                val auto_service = wings?.findVersionStr("auto-service") ?: libs?.findVersionStr("auto-service") ?: "0.0.8"
                add("ksp", "io.github.5hmla:auto-service:$auto_service")

                val ksp_poe = wings?.findVersionStr("ksp-poe") ?: libs?.findVersionStr("ksp-poe") ?: "0.0.4"
                add("implementation", "io.github.5gene:ksp-poe:$ksp_poe")
            }
            log("=========================== START【${this@KSPDevConfig}】 =========================")
        }
    }
}
