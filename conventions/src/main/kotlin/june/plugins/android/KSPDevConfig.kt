package june.plugins.android

import june.wing.AndroidCommonExtension
import june.wing.vlibs
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies


fun AndroidCommonExtension.kspSourceSets() {
    //KSP 会为每个 variant（buildType + flavor）生成目录：
    // build/generated/ksp/<variantName>/kotlin/
    //其中 <variant> 取决于你的构建变体，比如：
    //    debug ➜ build/generated/ksp/debug/kotlin/
    //对于 flavor + buildType，例如 freeDebug，则是：
    // flavor+debug ➜ build/generated/ksp/freeDebug/kotlin/

    //自定义ksp生成代码所在目录
    //ksp {
    //    arg("ksp.generatedDir", "$buildDir/custom-ksp-output")
    //}
//    srcDirs(
//        "build/generated/ksp/main/kotlin",
//        "build/generated/ksp/main/java"
//    )
    //ksp还会根据buildType生成目录
    //不用添加src好像自动会处理


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