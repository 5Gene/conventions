package knife.asm

import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.*
import java.lang.reflect.Modifier

/**
 * @author yun.
 * @date 2022/4/30
 * @des [一句话描述]
 * @since [https://github.com/5hmlA]
 * <p><a href="https://github.com/5hmlA">github</a>
 */

const val JAPI = Opcodes.ASM9

/**
 * ## 如果添加日志的话
 * - MAXSTACK 要➕maxStack:2
 * - MAXLOCALS 要➕maxLocals:0
 *
 * @return (maxStack, maxLocals)
 */
fun logCode(mv: MethodVisitor, tag: String, msg: String): Pair<Int, Int> {
    return logCode(mv, "i", tag, msg)
}

/**
 * ## 如果添加日志的话
 * - MAXSTACK 要➕maxStack:2
 * - MAXLOCALS 要➕maxLocals:0
 *
 * @return (maxStack, maxLocals)
 */
fun logCode(mv: MethodVisitor, level: String, tag: String, msg: String): Pair<Int, Int> {
    //加载字符串
    mv.visitLdcInsn(tag)
    mv.visitLdcInsn(msg)
    mv.visitMethodInsn(Opcodes.INVOKESTATIC, "android/util/Log", level, "(Ljava/lang/String;Ljava/lang/String;)I", false)
    mv.visitInsn(Opcodes.POP) //log.i有返回值 需要扔掉
    return 2 to 0
}

/**
 * ## 如果添加日志的话
 * - MAXSTACK 要➕maxStack:2
 * - MAXLOCALS 要➕maxLocals:0
 *
 * @return (maxStack, maxLocals)
 */
fun MethodVisitor.addLogCode(tag: String, msg: String): Pair<Int, Int> {
    return addLogCode("i", tag, msg) //log.i有返回值 需要扔掉
}


/**
 * ## 如果添加日志的话
 * - MAXSTACK 要➕maxStack:2
 * - MAXLOCALS 要➕maxLocals:0
 *
 * @return (maxStack, maxLocals)
 */
fun MethodVisitor.addLogCode(level: String, tag: String, msg: String): Pair<Int, Int> {
    visitLdcInsn(tag) //LDC tag 将字符串压入栈，栈深度变为 1。
    visitLdcInsn(msg) ////LDC msg 将另一个字符串压入栈，栈深度变为 2。
    //INVOKESTATIC 调用方法，消耗栈上的两个字符串，并将返回值压入栈， 栈深度变为 1。
    visitMethodInsn(Opcodes.INVOKESTATIC, "android/util/Log", level, "(Ljava/lang/String;Ljava/lang/String;)I", false)
    //POP 弹出栈顶的整数，栈深度变为 0。
    visitInsn(Opcodes.POP) //log.i有返回值 需要扔掉
    //因此，这段字节码的最大栈深度为 2

    //这段字节码 不需要使用任何局部变量，因此 MAXLOCALS 可以设置为 0。
    //原因：
    //  - 没有局部变量声明： 字节码中没有出现任何 STORE 指令（如 ASTORE、ISTORE 等），表示没有将值存储到局部变量表。
    //  - 参数直接传递给方法： 两个 LDC 指令将字符串常量直接加载到操作数栈，然后作为参数传递给 INVOKESTATIC 调用的方法。
    //因此，这段字节码不需要使用局部变量表来存储任何数据， MAXLOCALS 可以设置为 0

    //在实际的 Java 类文件中，MAXLOCALS 通常不会设置为 0，因为即使方法不使用局部变量，编译器也可能会为方法分配一个或多个局部变量槽， 用于存储 this 引用或其他隐式参数。
    //但是，从这段字节码片段来看，它本身不需要使用任何局部变量， 因此 MAXLOCALS 可以设置为 0。
    return 2 to 0
}


fun Int.isReturn(): Boolean {
    return (this <= Opcodes.RETURN && this >= Opcodes.IRETURN)
}


fun Int.isMethodEnd(): Boolean {
    return isReturn() || this == Opcodes.ATHROW
}

fun Int.isMethodInvoke(): Boolean {
    return (this <= Opcodes.INVOKEDYNAMIC && this >= Opcodes.INVOKEVIRTUAL)
}

fun Int.isMethodIgnore(): Boolean {
    return Modifier.isAbstract(this) || Modifier.isNative(this) || Modifier.isInterface(this)
}