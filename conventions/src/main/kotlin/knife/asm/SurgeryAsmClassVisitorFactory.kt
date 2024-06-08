package knife.asm

import com.android.build.api.instrumentation.AsmClassVisitorFactory
import com.android.build.api.instrumentation.ClassContext
import com.android.build.api.instrumentation.ClassData
import com.android.build.api.instrumentation.InstrumentationParameters
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import wing.green
import wing.lookDown
import wing.lookup

abstract class SurgeryInstrumentationParameters : InstrumentationParameters {

    @get:Input
    abstract val buildType: Property<String>

    @get:Input
    abstract val flavorName: Property<String>

    @get:Input
    abstract val variantName: Property<String>

    @get:Input
    @get:Optional
    //Map<类,Map<方法，修改> //这些类的哪些方法要做什么修改
    abstract val methodConfigs: MapProperty<String, Map<String, List<ModifyConfig>>>

    @get:Input
    @get:Optional
    abstract val targetClasses: SetProperty<String>
}

//必须是抽象类，会自动实现部分方法
abstract class SurgeryAsmClassVisitorFactory :
    AsmClassVisitorFactory<SurgeryInstrumentationParameters> {

//    gradle会自动生成实现
//    override val parameters: Property<SurgeryInstrumentationParameters>
//        get() =

    /**
     * isInstrumentable 返回true之后才执行，且在同线程
     */
    override fun createClassVisitor(
        classContext: ClassContext,
        nextClassVisitor: ClassVisitor
    ): ClassVisitor {
        //classContext.currentClassData.className 是正常的完整类名 .分割
        //inernalClass 是asm的类名 /分割
        return KnifeClassMethodVisitor(
            parameters.get().methodConfigs.get()[classContext.currentClassData.className]!!,
            instrumentationContext.apiVersion.get(),
            nextClassVisitor
        )
    }

    override fun isInstrumentable(classData: ClassData): Boolean {
        return parameters.get().targetClasses.get().contains(classData.className)
    }
}

//https://www.kingkk.com/2020/08/ASM%E5%8E%86%E9%99%A9%E8%AE%B0/
/**
 * @param methodConfigs 当前类要处理的所有方法
 *
 * 对某个类执行ASM处理
 */
