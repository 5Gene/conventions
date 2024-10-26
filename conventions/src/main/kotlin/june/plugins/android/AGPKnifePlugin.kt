package june.plugins.android

import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.instrumentation.InstrumentationScope
import com.android.build.api.variant.Variant
import june.knife.KnifeImpl
import june.knife.TaskListenApk
import june.knife.TransformConfigImpl
import june.knife.VariantKnifeActionImpl
import june.knife.asm.KnifeAsmClassVisitorFactory
import june.knife.asm.toModifyConfig
import june.wing.AndroidComponentsExtensions
import june.wing.isAndroidApplication
import june.wing.log
import june.wing.red
import june.wing.toStr
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.kotlin.dsl.register

//https://developer.android.google.cn/build/extend-agp?hl=zh-cn
class AGPKnifePlugin : AbsAndroidPlugin() {
    override fun androidComponentsExtensionConfig(project: Project): AndroidComponentsExtensions.(VersionCatalog) -> Unit =
        { _ ->

            val knifeImpl = KnifeImpl()
            val knifeExtension = knifeImpl.createExtension(project)

            val variantKnifeActionImpl = VariantKnifeActionImpl()
            variantKnifeActionImpl.createExtension(knifeExtension)

            project.log("knife > knifeExtension:${knifeImpl.onVariants}")

            /**
             * plugin中的onVariants{}会优先执行 ,所以app中建议用 beforeVariants{}遍历和配置
             */
            onVariants(selector().all()) { variant: Variant ->
                knifeImpl.onVariants?.let {
                    it.invoke(variant)//variant回调到build.gradle
                    //build.gradle中通过utility{}注册asmTransform到variantAction
                    project.log("knife > onVariant:${variant.name}")
                    //存在listenArtifact的时候才创建task
                    tryListenArtifact(variantKnifeActionImpl, project, variant)
                    //transform
                    tryAsmTransform(variantKnifeActionImpl, project, variant)
                }
            }
        }

    private fun tryAsmTransform(
        variantAction: VariantKnifeActionImpl,
        project: Project,
        variant: Variant
    ) {
        project.log("knife > tryAsmTransform:${variant.name}  ${variantAction.transformConfigs}")
        variantAction.transformConfigs?.let { asmTransform ->

            val transformConfigs = TransformConfigImpl()
            //build.gradle中配置的asmTransform {} 这个回调就是configs,这里执行asmTransform {}中代码块才会执行
            asmTransform(transformConfigs)

            if (transformConfigs.modifyConfigs.isEmpty()) {
                project.log("knife > tryAsmTransform:${variant.name} no transformConfigs skip >>".red)
                return
            }

            //https://developer.android.google.cn/reference/tools/gradle-api/8.3/null/com/android/build/api/variant/Instrumentation
            //instrumentation.excludes.add("com/example/donotinstrument/**")
            //variant.instrumentation.excludes.add("**/*Test")
            //一个*和两个*的区别【两个*表示匹配之前或者之后所有】
            //  两个*表示多级匹配【**/EmptyAllMethod,匹配任意包下的所有名为EmptyAllMethod的类】
            //  两个*表示多级匹配【android**,匹配任意android开头的所有包下的所有类】
            //  一个*表示类名匹配【com/osp/app/*,app包类下的所有】
            //  一个*表示类名匹配【**/EmptyAllMethod*,匹配EmptyAllMethod开头的所有类】
            //The set of glob patterns to exclude from instrumentation.
            variant.instrumentation.excludes.addAll(transformConfigs.excludes)

            //https://github1s.com/android/gradle-recipes/blob/agp-8.2/asmTransformClasses/build-logic/plugins/src/main/kotlin/CheckAsmTransformationTask.kt
            //https://github1s.com/android/gradle-recipes/blob/agp-8.2/asmTransformClasses/build-logic/plugins/src/main/kotlin/CustomPlugin.kt
            //COPY_FRAMES是默认值
            //FramesComputationMode.COPY_FRAMES 此Mode修改方法和操作变量后要自己计算
            transformConfigs.framesComputationMode?.let {
                variant.instrumentation.setAsmFramesComputationMode(it)
                //variant.instrumentation.setAsmFramesComputationMode(
                //    FramesComputationMode.COMPUTE_FRAMES_FOR_INSTRUMENTED_METHODS
                //)
            }

            project.log("knife > tryAsmTransform:${variant.name}  ${transformConfigs.modifyConfigs.toStr()}".red)
            val modifyConfigs = transformConfigs.modifyConfigs.map {
                it.toModifyConfig()
            }

            variant.instrumentation.transformClassesWith(
                KnifeAsmClassVisitorFactory::class.java,
                InstrumentationScope.ALL,
            ) { params ->
                params.buildType.set(variant.buildType)
                params.flavorName.set(variant.flavorName)
                params.variantName.set(variant.name)
                val mapValues = modifyConfigs.groupBy { it.targetMethod.fullClass }.mapValues {
                    it.value.groupBy { it.targetMethod.methodName }
                }
                mapValues.forEach { (key, value) ->
                    project.log("knife > tryAsmTransform:${variant.name} 👇👇👇👇👇👇👇👇👇👇 $key 👇👇👇👇👇👇👇👇👇👇".red)
                    value.forEach { (t, u) ->
                        project.log("knife > tryAsmTransform:${variant.name}       $t > ${u.map { it.methodAction }}".red)
                    }
                    project.log("knife > tryAsmTransform:${variant.name} 👆👆👆👆👆👆👆👆👆👆 $key 👆👆👆👆👆👆👆👆👆👆 ".red)
                }
                params.classConfigs.set(mapValues)
                val modifyClasses = modifyConfigs.map { it.targetMethod.fullClass }.toSet()
                params.targetClasses.set(modifyClasses)
            }
        }
    }

    private fun tryListenArtifact(
        variantAction: VariantKnifeActionImpl,
        project: Project,
        variant: Variant
    ) {
        project.log("knife > tryListenArtifact:${variant.name}")
        variantAction.listenArtifact?.let {
            val taskProvider =
                project.tasks.register<TaskListenApk>("listenApkFor${variant.name}") {
                    apkFolder.set(variant.artifacts.get(SingleArtifact.APK))
                    builtArtifactsLoader.set(variant.artifacts.getBuiltArtifactsLoader())
                    listenArtifact = variantAction.listenArtifact
                }
            //https://developer.android.google.cn/build/releases/gradle-plugin-api-updates?hl=zh-cn
            if (project.isAndroidApplication) {
                variant.artifacts.use(taskProvider).wiredWith {
                    it.apkFolder
                }.toListenTo(SingleArtifact.APK)
            } else {
                //https://developer.android.google.cn/build/releases/gradle-plugin-api-updates?hl=zh-cn
                variant.artifacts.use(taskProvider).wiredWith {
                    it.aarFile
                }.toListenTo(SingleArtifact.AAR)
            }
        }
    }
}