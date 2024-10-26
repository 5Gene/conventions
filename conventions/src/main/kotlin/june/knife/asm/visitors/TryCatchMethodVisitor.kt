package june.knife.asm.visitors

import june.knife.asm.asmLog
import june.knife.asm.insertDefReturn
import june.wing.purple
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

internal class TryCatchMethodVisitor(
    apiVersion: Int,
    methodVisitor: MethodVisitor,
    private val descriptor: String,
) : MethodVisitor(apiVersion, methodVisitor) {
    private val start = Label()
    private val end = Label()
    private val handler = Label()
    private var varIndexRecent = 0

    override fun visitCode() {
        super.visitCode()
        asmLog(1, "TryCatchMethodVisitor >> visitCode() => visitTryCatchBlock()".purple)
        //TRYCATCHBLOCK L0 L1 L2 java/lang/Exception
        // 开始 try 块
        mv.visitTryCatchBlock(start, end, handler, "java/lang/Throwable")
        mv.visitLabel(start)
    }

    override fun visitVarInsn(opcode: Int, varIndex: Int) {
        varIndexRecent = varIndex
        super.visitVarInsn(opcode, varIndex)
    }

    override fun visitMaxs(maxStack: Int, maxLocals: Int) {
        mv.visitLabel(end)
        mv.visitLabel(handler)
        mv.visitFrame(Opcodes.F_SAME1, 0, null, 1, arrayOf<Any>("java/lang/Throwable"))
        //e.printStackTrace()
        val varIndex = varIndexRecent + 1
        mv.visitVarInsn(Opcodes.ASTORE, varIndex)
        mv.visitVarInsn(Opcodes.ALOAD, varIndex)
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Throwable", "printStackTrace", "()V", false)
        //把默认值压栈
        // 根据返回类型返回默认值
        val consume = mv.insertDefReturn(descriptor)

        asmLog(1, "TryCatchMethodVisitor >> visitMaxs() => e.printStackTrace()".purple)

        //try catch e.printStackTrace() 至少要1个增加一个locaks, 因为e要存入本地变量表
        // 更新 maxStack 和 maxLocals
        super.visitMaxs(
            maxStack.coerceAtLeast(1).coerceAtLeast(consume.first),
            maxLocals + 1 + consume.second
        ) // +2 for stack size (aload and invokevirtual), +1 for exception local var
        //maxLocals：
        // - 原始方法的本地变量数。
        // - 加上一个新的本地变量用于存储异常对象。
        //  在这个例子中，假设 maxLocals 原来为 N，新的 maxLocals 将是 N + 1。

        //maxStack：
        // - 处理异常时，栈中需要额外的空间来存储异常对象（aload）和调用 printStackTrace 方法（invokevirtual）。
        // - aload 指令需要 1 个位置，invokevirtual 需要额外 1 个位置。
        // - 因此，额外增加 2 个位置。
        //  在这个例子中，假设 maxStack 原来为 M，新的 maxStack 将是 M + 2。
    }
}
