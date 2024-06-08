package knife.asm

import org.objectweb.asm.Handle
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import wing.lightRed
import wing.purple
import java.io.Serializable

data class MethodData(
    val fullClass: String,
    val internalClass: String,
    val methodName: String,
    val descriptor: String,
) : Serializable

data class MethodAction(
    val methodData: MethodData,
    val toNewClass: String? = null
) : Serializable {
    override fun toString(): String {
        if (toNewClass == null) {
            return "RemoveInvoke($methodData)"
        } else {
            return "ChangeInvoke($methodData to $toNewClass)"
        }
    }
}

internal fun asmLog(level: Int = 0, msg: String) {
    println("> ${Thread.currentThread().id} |-${"----".repeat(level)} $msg")
}

private fun String.isIgnore(): Boolean = this == "*"

private fun String.compareContains(other: String): Boolean = this == other || this.contains(other)

private fun List<MethodAction>.find(owner: String, name: String?, descriptor: String?): MethodAction? = find {
    val ignoreDescriptor = it.methodData.descriptor.isIgnore()
    val ignoreInternalClass = it.methodData.internalClass.isIgnore()
    if (ignoreDescriptor && ignoreInternalClass) {
        it.methodData.methodName == name
    } else if (ignoreDescriptor) {
        it.methodData.methodName == name && owner.compareContains(it.methodData.internalClass)
    } else if (ignoreInternalClass) {
        it.methodData.methodName == name && it.methodData.descriptor == descriptor
    } else {
        it.methodData.methodName == name && it.methodData.descriptor == descriptor && owner.compareContains(it.methodData.internalClass)
    }
}

private fun String.toMethodData(): MethodData {
    val (clz, method, desc) = this.split("#")
    return MethodData(clz, clz.replace(".", "/"), method, desc)
}

data class ModifyConfig(
    val targetMethod: MethodData,
    val methodAction: MethodAction? = null,
) : Serializable

internal fun String.toModifyConfig(): ModifyConfig {
    // "target.class#method#(I)V=>PrintStream#println#(I)V->dest/clazz"
    if (!contains("=>")) {
        return ModifyConfig(toMethodData())
    }
    val (targetMethodStr, innerMethodStr) = split("=>")
    val targetMethod = targetMethodStr.toMethodData()

    if (innerMethodStr.isEmpty()) {
        return ModifyConfig(targetMethod)
    }
    // PrintStream#println#(I)V->dest/clazz
    if (!innerMethodStr.contains("->")) {
        // PrintStream#println#(I)V
        return ModifyConfig(targetMethod, MethodAction(innerMethodStr.toMethodData()))
    }
    // PrintStream#println#(I)V->dest/clazz
    val (oldInnerMethodStr, toClz) = innerMethodStr.split("->")
    return ModifyConfig(targetMethod, MethodAction(oldInnerMethodStr.toMethodData(), toClz.replace(".", "/")))
}

//todo ## GeneratorAdapter 好用，封装了很多方法

