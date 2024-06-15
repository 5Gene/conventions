package knife

import com.android.build.api.instrumentation.FramesComputationMode
import com.android.build.api.variant.Variant
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware

/**
 * 定义功能接口
 */
interface Knife {
    fun onVariants(action: (Variant) -> Unit)
}

/**
 * 定义功能 Extension
 * 具体实现委托给 KnifeImpl
 * plugin里面用到的实际上是 KnifeImpl
 */
abstract class KnifeExtension(private val knifeExtensionImpl: KnifeImpl) : Knife, ExtensionAware {
    override fun onVariants(action: (Variant) -> Unit) {
        knifeExtensionImpl.onVariants = action
    }
}

/**
 * 定义功能具体实现
 */
class KnifeImpl : Knife {
    var onVariants: ((Variant) -> Unit)? = null
    override fun onVariants(action: (Variant) -> Unit) {
        onVariants = action
    }

    fun createExtension(project: Project): KnifeExtension {
        return project.extensions.create(
            "knife", KnifeExtension::class.java, this
        )
    }
}

interface VariantKnifeAction {
    fun onArtifactBuilt(action: (String) -> Unit)

    fun asmTransform(configs: TransformConfig.() -> Unit)
}

abstract class VariantKnifeActionExtension(private val variantActionImpl: VariantKnifeActionImpl) : VariantKnifeAction {
    override fun onArtifactBuilt(action: (String) -> Unit) {
        variantActionImpl.onArtifactBuilt(action)
    }

    override fun asmTransform(configs: TransformConfig.() -> Unit) {
        variantActionImpl.asmTransform(configs)
    }
}

class VariantKnifeActionImpl : VariantKnifeAction {

    var listenArtifact: ((String) -> Unit)? = null

    var transformConfigs: (TransformConfig.() -> Unit)? = null

    override fun onArtifactBuilt(action: (String) -> Unit) {
        //外部build.gradle中注册listenArtifact回调，在onVariants的时候执行
        listenArtifact = action
    }

    override fun asmTransform(configs: TransformConfig.() -> Unit) {
        //外部build.gradle中注册transformConfigs回调，在onVariants的时候执行
        transformConfigs = configs
    }

    //怀孕,生成子extension
    fun createExtension(knifeExtension: KnifeExtension): VariantKnifeActionExtension {
        return knifeExtension.extensions.create(
            "utility", VariantKnifeActionExtension::class.java, this
        )
    }
}

//fun Project.asmTransform(config: TransformConfig.() -> Unit) =
//    extensions.getByType<KnifeExtension>().extensions.getByType<VariantKnifeAction>()
//        .asmTransform(config)
//
//fun Project.onArtifactBuilt(listen: (String) -> Unit) =
//    extensions.getByType<KnifeExtension>().extensions.getByType<VariantKnifeAction>()
//        .onArtifactBuilt(listen)

interface TransformConfig {
    fun configs(vararg configs: String)
    val excludes: MutableSet<String>

    /**
     * 排除要asm处理的类
     */
    fun execludes(vararg configs: String)
    var framesComputationMode: FramesComputationMode?
}

class TransformConfigImpl : TransformConfig {
    val modifyConfigs = mutableListOf<String>()
    override fun configs(vararg configs: String) {
        modifyConfigs.addAll(configs)
    }

    override val excludes: MutableSet<String> = mutableSetOf()

    override fun execludes(vararg configs: String) {
        excludes.addAll(configs)
    }

    override var framesComputationMode: FramesComputationMode? = null
}