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


//\(.{1,7}=\$
//\(\$
val frame_type_str: Map<Int, String> = mapOf(
    //An expanded frame. See {@link ClassReader#EXPAND_FRAMES}
    "Opcodes.F_NEW" to -1,

    /** 具有完整帧数据的压缩帧。*/
    "Opcodes.F_FULL" to 0,

    /**
     * 压缩帧，其中局部变量与前一帧中的局部变量相同，除了
     * 定义了额外的 1-3 个局部变量，并且堆栈为空。
     */
    "Opcodes.F_APPEND" to 1,

    /**
     * 压缩帧，其中局部变量与前一帧中的局部变量相同，除了
     * 最后 1-3 个局部变量不存在并且堆栈为空。
     */
    "Opcodes.F_CHOP" to 2,

    /**
     * 与前一帧具有完全相同的局部变量且具有空堆栈的压缩帧。
     */
    "Opcodes.F_SAME" to 3,

    /**
     * 与前一帧具有完全相同的局部变量且具有单个值的压缩帧在堆栈上。
     */
    "Opcodes.F_SAME1" to 4,
).map {
    it.value to it.key
}.toMap()

fun Int.frameType() = frame_type_str[this]

val opcode_str: Map<Int, String> = mapOf(
    "Opcodes.NOP" to 0,
    "Opcodes.ACONST_NULL" to 1,
    "Opcodes.ICONST_M1" to 2,
    "Opcodes.ICONST_0" to 3,
    "Opcodes.ICONST_1" to 4,
    "Opcodes.ICONST_2" to 5,
    "Opcodes.ICONST_3" to 6,
    "Opcodes.ICONST_4" to 7,
    "Opcodes.ICONST_5" to 8,
    "Opcodes.LCONST_0" to 9,
    "Opcodes.LCONST_1" to 10,
    "Opcodes.FCONST_0" to 11,
    "Opcodes.FCONST_1" to 12,
    "Opcodes.FCONST_2" to 13,
    "Opcodes.DCONST_0" to 14,
    "Opcodes.DCONST_1" to 15,
    "Opcodes.BIPUSH" to 16,
    "Opcodes.SIPUSH" to 17,
    "Opcodes.LDC" to 18,
    "Opcodes.ILOAD" to 21,
    "Opcodes.LLOAD" to 22,
    "Opcodes.FLOAD" to 23,
    "Opcodes.DLOAD" to 24,
    "Opcodes.ALOAD" to 25,
    "Opcodes.IALOAD" to 46,
    "Opcodes.LALOAD" to 47,
    "Opcodes.FALOAD" to 48,
    "Opcodes.DALOAD" to 49,
    "Opcodes.AALOAD" to 50,
    "Opcodes.BALOAD" to 51,
    "Opcodes.CALOAD" to 52,
    "Opcodes.SALOAD" to 53,
    "Opcodes.ISTORE" to 54,
    "Opcodes.LSTORE" to 55,
    "Opcodes.FSTORE" to 56,
    "Opcodes.DSTORE" to 57,
    "Opcodes.ASTORE" to 58,
    "Opcodes.IASTORE" to 79,
    "Opcodes.LASTORE" to 80,
    "Opcodes.FASTORE" to 81,
    "Opcodes.DASTORE" to 82,
    "Opcodes.AASTORE" to 83,
    "Opcodes.BASTORE" to 84,
    "Opcodes.CASTORE" to 85,
    "Opcodes.SASTORE" to 86,
    "Opcodes.POP" to 87,
    "Opcodes.POP2" to 88,
    "Opcodes.DUP" to 89,
    "Opcodes.DUP_X1" to 90,
    "Opcodes.DUP_X2" to 91,
    "Opcodes.DUP2" to 92,
    "Opcodes.DUP2_X1" to 93,
    "Opcodes.DUP2_X2" to 94,
    "Opcodes.SWAP" to 95,
    "Opcodes.IADD" to 96,
    "Opcodes.LADD" to 97,
    "Opcodes.FADD" to 98,
    "Opcodes.DADD" to 99,
    "Opcodes.ISUB" to 100,
    "Opcodes.LSUB" to 101,
    "Opcodes.FSUB" to 102,
    "Opcodes.DSUB" to 103,
    "Opcodes.IMUL" to 104,
    "Opcodes.LMUL" to 105,
    "Opcodes.FMUL" to 106,
    "Opcodes.DMUL" to 107,
    "Opcodes.IDIV" to 108,
    "Opcodes.LDIV" to 109,
    "Opcodes.FDIV" to 110,
    "Opcodes.DDIV" to 111,
    "Opcodes.IREM" to 112,
    "Opcodes.LREM" to 113,
    "Opcodes.FREM" to 114,
    "Opcodes.DREM" to 115,
    "Opcodes.INEG" to 116,
    "Opcodes.LNEG" to 117,
    "Opcodes.FNEG" to 118,
    "Opcodes.DNEG" to 119,
    "Opcodes.ISHL" to 120,
    "Opcodes.LSHL" to 121,
    "Opcodes.ISHR" to 122,
    "Opcodes.LSHR" to 123,
    "Opcodes.IUSHR" to 124,
    "Opcodes.LUSHR" to 125,
    "Opcodes.IAND" to 126,
    "Opcodes.LAND" to 127,
    "Opcodes.IOR" to 128,
    "Opcodes.LOR" to 129,
    "Opcodes.IXOR" to 130,
    "Opcodes.LXOR" to 131,
    "Opcodes.IINC" to 132,
    "Opcodes.I2L" to 133,
    "Opcodes.I2F" to 134,
    "Opcodes.I2D" to 135,
    "Opcodes.L2I" to 136,
    "Opcodes.L2F" to 137,
    "Opcodes.L2D" to 138,
    "Opcodes.F2I" to 139,
    "Opcodes.F2L" to 140,
    "Opcodes.F2D" to 141,
    "Opcodes.D2I" to 142,
    "Opcodes.D2L" to 143,
    "Opcodes.D2F" to 144,
    "Opcodes.I2B" to 145,
    "Opcodes.I2C" to 146,
    "Opcodes.I2S" to 147,
    "Opcodes.LCMP" to 148,
    "Opcodes.FCMPL" to 149,
    "Opcodes.FCMPG" to 150,
    "Opcodes.DCMPL" to 151,
    "Opcodes.DCMPG" to 152,
    "Opcodes.IFEQ" to 153,
    "Opcodes.IFNE" to 154,
    "Opcodes.IFLT" to 155,
    "Opcodes.IFGE" to 156,
    "Opcodes.IFGT" to 157,
    "Opcodes.IFLE" to 158,
    "Opcodes.IF_ICMPEQ" to 159,
    "Opcodes.IF_ICMPNE" to 160,
    "Opcodes.IF_ICMPLT" to 161,
    "Opcodes.IF_ICMPGE" to 162,
    "Opcodes.IF_ICMPGT" to 163,
    "Opcodes.IF_ICMPLE" to 164,
    "Opcodes.IF_ACMPEQ" to 165,
    "Opcodes.IF_ACMPNE" to 166,
    "Opcodes.GOTO" to 167,
    "Opcodes.JSR" to 168,
    "Opcodes.RET" to 169,
    "Opcodes.TABLESWITCH" to 170,
    "Opcodes.LOOKUPSWITCH" to 171,
    "Opcodes.IRETURN" to 172,
    "Opcodes.LRETURN" to 173,
    "Opcodes.FRETURN" to 174,
    "Opcodes.DRETURN" to 175,
    "Opcodes.ARETURN" to 176,
    "Opcodes.RETURN" to 177,
    "Opcodes.GETSTATIC" to 178,
    "Opcodes.PUTSTATIC" to 179,
    "Opcodes.GETFIELD" to 180,
    "Opcodes.PUTFIELD" to 181,
    "Opcodes.INVOKEVIRTUAL" to 182,
    "Opcodes.INVOKESPECIAL" to 183,
    "Opcodes.INVOKESTATIC" to 184,
    "Opcodes.INVOKEINTERFACE" to 185,
    "Opcodes.INVOKEDYNAMIC" to 186,
    "Opcodes.NEW" to 187,
    "Opcodes.NEWARRAY" to 188,
    "Opcodes.ANEWARRAY" to 189,
    "Opcodes.ARRAYLENGTH" to 190,
    "Opcodes.ATHROW" to 191,
    "Opcodes.CHECKCAST" to 192,
    "Opcodes.INSTANCEOF" to 193,
    "Opcodes.MONITORENTER" to 194,
    "Opcodes.MONITOREXIT" to 195,
    "Opcodes.MULTIANEWARRAY" to 197,
    "Opcodes.IFNULL" to 198,
    "Opcodes.IFNONNULL" to 199,
).map {
    it.value to it.key
}.toMap()