internal class EmptyInitFunctionVisitor(
    apiVersion: Int,
    private val classMethod: String,
    private val methodDesc: String,
    private val methodVisitor: MethodVisitor,
) : MethodVisitor(apiVersion, methodVisitor) {

    override fun visitMethodInsn(opcode: Int, owner: String?, name: String?, descriptor: String?, isInterface: Boolean) {
        //INVOKESPECIAL java/lang/Object.<init> ()V
        val isConstructor = opcode == Opcodes.INVOKESPECIAL && name == "<init>"
        //执行构造方法
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
        if (isConstructor) {
            asmLog(1, "EmptyInitFunctionVisitor >> [$classMethod]$methodDesc > # $name$descriptor".lightRed)
            //构造方法中，正常应该是首先执行父类狗子方法的，但是kotlin在前面插入了参数的空判断
            //执行过构造方法只好再执行的方法调用就不需要了
            mv = null
        }
    }

    override fun visitFieldInsn(opcode: Int, owner: String?, name: String?, descriptor: String?) {
        //GETSTATIC java/lang/System.out : Ljava/io/PrintStream; 这条指令在 ASM 中对应的方法是 visitFieldInsn，具体来说是操作码为 Opcodes.GETSTATIC 的情况。
        //new Change -> mv.visitTypeInsn(Opcodes.NEW, "transform/Change");
        //PUTSTATIC transform/Origin.INSTANCE : Ltransform/Origin; 这条指令在 ASM 中对应的方法是 visitFieldInsn，具体来说是操作码为 Opcodes.PUTSTATIC 的情况。
        //给变量INSTANCE赋值
        if (opcode == Opcodes.PUTSTATIC && name == "INSTANCE") {
            asmLog(1, "EmptyInitFunctionVisitor >> [$classMethod]$methodDesc > # PUTSTATIC INSTANCE".lightRed)
            //Object类中 构造方法如下，会默认给INSTANCE复制
            //INVOKESPECIAL transform/Change.<init> ()V
            //1, ASTORE 0
            //2, ALOAD 0
            //3, PUTSTATIC transform/Change.INSTANCE : Ltransform/Change;
            //没太懂 1,2两步ASM没有,ASM调用链如下
            //visitMethodInsn(opcode=INVOKESPECIAL, name=<init>)
            //visitFieldInsn(opcode=PUTSTATIC, name=INSTANCE)
            methodVisitor.visitFieldInsn(opcode, owner, name, descriptor)

            mv = null
            //创建了INSTANCE之后 mv=null 就设置为后续不需要做任何操作了
        } else {
            super.visitFieldInsn(opcode, owner, name, descriptor)
        }
    }

    override fun visitInsn(opcode: Int) {
        if (opcode == Opcodes.RETURN) {
            //执行构造方法后mv可能为null所以必须手动设置
            methodVisitor.visitInsn(opcode)
        } else {
            super.visitInsn(opcode)
        }
    }

    override fun visitMaxs(maxStack: Int, maxLocals: Int) {
        //原来多少就给多少
        methodVisitor.visitMaxs(maxStack, maxLocals)
    }

    override fun visitEnd() {
        methodVisitor.visitEnd()
    }
}


/**
 * 一个 MethodVisitor，用于将目标方法修改为空方法，并返回适当的默认值。
 *
 * @param methodVisitor 父 MethodVisitor 用于委托
 *
 * @param methodDesc 方法的描述符，用于确定返回类型
 */
