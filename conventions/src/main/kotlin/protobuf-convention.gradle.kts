import june.wing.log
import june.wing.vlibs

//https://docs.gradle.org/current/userguide/custom_plugins.html
//预编译脚本插件
//预编译脚本插件在执行之前会被编译成类文件并打包成 JAR。这些插件使用 Groovy 或 Kotlin DSL，
// 而不是纯 Java、Kotlin 或 Groovy。它们最好用作跨项目共享构建逻辑的约定插件，或者作为整齐组织构建逻辑的一种方式。
//要创建预编译脚本插件，您可以：
// - 使用 Gradle 的 Kotlin DSL - 插件是一个.gradle.kts文件，并应用id("kotlin-dsl").
// - 使用 Gradle 的 Groovy DSL - 该插件是一个.gradle文件，并应用id("groovy-gradle-plugin").

//要应用预编译脚本插件，您需要知道其ID。 ID 源自插件脚本的文件名及其（可选）包声明。
//例如，该脚本src/main/*/java-library.gradle(.kts)的插件 ID 为java-library（假设它没有包声明）。
//同样，只要它的包声明为 ，src/main/*/my/java-library.gradle(.kts)就有一个插件 ID 。my.java-librarymy


//此插件引入方式 xxx.gradle.kts 文件名字就是插件名字
// (发布插件的时候默认文件名就是插件名，但是上传插件的是必须要加上grpup前缀)
//plugins {
//    本地引用
//    id("protobuf-convention")
//     发布插件后远程引用
//    id("io.github.5hmlA.protobuf-convention")
//}

log("=========================== START【${this}】 =========================")

plugins {
    id("com.google.protobuf")
}

protobuf {
    protoc {
        // By default the plugin will search for the protoc executable in the system search path. We recommend you to take the advantage of pre-compiled protoc that we have published on Maven Central:
        artifact = "com.google.protobuf:protoc:${vlibs.findVersion("protobuf").get()}"
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

println("protobuf文档: https://protobuf.dev/")
println("最佳实践: https://protobuf.dev/programming-guides/api/")
println("   - 不要重复使用标签号码 ")
println("   - 为已删除的字段保留标签号")
println("   - 为已删除的枚举值保留编号")
println("   - 不要更改字段的类型 ")
println("   - 不要发送包含很多字段的消息 ")
println("   - 不要更改字段的默认值 ")
println("   - 不要更改字段的默认值 ")

dependencies {
    add("implementation", vlibs.findLibrary("protobuf-kotlin").get())
//    implementation("com.google.protobuf:protobuf-gradle-plugin:0.9.4")
}
log("=========================== END【${this}】 =========================")