fun Int.show() = opcode_str[this]

open class LoggingMethodVisitor(api: Int = Opcodes.ASM9, mv: MethodVisitor?) : MethodVisitor(api, mv) {

    override fun visitParameter(name: String?, access: Int) {
        println("mv.visitParameter(\"$name\", $access)")
        super.visitParameter(name, access)
    }

    override fun visitAnnotationDefault(): AnnotationVisitor? {
        println("mv.visitAnnotationDefault()")
        return super.visitAnnotationDefault()
    }

    override fun visitAnnotation(descriptor: String?, visible: Boolean): AnnotationVisitor? {
        println("mv.visitAnnotation(descriptor=\"$descriptor\", $visible)")
        return super.visitAnnotation(descriptor, visible)
    }

    override fun visitAnnotableParameterCount(parameterCount: Int, visible: Boolean) {
        println("mv.visitAnnotableParameterCount(parameterCount=$parameterCount, $visible)")
        super.visitAnnotableParameterCount(parameterCount, visible)
    }

    override fun visitParameterAnnotation(parameter: Int, descriptor: String?, visible: Boolean): AnnotationVisitor? {
        println("mv.visitParameterAnnotation(parameter=$parameter, \"$descriptor\", $visible)")
        return super.visitParameterAnnotation(parameter, descriptor, visible)
    }

