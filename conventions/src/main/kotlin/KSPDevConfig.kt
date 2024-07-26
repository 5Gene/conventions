import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.findByType
import wing.log
import kotlin.jvm.optionals.getOrNull

/**
 * 插件引入方式
 * ```kotlin
 * apply<ProtobufConfig>()
 * ```
 */

class KSPDevConfig : Plugin<Project> {

    override fun apply(target: Project) {
        with(target) {
            log("=========================== START【${this@KSPDevConfig}】 =========================")
            with(pluginManager) {
                apply("com.google.devtools.ksp")
            }

            dependencies {
                val libs = extensions.findByType<VersionCatalogsExtension>()?.named("libs")
                val auto_service = libs?.findVersion("auto-service")?.getOrNull()?.toString() ?: "0.0.8"
                add("ksp", "io.github.5hmla:auto-service:$auto_service")

                val ksp_poe = libs?.findVersion("ksp-poe")?.getOrNull()?.toString() ?: "0.0.2"
                add("implementation", "io.github.5gene:ksp-poe:$ksp_poe")
            }
            log("=========================== START【${this@KSPDevConfig}】 =========================")
        }
    }
}