internal class EmptyMethodVisitor(
    apiVersion: Int,
    access: Int,
    private val classMethod: String,
    private val methodDesc: String,
    private val methodVisitor: MethodVisitor,
) : MethodVisitor(apiVersion) {
    private val isStaticMethod: Boolean = (access and Opcodes.ACC_STATIC) != 0

    /**
     * 访问方法的代码开始处。插入指令使方法为空方法。
     */
    override fun visitCode() {
        super.visitCode()
        // 根据方法的返回类型插入相应的返回指令
        //方法的局部变量表 (maxLocals)
        //局部变量表用于存储方法的参数和局部变量。在实例方法中，第一个局部变量索引是 this 引用。
        // 方法参数按顺序排列，每个参数占用一个索引，除非是 long 或 double 类型，它们占用两个索引。
        //计算 maxLocals:
        //  实例方法的 this 引用:
        //  - 占用索引 0。
        //方法参数:
        //  - 每个参数占用一个索引，除非是 long 或 double 类型，它们占用两个索引。
        //局部变量:
        //  - 方法体内声明的局部变量。

        // 如果添加日志的话 MAXSTACK 要➕2
        //methodVisitor.addLogCode("knife", classMethod)

        //ASTORE 2
        //作用： 将操作数栈顶的值弹出，并将其存储到局部变量表索引为 2 的位置。
        //区别：
        //存储的是一个引用类型的值（对象、数组）。
        //局部变量表索引 2 必须已经声明为一个可以存储该引用类型的变量。
        //ALOAD 1
        //作用： 将局部变量表索引为 1 的引用类型值加载到操作数栈顶。
        //区别：
        //加载的是一个引用类型的值。
        //局部变量表索引 1 必须已经存储了一个引用类型的值。
        //方法内加载变量需要增加 maxLocals

        //静态方法不需要加载this
        var maxLocals: Int = if (isStaticMethod) 0 else 1
        val arguments = Type.getArgumentTypes(methodDesc)
        arguments.forEach {
            if (it == Type.DOUBLE_TYPE || it == Type.LONG_TYPE) {
                //long 或 double 类型，它们占用两个索引。
                maxLocals += 2
            } else {
                maxLocals += 1
            }
        }

        //方法的操作数栈 (maxStack)
        //操作数栈用于执行字节码指令时的中间结果。计算 maxStack 的关键是跟踪每条字节码指令对栈的影响（入栈和出栈操作），
        // 并找出操作数栈在方法执行过程中达到的最大深度。
        //计算 maxStack:
        //  - 分析方法体的字节码，跟踪每条指令对栈的影响（入栈和出栈操作）。
        //  - 找出操作数栈在执行过程中达到的最大深度。
        //常量加载指令（如 iconst_0、ldc）会将常量压入栈中，增加栈深度。
        //返回指令（如 ireturn、dreturn）会弹出栈顶元素，并在返回后栈深度变为 0。
        //方法调用前需要确保栈有足够的空间来存储参数和返回值。
        val maxStack: Int
        when (Type.getReturnType(methodDesc).sort) {
            Type.VOID -> {
                // 如果返回类型是 void，插入 RETURN 指令
                methodVisitor.visitInsn(Opcodes.RETURN)
                maxStack = 0
            }

            Type.BOOLEAN, Type.CHAR, Type.BYTE, Type.SHORT, Type.INT -> {
                // 对于 boolean、char、byte、short 和 int 类型，插入 ICONST_0 和 IRETURN 指令
                methodVisitor.visitInsn(Opcodes.ICONST_0)
                methodVisitor.visitInsn(Opcodes.IRETURN)
                maxStack = 1
            }

            Type.FLOAT -> {
                // 对于 float 类型，插入 FCONST_0 和 FRETURN 指令
                methodVisitor.visitInsn(Opcodes.FCONST_0)
                methodVisitor.visitInsn(Opcodes.FRETURN)
                maxStack = 1
            }

            Type.LONG -> {
                // 对于 long 类型，插入 LCONST_0 和 LRETURN 指令
                methodVisitor.visitInsn(Opcodes.LCONST_0)
                methodVisitor.visitInsn(Opcodes.LRETURN)
                maxStack = 2
            }

            Type.DOUBLE -> {
                // 对于 double 类型，插入 DCONST_0 和 DRETURN 指令
                methodVisitor.visitInsn(Opcodes.DCONST_0)
                methodVisitor.visitInsn(Opcodes.DRETURN)
                maxStack = 2
            }

            Type.ARRAY, Type.OBJECT -> {
                //Ljava/lang/String;返回值为String
                if (methodDesc.endsWith("lang/String;")) {
                    // 加载空字符串常量到操作数栈
                    methodVisitor.visitLdcInsn("def from knife plugin")
                } else if (methodDesc.endsWith("java/util/List;")) {
                    //返回空list列表
                    methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "kotlin/collections/CollectionsKt", "emptyList", "()Ljava/util/List;", false)
                } else if (methodDesc.endsWith("java/util/Map;")) {
                    //返回空map集合
                    methodVisitor.visitMethodInsn(Opcodes.INVOKESTATIC, "kotlin/collections/MapsKt", "emptyMap", "()Ljava/util/Map;", false)
                } else {
                    // 对于数组和对象类型，插入 ACONST_NULL 和 ARETURN 指令
                    methodVisitor.visitInsn(Opcodes.ACONST_NULL)
                }
                methodVisitor.visitInsn(Opcodes.ARETURN)
                maxStack = 1
            }

            else -> throw IllegalArgumentException("不支持的返回类型:$methodDesc")
        }

        // 计算并设置最大堆栈大小和局部变量表的大小
        // 因为方法中可能有返回值的指令，所以需要合理设置堆栈和局部变量的大小
        methodVisitor.visitMaxs(maxStack, maxLocals)
        if (isStaticMethod) {
            asmLog(1, "EmptyMethodVisitor >> [$classMethod] > $methodDesc STATIC [maxStack:$maxStack, maxLocals:$maxLocals]".lightRed)
        } else {
            asmLog(1, "EmptyMethodVisitor >> [$classMethod] > $methodDesc [maxStack:$maxStack, maxLocals:$maxLocals]".lightRed)
        }

        // 标识方法访问的结束
        // 标识方法访问的结束。这个调用是必要的，以完成对方法的访问。如果没有这个调用，方法的定义将不完整，从而导致生成的字节码不正确。
        methodVisitor.visitEnd()
    }

}

