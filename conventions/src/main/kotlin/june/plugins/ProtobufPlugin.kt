package june.plugins

import com.google.protobuf.gradle.ProtobufExtension
import june.wing.logDebug
import june.wing.logInfo
import june.wing.vlibs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.findByType
import kotlin.jvm.optionals.getOrDefault

/**
 * 插件引入方式
 * ```kotlin
 * apply<june.plugins.ProtobufPlugin>()
 * ```
 */

class ProtobufPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        with(target) {
            logDebug("=========================== START【${this@ProtobufPlugin}】 =========================")
            with(pluginManager) {
                apply("com.google.protobuf")
            }
            val pbExtension = extensions.findByType<ProtobufExtension>()
            val catalog = vlibs
            if (catalog == null) {
                logger.warn("Version catalog 'vcl' not found. Protobuf plugin configuration may be incomplete.")
            }
            
            pbExtension?.apply {
                protoc {
                    artifact = "com.google.protobuf:protoc:${
                        catalog?.findVersion("google-protobuf")?.getOrDefault("4.30.2") 
                            ?: "4.30.2"
                    }"
                }
                generateProtoTasks {
                    all().forEach { task ->
                        task.builtins {
                            maybeCreate("java").apply {
                                option("lite")
                            }
                            maybeCreate("kotlin").apply {
                                option("lite")
                            }
                        }
                    }
                }
            }
            dependencies {
                catalog?.findLibrary("protobuf-kotlin")?.ifPresent {
                    add("implementation", it)
                } ?: logger.warn("protobuf-kotlin library not found in version catalog, skipping dependency")
            }

            logInfo("protobuf文档: https://protobuf.dev/")
            logInfo("最佳实践: https://protobuf.dev/programming-guides/api/")
            logInfo("   - 不要重复使用标签号码")
            logInfo("   - 为已删除的字段保留标签号")
            logInfo("   - 为已删除的枚举值保留编号")
            logInfo("   - 不要更改字段的类型")
            logInfo("   - 不要发送包含很多字段的消息")
            logInfo("   - 不要更改字段的默认值")
            logDebug("=========================== END【${this@ProtobufPlugin}】 =========================")
        }
    }
}