    override fun visitAttribute(attribute: Attribute?) {
        println("mv.visitAttribute(attribute=$attribute)")
        super.visitAttribute(attribute)
    }

    override fun visitCode() {
        println("mv.visitCode()")
        super.visitCode()
    }

    override fun visitFrame(type: Int, numLocal: Int, local: Array<Any>?, numStack: Int, stack: Array<Any>?) {
        println("mv.visitFrame(${type.frameType()}, $numLocal, ${local?.contentToString()}, $numStack, ${stack?.contentToString()})")
        super.visitFrame(type, numLocal, local, numStack, stack)
    }

    override fun visitInsn(opcode: Int) {
        println("mv.visitInsn(${opcode.show()})")
        super.visitInsn(opcode)
    }

    override fun visitIntInsn(opcode: Int, operand: Int) {
        println("mv.visitIntInsn(${opcode.show()}, $operand)")
        super.visitIntInsn(opcode, operand)
    }

    override fun visitVarInsn(opcode: Int, varIndex: Int) {
        println("mv.visitVarInsn(${opcode.show()}, $varIndex)")
        super.visitVarInsn(opcode, varIndex)
    }

    override fun visitTypeInsn(opcode: Int, type: String?) {
        println("mv.visitTypeInsn(${opcode.show()}, \"$type\")")
        super.visitTypeInsn(opcode, type)
    }

