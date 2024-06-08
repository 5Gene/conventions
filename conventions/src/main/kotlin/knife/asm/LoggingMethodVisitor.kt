package knife.asm

import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.Attribute
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Handle
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Opcodes.ASM9

val opcode_str: Map<Int, String> = mapOf(
    "NOP" to 0,
    "ACONST_NULL" to 1,
    "ICONST_M1" to 2,
    "ICONST_0" to 3,
    "ICONST_1" to 4,
    "ICONST_2" to 5,
    "ICONST_3" to 6,
    "ICONST_4" to 7,
    "ICONST_5" to 8,
    "LCONST_0" to 9,
    "LCONST_1" to 10,
    "FCONST_0" to 11,
    "FCONST_1" to 12,
    "FCONST_2" to 13,
    "DCONST_0" to 14,
    "DCONST_1" to 15,
    "BIPUSH" to 16,
    "SIPUSH" to 17,
    "LDC" to 18,
    "ILOAD" to 21,
    "LLOAD" to 22,
    "FLOAD" to 23,
    "DLOAD" to 24,
    "ALOAD" to 25,
    "IALOAD" to 46,
    "LALOAD" to 47,
    "FALOAD" to 48,
    "DALOAD" to 49,
    "AALOAD" to 50,
    "BALOAD" to 51,
    "CALOAD" to 52,
    "SALOAD" to 53,
    "ISTORE" to 54,
    "LSTORE" to 55,
    "FSTORE" to 56,
    "DSTORE" to 57,
    "ASTORE" to 58,
    "IASTORE" to 79,
    "LASTORE" to 80,
    "FASTORE" to 81,
    "DASTORE" to 82,
    "AASTORE" to 83,
    "BASTORE" to 84,
    "CASTORE" to 85,
    "SASTORE" to 86,
    "POP" to 87,
    "POP2" to 88,
    "DUP" to 89,
    "DUP_X1" to 90,
    "DUP_X2" to 91,
    "DUP2" to 92,
    "DUP2_X1" to 93,
    "DUP2_X2" to 94,
    "SWAP" to 95,
    "IADD" to 96,
    "LADD" to 97,
    "FADD" to 98,
    "DADD" to 99,
    "ISUB" to 100,
    "LSUB" to 101,
    "FSUB" to 102,
    "DSUB" to 103,
    "IMUL" to 104,
    "LMUL" to 105,
    "FMUL" to 106,
    "DMUL" to 107,
    "IDIV" to 108,
    "LDIV" to 109,
    "FDIV" to 110,
    "DDIV" to 111,
    "IREM" to 112,
    "LREM" to 113,
    "FREM" to 114,
    "DREM" to 115,
    "INEG" to 116,
    "LNEG" to 117,
    "FNEG" to 118,
    "DNEG" to 119,
    "ISHL" to 120,
    "LSHL" to 121,
    "ISHR" to 122,
    "LSHR" to 123,
    "IUSHR" to 124,
    "LUSHR" to 125,
    "IAND" to 126,
    "LAND" to 127,
    "IOR" to 128,
    "LOR" to 129,
    "IXOR" to 130,
    "LXOR" to 131,
    "IINC" to 132,
    "I2L" to 133,
    "I2F" to 134,
    "I2D" to 135,
    "L2I" to 136,
    "L2F" to 137,
    "L2D" to 138,
    "F2I" to 139,
    "F2L" to 140,
    "F2D" to 141,
    "D2I" to 142,
    "D2L" to 143,
    "D2F" to 144,
    "I2B" to 145,
    "I2C" to 146,
    "I2S" to 147,
    "LCMP" to 148,
    "FCMPL" to 149,
    "FCMPG" to 150,
    "DCMPL" to 151,
    "DCMPG" to 152,
    "IFEQ" to 153,
    "IFNE" to 154,
    "IFLT" to 155,
    "IFGE" to 156,
    "IFGT" to 157,
    "IFLE" to 158,
    "IF_ICMPEQ" to 159,
    "IF_ICMPNE" to 160,
    "IF_ICMPLT" to 161,
    "IF_ICMPGE" to 162,
    "IF_ICMPGT" to 163,
    "IF_ICMPLE" to 164,
    "IF_ACMPEQ" to 165,
    "IF_ACMPNE" to 166,
    "GOTO" to 167,
    "JSR" to 168,
    "RET" to 169,
    "TABLESWITCH" to 170,
    "LOOKUPSWITCH" to 171,
    "IRETURN" to 172,
    "LRETURN" to 173,
    "FRETURN" to 174,
    "DRETURN" to 175,
    "ARETURN" to 176,
    "RETURN" to 177,
    "GETSTATIC" to 178,
    "PUTSTATIC" to 179,
    "GETFIELD" to 180,
    "PUTFIELD" to 181,
    "INVOKEVIRTUAL" to 182,
    "INVOKESPECIAL" to 183,
    "INVOKESTATIC" to 184,
    "INVOKEINTERFACE" to 185,
    "INVOKEDYNAMIC" to 186,
    "NEW" to 187,
    "NEWARRAY" to 188,
    "ANEWARRAY" to 189,
    "ARRAYLENGTH" to 190,
    "ATHROW" to 191,
    "CHECKCAST" to 192,
    "INSTANCEOF" to 193,
    "MONITORENTER" to 194,
    "MONITOREXIT" to 195,
    "MULTIANEWARRAY" to 197,
    "IFNULL" to 198,
    "IFNONNULL" to 199,
).map {
    it.value to it.key
}.toMap()

fun Int.show() = opcode_str[this]

open class LoggingMethodVisitor(api: Int = Opcodes.ASM9, mv: MethodVisitor?) : MethodVisitor(api, mv) {

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
        println("visitInsn(opcode=${opcode.show()})")
        super.visitInsn(opcode)
    }

    override fun visitIntInsn(opcode: Int, operand: Int) {
        println("visitIntInsn(opcode=${opcode.show()}, operand=$operand)")
        super.visitIntInsn(opcode, operand)
    }

    override fun visitVarInsn(opcode: Int, varIndex: Int) {
        println("visitVarInsn(opcode=${opcode.show()}, var=$varIndex)")
        super.visitVarInsn(opcode, varIndex)
    }

    override fun visitTypeInsn(opcode: Int, type: String?) {
        println("visitTypeInsn(opcode=${opcode.show()}, type=$type)")
        super.visitTypeInsn(opcode, type)
    }

    override fun visitFieldInsn(opcode: Int, owner: String?, name: String?, descriptor: String?) {
        println("visitFieldInsn(opcode=${opcode.show()}, owner=$owner, name=$name, descriptor=$descriptor)")
        super.visitFieldInsn(opcode, owner, name, descriptor)
    }

    override fun visitMethodInsn(opcode: Int, owner: String?, name: String?, descriptor: String?, isInterface: Boolean) {
        println("visitMethodInsn(opcode=${opcode.show()}, owner=$owner, name=$name, descriptor=$descriptor, isInterface=$isInterface)")
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
    }

    override fun visitInvokeDynamicInsn(name: String?, descriptor: String?, bootstrapMethodHandle: Handle?, vararg bootstrapMethodArguments: Any?) {
        println("visitInvokeDynamicInsn(name=$name, descriptor=$descriptor, bootstrapMethodHandle=$bootstrapMethodHandle, bootstrapMethodArguments=${bootstrapMethodArguments.contentToString()})")
        super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, *bootstrapMethodArguments)
    }

    override fun visitJumpInsn(opcode: Int, label: Label?) {
        println("visitJumpInsn(opcode=${opcode.show()}, label=$label)")
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
