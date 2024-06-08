package knife.asm

import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.Attribute
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Handle
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes.ASM9

open class LoggingMethodVisitor(api: Int, mv: MethodVisitor?) : MethodVisitor(api, mv) {

    override fun visitParameter(name: String?, access: Int) {
        println("visitParameter(name=$name, access=$access)")
        super.visitParameter(name, access)
    }

    override fun visitAnnotationDefault(): AnnotationVisitor? {
        println("visitAnnotationDefault()")
        return super.visitAnnotationDefault()
    }

    override fun visitAnnotation(descriptor: String?, visible: Boolean): AnnotationVisitor? {
        println("visitAnnotation(descriptor=$descriptor, visible=$visible)")
        return super.visitAnnotation(descriptor, visible)
    }

    override fun visitAnnotableParameterCount(parameterCount: Int, visible: Boolean) {
        println("visitAnnotableParameterCount(parameterCount=$parameterCount, visible=$visible)")
        super.visitAnnotableParameterCount(parameterCount, visible)
    }

    override fun visitParameterAnnotation(parameter: Int, descriptor: String?, visible: Boolean): AnnotationVisitor? {
        println("visitParameterAnnotation(parameter=$parameter, descriptor=$descriptor, visible=$visible)")
        return super.visitParameterAnnotation(parameter, descriptor, visible)
    }

    override fun visitAttribute(attribute: Attribute?) {
        println("visitAttribute(attribute=$attribute)")
        super.visitAttribute(attribute)
    }

    override fun visitCode() {
        println("visitCode()")
        super.visitCode()
    }

    override fun visitFrame(type: Int, numLocal: Int, local: Array<Any>?, numStack: Int, stack: Array<Any>?) {
        println("visitFrame(type=$type, numLocal=$numLocal, local=${local?.contentToString()}, numStack=$numStack, stack=${stack?.contentToString()})")
        super.visitFrame(type, numLocal, local, numStack, stack)
    }

    override fun visitInsn(opcode: Int) {
        println("visitInsn(opcode=$opcode)")
        super.visitInsn(opcode)
    }

    override fun visitIntInsn(opcode: Int, operand: Int) {
        println("visitIntInsn(opcode=$opcode, operand=$operand)")
        super.visitIntInsn(opcode, operand)
    }

    override fun visitVarInsn(opcode: Int, varIndex: Int) {
        println("visitVarInsn(opcode=$opcode, var=$varIndex)")
        super.visitVarInsn(opcode, varIndex)
    }

    override fun visitTypeInsn(opcode: Int, type: String?) {
        println("visitTypeInsn(opcode=$opcode, type=$type)")
        super.visitTypeInsn(opcode, type)
    }

    override fun visitFieldInsn(opcode: Int, owner: String?, name: String?, descriptor: String?) {
        println("visitFieldInsn(opcode=$opcode, owner=$owner, name=$name, descriptor=$descriptor)")
        super.visitFieldInsn(opcode, owner, name, descriptor)
    }

    override fun visitMethodInsn(opcode: Int, owner: String?, name: String?, descriptor: String?, isInterface: Boolean) {
        println("visitMethodInsn(opcode=$opcode, owner=$owner, name=$name, descriptor=$descriptor, isInterface=$isInterface)")
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
    }

    override fun visitInvokeDynamicInsn(name: String?, descriptor: String?, bootstrapMethodHandle: Handle?, vararg bootstrapMethodArguments: Any?) {
        println("visitInvokeDynamicInsn(name=$name, descriptor=$descriptor, bootstrapMethodHandle=$bootstrapMethodHandle, bootstrapMethodArguments=${bootstrapMethodArguments.contentToString()})")
        super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, *bootstrapMethodArguments)
    }

    override fun visitJumpInsn(opcode: Int, label: Label?) {
        println("visitJumpInsn(opcode=$opcode, label=$label)")
        super.visitJumpInsn(opcode, label)
    }

    override fun visitLabel(label: Label?) {
        println("visitLabel(label=$label)")
        super.visitLabel(label)
    }

    override fun visitLdcInsn(value: Any?) {
        println("visitLdcInsn(value=$value)")
        super.visitLdcInsn(value)
    }

    override fun visitIincInsn(varIndex: Int, increment: Int) {
        println("visitIincInsn(var=$varIndex, increment=$increment)")
        super.visitIincInsn(varIndex, increment)
    }

    override fun visitTableSwitchInsn(min: Int, max: Int, dflt: Label?, vararg labels: Label?) {
        println("visitTableSwitchInsn(min=$min, max=$max, dflt=$dflt, labels=${labels.contentToString()})")
        super.visitTableSwitchInsn(min, max, dflt, *labels)
    }

    override fun visitLookupSwitchInsn(dflt: Label?, keys: IntArray?, labels: Array<Label>?) {
        println("visitLookupSwitchInsn(dflt=$dflt, keys=${keys?.contentToString()}, labels=${labels?.contentToString()})")
        super.visitLookupSwitchInsn(dflt, keys, labels)
    }

    override fun visitMultiANewArrayInsn(descriptor: String?, numDimensions: Int) {
        println("visitMultiANewArrayInsn(descriptor=$descriptor, numDimensions=$numDimensions)")
        super.visitMultiANewArrayInsn(descriptor, numDimensions)
    }

    override fun visitTryCatchBlock(start: Label?, end: Label?, handler: Label?, type: String?) {
        println("visitTryCatchBlock(start=$start, end=$end, handler=$handler, type=$type)")
        super.visitTryCatchBlock(start, end, handler, type)
    }

    override fun visitLocalVariable(name: String?, descriptor: String?, signature: String?, start: Label?, end: Label?, index: Int) {
        println("visitLocalVariable(name=$name, descriptor=$descriptor, signature=$signature, start=$start, end=$end, index=$index)")
        super.visitLocalVariable(name, descriptor, signature, start, end, index)
    }

    override fun visitLineNumber(line: Int, start: Label?) {
        println("visitLineNumber(line=$line, start=$start)")
        super.visitLineNumber(line, start)
    }

    override fun visitMaxs(maxStack: Int, maxLocals: Int) {
        println("visitMaxs(maxStack=$maxStack, maxLocals=$maxLocals)")
        super.visitMaxs(maxStack, maxLocals)
    }

    override fun visitEnd() {
        println("visitEnd()")
        super.visitEnd()
    }
}

// 用于测试的主函数
fun main() {
    val classReader = ClassReader("java.lang.Runnable")
    val classWriter = ClassWriter(ClassWriter.COMPUTE_FRAMES)
    val classVisitor = object : ClassVisitor(ASM9, classWriter) {
        override fun visitMethod(access: Int, name: String?, descriptor: String?, signature: String?, exceptions: Array<out String>?): MethodVisitor {
            val mv = super.visitMethod(access, name, descriptor, signature, exceptions)
            return LoggingMethodVisitor(ASM9, mv)
        }
    }
    classReader.accept(classVisitor, ClassReader.EXPAND_FRAMES)
}