    override fun visitFieldInsn(opcode: Int, owner: String?, name: String?, descriptor: String?) {
        println("mv.visitFieldInsn(${opcode.show()}, \"$owner\", \"$name\", \"$descriptor\")")
        super.visitFieldInsn(opcode, owner, name, descriptor)
    }

    override fun visitMethodInsn(opcode: Int, owner: String?, name: String?, descriptor: String?, isInterface: Boolean) {
        println("mv.visitMethodInsn(${opcode.show()}, \"$owner\", \"$name\", \"$descriptor\", $isInterface)")
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
    }

    override fun visitInvokeDynamicInsn(name: String?, descriptor: String?, bootstrapMethodHandle: Handle?, vararg bootstrapMethodArguments: Any?) {
        println("mv.visitInvokeDynamicInsn(\"$name\", \"$descriptor\", bootstrapMethodHandle=$bootstrapMethodHandle, bootstrapMethodArguments=${bootstrapMethodArguments.contentToString()})")
        super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, *bootstrapMethodArguments)
    }

    override fun visitJumpInsn(opcode: Int, label: Label?) {
        println("mv.visitJumpInsn(${opcode.show()}, $label)")
        super.visitJumpInsn(opcode, label)
    }

    override fun visitLabel(label: Label) {
        try_catch_labels[label.toString()]?.apply {
            println("mv.visitLabel(label=【$this】$label)")
        } ?: println("mv.visitLabel($label)")
        super.visitLabel(label)
    }

    override fun visitLdcInsn(value: Any?) {
        println("mv.visitLdcInsn($value)")
        super.visitLdcInsn(value)
    }

    override fun visitIincInsn(varIndex: Int, increment: Int) {
        println("mv.visitIincInsn($varIndex, $increment)")
        super.visitIincInsn(varIndex, increment)
    }

    override fun visitTableSwitchInsn(min: Int, max: Int, dflt: Label?, vararg labels: Label?) {
        println("mv.visitTableSwitchInsn($min, $max, $dflt, ${labels.contentToString()})")
        super.visitTableSwitchInsn(min, max, dflt, *labels)
    }

    override fun visitLookupSwitchInsn(dflt: Label?, keys: IntArray?, labels: Array<Label>?) {
        println("mv.visitLookupSwitchInsn($dflt, ${keys?.contentToString()}, ${labels?.contentToString()})")
        super.visitLookupSwitchInsn(dflt, keys, labels)
    }

    override fun visitMultiANewArrayInsn(descriptor: String?, numDimensions: Int) {
        println("mv.visitMultiANewArrayInsn(descriptor=\"$descriptor\", numDimensions=$numDimensions)")
        super.visitMultiANewArrayInsn(descriptor, numDimensions)
    }

    private val try_catch_labels = mutableMapOf<String, String>()

    override fun visitTryCatchBlock(start: Label?, end: Label?, handler: Label?, type: String?) {
        try_catch_labels[start.toString()] = "start"
        try_catch_labels[end.toString()] = "end"
        try_catch_labels[handler.toString()] = "handler"
        println("mv.visitTryCatchBlock(start=【start】$start, end=【end】$end, handler=【handler】$handler, \"$type\")")
        super.visitTryCatchBlock(start, end, handler, type)
    }

    override fun visitLocalVariable(name: String?, descriptor: String?, signature: String?, start: Label?, end: Label?, index: Int) {
        println("mv.visitLocalVariable(\"$name\", \"$descriptor\", \"$signature\", $start, $end, $index)")
        super.visitLocalVariable(name, descriptor, signature, start, end, index)
    }

    override fun visitLineNumber(line: Int, start: Label?) {
        println("mv.visitLineNumber($line, $start)")
        super.visitLineNumber(line, start)
    }

    override fun visitMaxs(maxStack: Int, maxLocals: Int) {
        println("mv.visitMaxs(maxStack=$maxStack, $maxLocals)")
        super.visitMaxs(maxStack, maxLocals)
    }

    override fun visitEnd() {
        println("mv.visitEnd()")
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
