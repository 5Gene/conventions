package june.knife.asm.visitors

import june.knife.asm.asmLog
import june.knife.asm.isMethodExit
import june.wing.purple
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

class TraceMethodVisitor(
    apiVersion: Int,
    methodVisitor: MethodVisitor,
    private val classMethodName: String,
) : MethodVisitor(apiVersion, methodVisitor) {
    override fun visitCode() {
        asmLog(1, "TraceMethodVisitor >> $classMethodName >> visitCode() => Trace.beginSection(sectionName)".purple)
        val tag = classMethodName.let {
            it.substring(0.coerceAtLeast(it.length - 126))
        }
        //LDC 指令会将一个常量值压入栈顶。常量值在栈中占用的槽位数取决于其类型：
        //基本类型（int, long, float, double）和引用类型占用一个槽位。
        //long 和 double 类型占用两个槽位。
        //因此，maxStack 的增加量取决于 tag 的类型
        mv.visitLdcInsn(tag)
        //表示调用静态方法 android/os/Trace.beginSection(String)。
        //对操作数栈的影响：
        //弹出参数： beginSection(String) 方法需要一个 String 类型的参数，因此会从操作数栈弹出表示 String 对象的引用。
        //无返回值： beginSection 方法没有返回值（返回值类型为 void，用 V 表示）。
        //因此，maxStack 的净变化为 -1（弹出一个引用）
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "android/os/Trace", "beginSection", "(Ljava/lang/String;)V", false)
        //所以上述不影响 maxStack
        super.visitCode()
    }

    override fun visitInsn(opcode: Int) {
        if (opcode.isMethodExit()) {
            asmLog(1, "TraceMethodVisitor >> $classMethodName >> visitInsn(${opcode.show()}) => Trace.endSection()".purple)
            mv.visitMethodInsn(Opcodes.INVOKESTATIC, "android/os/Trace", "endSection", "()V", false)
        }
        super.visitInsn(opcode)
    }

    override fun visitMaxs(maxStack: Int, maxLocals: Int) {
        //Trace至少需要一个stack保存变量sectionName
        super.visitMaxs(maxStack.coerceAtLeast(1), maxLocals)
    }
}