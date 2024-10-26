package june.plugins

import com.google.protobuf.gradle.ProtobufExtension
import june.wing.log
import june.wing.vlibs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.findByType

/**
 * 插件引入方式
 * ```kotlin
 * apply<june.plugins.ProtobufPlugin>()
 * ```
 */

class ProtobufPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        with(target) {
            log("=========================== START【${this@ProtobufPlugin}】 =========================")
            with(pluginManager) {
                apply("com.google.protobuf")
            }
            val pbExtension = extensions.findByType<ProtobufExtension>()
            val catalog = vlibs
            pbExtension?.apply {
                protoc {
                    artifact = "com.google.protobuf:protoc:${catalog.findVersion("protobuf").get()}"
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
                add("implementation", catalog.findLibrary("protobuf-kotlin").get())
            }

            println("protobuf文档: https://protobuf.dev/")
            println("最佳实践: https://protobuf.dev/programming-guides/api/")
            println("   - 不要重复使用标签号码 ")
            println("   - 为已删除的字段保留标签号")
            println("   - 为已删除的枚举值保留编号")
            println("   - 不要更改字段的类型 ")
            println("   - 不要发送包含很多字段的消息 ")
            println("   - 不要更改字段的默认值 ")
            println("   - 不要更改字段的默认值 ")
            log("=========================== START【${this@ProtobufPlugin}】 =========================")
        }
    }
}