class KnifeClassMethodVisitor(
    private val methodConfigs: Map<String, List<ModifyConfig>>,
    private val apiVersion: Int,
    cv: ClassVisitor,
) : ClassVisitor(apiVersion, cv) {

    private var internalClass = ""

    override fun visit(
        version: Int,
        access: Int,
        name: String,
        signature: String?,
        superName: String?,
        interfaces: Array<out String>?
    ) {
        internalClass = name
        asmLog(0, "KnifeClassMethodVisitor >> ${internalClass.lookDown} ")
        super.visit(version, access, name, signature, superName, interfaces)
    }


    private fun doEmptyMethodVisitor(
        isInit: Boolean,
        access: Int,
        apiVersion: Int,
        name: String,
        descriptor: String,
        visitMethod: MethodVisitor,
    ) = if (isInit) {
        EmptyInitFunctionVisitor(apiVersion, name, descriptor, visitMethod)
    } else {
        EmptyMethodVisitor(apiVersion, access, name, descriptor, visitMethod)
    }


    /**
     * 访问类的方法。如果方法匹配目标方法，则返回一个自定义的 MethodVisitor 来修改它。
     *
     * #### 参数descriptor详解
     * 举例 **```(I)Ljava/lang/String;```**
     * - 参数列表：由圆括号 () 包围。
     *      - (I) 表示该方法有一个参数，其类型为 int。在描述符中，I 表示 int 类型。
     * - 返回类型：在圆括号之后描述。
     *      - Ljava/lang/String; 表示返回类型为 java.lang.String。在描述符中，对象类型以 L 开头，后跟类的完全限定名，最后以分号 ; 结尾。
     *
     *  - **对象类型：** L<classname>; 表示对象类型，其中 classname 是类的完全限定名
     *  - **数组类型：** [<type> 表示数组类型，其中 <type> 可以是基本类型、对象类型或另一种数组类型。例如，[I 表示 int 数组，[[Ljava/lang/String; 表示 String 的二维数组。
     *
     *
     * @param access 方法的访问标志（见 Opcodes）
     *
     * @param name 方法的名称
     *
     * @param descriptor 方法的描述符（见 Type）
     *
     * @param signature 方法的签名（可选，用于泛型）
     *
     * @param exceptions 方法的异常类的内部名称
     *
     * @return 用于访问方法代码的 MethodVisitor
     */
    override fun visitMethod(
        access: Int,
        name: String,
        descriptor: String,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor? {
        val visitMethod = super.visitMethod(access, name, descriptor, signature, exceptions)
        if (access.isMethodIgnore()) {
            //抽象方法不处理
            return visitMethod
        }

        val isInit = name == "<init>" || name == "<clinit>"
        val fullMethodName = "$internalClass#$name"

        val wildcard = methodConfigs["*"]
        if (wildcard != null) {
            //这里表示匹配类的任意方法
            val modifyConfig = wildcard.first()
            //只取第一个方法的匹配方案，看descriptor
            val targetDescriptor = modifyConfig.targetMethod.descriptor
            if (targetDescriptor == "*") {
                //目标方法的descriptor规则为通配符，则忽略descriptor匹配，也就是说这个类的所有方法都匹配，全置空
                asmLog(msg = "KnifeClassMethodVisitor >> need empty all method[*] -> in [$internalClass]: method=[${name}], descriptor=[${descriptor}], signature=[${signature}], exceptions=[${exceptions}]".green)
                return doEmptyMethodVisitor(isInit, access, apiVersion, fullMethodName, descriptor, visitMethod)
            } else if (targetDescriptor == descriptor) {
                //表示匹配这个类下所有签名为descriptor的方法 (参数+返回值)
                asmLog(msg = "KnifeClassMethodVisitor >> need empty -> in [$internalClass]: method[${name}], descriptor=[${descriptor}], signature=[${signature}], exceptions=[${exceptions}]".green)
                return doEmptyMethodVisitor(isInit, access, apiVersion, fullMethodName, descriptor, visitMethod)
            }
        }
//        指定方法不处理，其他都处理，以后再做
//        val exclude = methodConfigs.filter { it.key.startsWith("-") }

        val modifyConfigs = methodConfigs[name] ?: return visitMethod
        val matchedModifyConfigs = modifyConfigs.filter { modifyConfig ->
            modifyConfig.targetMethod.descriptor == "*" || modifyConfig.targetMethod.descriptor == "?" || modifyConfig.targetMethod.descriptor == descriptor
        }

        if (matchedModifyConfigs.isEmpty()) {
            return visitMethod
        }

        //方法置空处理
        val emptyMethodConfig = matchedModifyConfigs.find {
            it.methodAction == null
        }
        if (emptyMethodConfig != null) {
            //方法置空的话 后面就不需要处理了，后面都是方法内部的处理
            asmLog(msg = "KnifeClassMethodVisitor >> need empty -> in [$internalClass]: method=[${name}], descriptor=[${descriptor}] ->$emptyMethodConfig".green)
            return doEmptyMethodVisitor(isInit, access, apiVersion, fullMethodName, descriptor, visitMethod)
        }

        //不是置空这个方法
        //接下来看这个方法内部的调用，是要移除某行调用还是修改某行调用
        val changeInvokeMethodActions = modifyConfigs.filter { modifyConfig ->
            //新类toNewClass不为空，说明这个方法内部要修改调用某行方法，执行者替换为某个类的同签名的静态方法
            modifyConfig.methodAction!!.toNewClass != null
        }.map { it.methodAction!! }

        if (changeInvokeMethodActions.isEmpty()) {
            val removeInvokeMethodActions = modifyConfigs.filter { modifyConfig ->
                //toNewClass为空说明要这个方法内部要移除某调用
                modifyConfig.methodAction!!.toNewClass == null
            }.map { it.methodAction!! }

            if (removeInvokeMethodActions.isEmpty()) {
                return visitMethod
            }

            //这个方法内部的处理：只要移除某行调用
            asmLog(msg = "KnifeClassMethodVisitor >> need remove -> in [$internalClass]: method=[${name}], descriptor=[${descriptor}] -> $removeInvokeMethodActions".green)
            return RemoveInvokeMethodVisitor(
                fullMethodName,
                removeInvokeMethodActions,
                apiVersion,
                visitMethod
            )
        }

        asmLog(msg = "KnifeClassMethodVisitor >> need change -> in [$internalClass]: method=[${name}], descriptor=[${descriptor}] -> $changeInvokeMethodActions".green)

        val changeInvokeOwnerMethodVisitor = ChangeInvokeOwnerMethodVisitor(
            fullMethodName,
            changeInvokeMethodActions,
            apiVersion,
            visitMethod
        )

        val removeInvokeMethodActions = modifyConfigs.filter { modifyConfig ->
            modifyConfig.methodAction!!.toNewClass == null
        }.map { it.methodAction!! }

        if (removeInvokeMethodActions.isEmpty()) {
            //这个方法内部的处理：只要修改调用方不用移除某行调用
            return changeInvokeOwnerMethodVisitor
        }

        //这个方法内部的处理：既要修改调用方也要移除某行调用
        asmLog(msg = "KnifeClassMethodVisitor >> need remove -> in [$internalClass]: method=[${name}], descriptor=[${descriptor}] -> $removeInvokeMethodActions".green)
        return RemoveInvokeMethodVisitor(
            fullMethodName,
            removeInvokeMethodActions,
            apiVersion,
            changeInvokeOwnerMethodVisitor
        )
    }

    override fun visitEnd() {
        super.visitEnd()
        asmLog(0, "KnifeClassMethodVisitor >> ${internalClass.lookup} ")
    }
}
