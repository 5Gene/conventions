import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.instrumentation.InstrumentationScope
import com.android.build.api.variant.Variant
import knife.KnifeImpl
import knife.TaskListenApk
import knife.TransformConfigImpl
import knife.VariantKnifeActionImpl
import knife.asm.SurgeryAsmClassVisitorFactory
import knife.asm.toModifyConfig
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.kotlin.dsl.register
import wing.AndroidComponentsExtensions
import wing.isAndroidApplication
import wing.log
import wing.red
import wing.toStr

//https://developer.android.google.cn/build/extend-agp?hl=zh-cn
class AGPKnifePlugin : AbsAndroidConfig() {
    context(Project) override fun androidComponentsExtensionConfig(): AndroidComponentsExtensions.(VersionCatalog) -> Unit =
        { _ ->

            val knifeImpl = KnifeImpl()
            val knifeExtension = knifeImpl.createExtension(project)

            val variantKnifeActionImpl = VariantKnifeActionImpl()
            variantKnifeActionImpl.createExtension(knifeExtension)

            log("knife > knifeExtension:${knifeImpl.onVariants}")

            /**
             * plugin中的onVariants{}会优先执行 ,所以app中建议用 beforeVariants{}遍历和配置
             */
            onVariants(selector().all()) { variant: Variant ->
                knifeImpl.onVariants?.let {
                    log("knife > onVariant:${variant.name}")
                    variantKnifeActionImpl.doListenArtifact = {
                        //存在listenArtifact的时候才创建task
                        tryListenArtifact(variantKnifeActionImpl, project, variant)
                    }
                    //transform
                    variantKnifeActionImpl.doAsmTransform = {
                        tryAsmTransform(variantKnifeActionImpl, project, variant)
                    }
                    it.invoke(variant)
                }
            }
        }

    private fun tryAsmTransform(
        variantAction: VariantKnifeActionImpl,
        project: Project,
        variant: Variant
    ) {
        project.log("knife > tryAsmTransform:${variant.name}  ${variantAction.transformConfigs}")
        variantAction.transformConfigs?.let { configs ->
            val transformConfigs = TransformConfigImpl()
            configs(transformConfigs)
            if (transformConfigs.modifyConfigs.isEmpty()) {
                println(transformConfigs.modifyConfigs.toStr().red)
                return
            }
            project.log("knife > tryAsmTransform:${variant.name}  ${transformConfigs.modifyConfigs.toStr()}".red)

            val modifyConfigs = transformConfigs.modifyConfigs.map {
                it.toModifyConfig()
            }

            //https://github1s.com/android/gradle-recipes/blob/agp-8.2/asmTransformClasses/build-logic/plugins/src/main/kotlin/CheckAsmTransformationTask.kt
            //https://github1s.com/android/gradle-recipes/blob/agp-8.2/asmTransformClasses/build-logic/plugins/src/main/kotlin/CustomPlugin.kt
//            variant.instrumentation.setAsmFramesComputationMode(
//                FramesComputationMode.COMPUTE_FRAMES_FOR_INSTRUMENTED_METHODS
//            )
            //COPY_FRAMES是默认值
            //FramesComputationMode.COPY_FRAMES 此Mode修改方法和操作变量后要自己计算
            variant.instrumentation.transformClassesWith(
                SurgeryAsmClassVisitorFactory::class.java,
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
                    value.forEach { t, u ->
                        project.log("knife > tryAsmTransform:${variant.name}       $t > ${u.map { it.methodAction ?: "EmptyMethod" }}".red)
                    }
                    project.log("knife > tryAsmTransform:${variant.name} 👆👆👆👆👆👆👆👆👆👆 $key 👆👆👆👆👆👆👆👆👆👆 ".red)
                }
                params.methodConfigs.set(mapValues)
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