/**
 * 移除方法中 调用的某行方法
 */
internal class RemoveInvokeMethodVisitor(
    private val classMethod: String,
    private val methodActions: List<MethodAction>,
    apiVersion: Int,
    nextVisitor: MethodVisitor
) : MethodVisitor(apiVersion, nextVisitor) {

    override fun visitMethodInsn(
        opcode: Int,
        owner: String,
        name: String?,
        descriptor: String?,
        isInterface: Boolean
    ) {
        val methodAction = methodActions.find(owner, name, descriptor)
        if (methodAction == null) {
            //没匹配到就不需要移除
            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
        } else {
            //移除的是方法里面调用的其他方法
            //不执行【super.visitMethodInsn()】那么就是移除方法的调用
            asmLog(1, "RemoveInvokeMethodVisitor >> owner=[${owner}], name=[${name}], descriptor=[${descriptor}], in [$classMethod]".lightRed)
        }
    }
}


/**
 * 替换方法内部中调用的指定方法的对象
 * fun toBeChange(){
 *      a.method() => b.method()
 * }
 */
internal class ChangeInvokeOwnerMethodVisitor(
    private val classMethod: String,
    private val methodActions: List<MethodAction>,
    apiVersion: Int,
    nextVisitor: MethodVisitor
) : MethodVisitor(apiVersion, nextVisitor) {

    /**
     * 访问方法中的方法调用指令。替换旧的调用对象为新的调用对象。
     *
     * @param opcode 方法调用的操作码（如 INVOKEVIRTUAL, INVOKESTATIC 等）
     *
     * @param owner 方法调用的对象的内部名称
     *
     * @param name 调用的方法的名称
     *
     * @param descriptor 调用的方法的描述符
     *
     * @param isInterface 方法调用的对象是否是接口
     */
    override fun visitMethodInsn(
        opcode: Int,
        owner: String,
        name: String?,
        descriptor: String,
        isInterface: Boolean
    ) {
        val methodAction = methodActions.find(owner, name, descriptor)
        if (methodAction == null) {
            //没匹配到就不需要处理
            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
        } else {
            asmLog(1, "ChangeInvokeOwnerMethodVisitor >> owner=[${owner}], name=[${name}], descriptor=[${descriptor}], to [${methodAction.toNewClass}]".purple)
            //方法调用 opcode
            //Opcodes.INVOKESPECIAL: 用于调用构造方法、私有方法和父类方法。这些方法的调用目标在编译时就确定了， 不依赖于对象的运行时类型。
            //Opcodes.INVOKESTATIC: 用于调用静态方法。静态方法不依赖于对象实例， 直接通过类名调用。
            //Opcodes.INVOKEINTERFACE: 用于调用接口方法。接口方法的调用目标在运行时确定， 根据对象的实际类型来查找对应的方法实现。
            //Opcodes.INVOKEDYNAMIC: 用于调用动态方法。动态方法的调用目标在运行时通过 CallSite 对象确定，提供了更灵活的方法调用机制。
            if (opcode == Opcodes.INVOKEVIRTUAL) {
                // 针对 对象.方法()的调用。把此对象当第一个参数传入
                // 如果替换的是对象的调用，静态方法必须第一个参数是对象
                //visitLdcInsn("str") // 加载字符串参数
                val newDescriptor = "(L${owner};${descriptor.substring(1)}"
                super.visitMethodInsn(
                    Opcodes.INVOKESTATIC,
                    methodAction.toNewClass,
                    name,
                    newDescriptor,
                    isInterface
                )
                return
            }
            // 替换为新的类的静态方法调用
            super.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                methodAction.toNewClass,
                name,
                descriptor,
                isInterface
            )
        }
    }

    //    https://jack-zheng.github.io/hexo/2020/09/07/ASM-quick-guide/
    override fun visitInvokeDynamicInsn(
        name: String?,
        descriptor: String?,
        bootstrapMethodHandle: Handle?,
        vararg bootstrapMethodArguments: Any?
    ) {
        //检测 lambda 表达式
        super.visitInvokeDynamicInsn(
            name,
            descriptor,
            bootstrapMethodHandle,
            *bootstrapMethodArguments
        )
    }
}