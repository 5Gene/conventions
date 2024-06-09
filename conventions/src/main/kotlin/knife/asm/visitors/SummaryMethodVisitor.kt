package knife.asm.visitors

import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.Attribute
import org.objectweb.asm.Handle
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor

/**
 *  ## MethodVisitor 回调方法有：以下非顺序
 *  - visitCode：开始访问方法代码，此处可以添加方法运行前拦截器
 *  - visitParameter：访问方法一个参数
 *  - visitAnnotationDefualt：访问注解接口方法的默认值
 *  - visitAnnotaion：访问方法的一个注解
 *  - visitTypeAnnotation：访问方法签名上的一个类型的注解
 *  - visitAnnotableParameterCount：访问注解参数数量，就是访问方法参数有注解参数个数
 *  - visitParameterAnnotation：访问参数的注解，返回一个 AnnotationVisitor 可以访问该注解值
 *  - visitAttribute：访问方法的属性
 *  - visitFrame：访问方法局部变量的当前状态以及操作栈成员信息
 *  - visitIntInsn：访问数值类型指令,当 int 取值-1~5采用 ICONST 指令，取值 -128~127 采用 BIPUSH 指令，取值 -32768~32767 采用 SIPUSH 指令，取值 -2147483648~2147483647 采用 ldc 指令。
 *  - visitVarInsn：访问本地变量类型指令
 *  - visitTypeInsn：访问类型指令，类型指令会把类的内部名称当成参数 Type
 *  - visitFieldInsn：域操作指令，用来加载或者存储对象的 Field
 *  - visitMethodInsn：访问方法操作指令
 *  - visitDynamicInsn：访问动态类型指令
 *  - visitJumpInsn：访问比较跳转指令
 *  - visitLabelInsn：访问 label，当会在调用该方法后访问该label标记一个指令
 *  - visitLdcInsn：访问 LDC 指令，也就是访问常量池索引
 *  - visitLineNumber：访问行号描述
 *  - visitMaxs：访问操作数栈最大值和本地变量表最大值
 *  - visitLocalVariable：访问本地变量描述
 */
class SummaryMethodVisitor(api: Int, methodVisitor: MethodVisitor?) : MethodVisitor(api, methodVisitor) {

    override fun visitParameter(name: String?, access: Int) {
        super.visitParameter(name, access)
        // visitParameter方法用来访问方法的参数
        // Java: void foo(@Nullable String s) {}
        // Kotlin: fun foo(@Nullable s: String?) {}
    }

    override fun visitAnnotationDefault(): AnnotationVisitor? {
        // visitAnnotationDefault方法用于访问注解的默认值
        // Java: @interface Foo { int value() default 1; }
        // Kotlin: annotation class Foo(val value: Int = 1)
        return super.visitAnnotationDefault()
    }

    override fun visitAnnotation(descriptor: String?, visible: Boolean): AnnotationVisitor? {
        // visitAnnotation方法用于访问方法的注解
        // Java: @Override
        // Kotlin: @Override
        return super.visitAnnotation(descriptor, visible)
    }

    override fun visitAnnotableParameterCount(parameterCount: Int, visible: Boolean) {
        super.visitAnnotableParameterCount(parameterCount, visible)
        // visitAnnotableParameterCount方法用于访问可注解参数的数量
        // Java: void foo(@Nullable String s) {}
        // Kotlin: fun foo(@Nullable s: String?) {}
    }

    override fun visitParameterAnnotation(parameter: Int, descriptor: String?, visible: Boolean): AnnotationVisitor? {
        // visitParameterAnnotation方法用于访问方法参数的注解
        // Java: void foo(@Nullable String s) {}
        // Kotlin: fun foo(@Nullable s: String?) {}
        return super.visitParameterAnnotation(parameter, descriptor, visible)
    }

    override fun visitAttribute(attribute: Attribute?) {
        super.visitAttribute(attribute)
        // visitAttribute方法用于访问方法的非标准属性
        // 该方法在Java和Kotlin中没有直接对应的语法
    }

    override fun visitCode() {
        super.visitCode()
        // visitCode方法在方法的代码开始时被调用
        // Java: public void foo() { // visitCode
        // Kotlin: fun foo() { // visitCode
    }

    override fun visitFrame(type: Int, numLocal: Int, local: Array<Any>?, numStack: Int, stack: Array<Any>?) {
        super.visitFrame(type, numLocal, local, numStack, stack)
        // visitFrame方法用于访问方法中的帧
        // 该方法在Java和Kotlin中没有直接对应的语法
    }

    override fun visitInsn(opcode: Int) {
        super.visitInsn(opcode)
        // visitInsn方法用于访问零操作数指令
        // Java: return;
        // Kotlin: return
    }

    override fun visitIntInsn(opcode: Int, operand: Int) {
        super.visitIntInsn(opcode, operand)
        // visitIntInsn方法用于访问带有单个int操作数的指令
        // Java: newarray int
        // Kotlin: IntArray(size)
    }

    override fun visitVarInsn(opcode: Int, `var`: Int) {
        super.visitVarInsn(opcode, `var`)
        // visitVarInsn方法用于访问局部变量指令
        // Java: int a = 1;
        // Kotlin: val a = 1
    }

    override fun visitTypeInsn(opcode: Int, type: String?) {
        super.visitTypeInsn(opcode, type)
        // visitTypeInsn方法用于访问类型指令
        // 所有可能的opcode包括:
        // Opcodes.NEW: type此时为类名 -> Java: new 类名, Kotlin: 类名()
        // Opcodes.ANEWARRAY: type此时为数组元素类型 -> Java: new 数组元素类型[], Kotlin: arrayOfNulls<数组元素类型>(size)
        // Opcodes.CHECKCAST: type此时为类名 -> Java: (类名)对象, Kotlin: 对象 as 类名
        // Opcodes.INSTANCEOF: type此时为类名 -> Java: 对象 instanceof 类名, Kotlin: 对象 is 类名
    }

    override fun visitFieldInsn(opcode: Int, owner: String?, name: String?, descriptor: String?) {
        super.visitFieldInsn(opcode, owner, name, descriptor)
        // visitFieldInsn方法用于访问字段指令
        // Java: 对象.字段 = 值;
        // Kotlin: 对象.字段 = 值
    }

    override fun visitMethodInsn(opcode: Int, owner: String?, name: String?, descriptor: String?, isInterface: Boolean) {
        super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
        // visitMethodInsn方法用于访问方法指令
        // Java: 对象.方法(参数);
        // Kotlin: 对象.方法(参数)
    }

    override fun visitInvokeDynamicInsn(name: String?, descriptor: String?, bootstrapMethodHandle: Handle?, vararg bootstrapMethodArguments: Any?) {
        super.visitInvokeDynamicInsn(name, descriptor, bootstrapMethodHandle, *bootstrapMethodArguments)
        // visitInvokeDynamicInsn方法用于访问invokedynamic指令
        // 该方法在Java和Kotlin中没有直接对应的语法
    }

    override fun visitJumpInsn(opcode: Int, label: Label?) {
        super.visitJumpInsn(opcode, label)
        // visitJumpInsn方法用于访问跳转指令
        // Java: if (条件) { ... } else { ... }
        // Kotlin: if (条件) { ... } else { ... }
    }

    override fun visitLabel(label: Label?) {
        super.visitLabel(label)
        // visitLabel方法用于访问标签
        // 该方法在Java和Kotlin中没有直接对应的语法
    }

    override fun visitLdcInsn(value: Any?) {
        super.visitLdcInsn(value)
        // visitLdcInsn方法用于访问ldc指令
        // Java: 常量值
        // Kotlin: 常量值
    }

    override fun visitIincInsn(`var`: Int, increment: Int) {
        super.visitIincInsn(`var`, increment)
        // visitIincInsn方法用于访问iinc指令
        // Java: 变量 += 增量;
        // Kotlin: 变量 += 增量
    }

    override fun visitTableSwitchInsn(min: Int, max: Int, dflt: Label?, vararg labels: Label?) {
        super.visitTableSwitchInsn(min, max, dflt, *labels)
        // visitTableSwitchInsn方法用于访问tableswitch指令
        // Java: switch (变量) { case 值: ... }
        // Kotlin: when (变量) { 值 -> ... }
    }

    override fun visitLookupSwitchInsn(dflt: Label?, keys: IntArray?, labels: Array<Label>?) {
        super.visitLookupSwitchInsn(dflt, keys, labels)
        // visitLookupSwitchInsn方法用于访问lookupswitch指令
        // Java: switch (变量) { case 值: ... }
        // Kotlin: when (变量) { 值 -> ... }
    }

    override fun visitMultiANewArrayInsn(descriptor: String?, numDimensions: Int) {
        super.visitMultiANewArrayInsn(descriptor, numDimensions)
        // visitMultiANewArrayInsn方法用于访问multianewarray指令
        // Java: new 数组类型[维数]
        // Kotlin: Array<数组类型>(维数) { ... }
    }

    override fun visitTryCatchBlock(start: Label?, end: Label?, handler: Label?, type: String?) {
        super.visitTryCatchBlock(start, end, handler, type)
        // visitTryCatchBlock方法用于访问try-catch块
        // Java: try { ... } catch (异常类型 e) { ... }
        // Kotlin: try { ... } catch (e: 异常类型) { ... }
    }

    override fun visitLocalVariable(name: String?, descriptor: String?, signature: String?, start: Label?, end: Label?, index: Int) {
        super.visitLocalVariable(name, descriptor, signature, start, end, index)
        // visitLocalVariable方法用于访问局部变量
        // Java: int 变量 = 值;
        // Kotlin: val 变量: 类型 = 值
    }

    override fun visitLineNumber(line: Int, start: Label?) {
        super.visitLineNumber(line, start)
        // visitLineNumber方法用于访问行号
        // 该方法在Java和Kotlin中没有直接对应的语法
    }

    override fun visitMaxs(maxStack: Int, maxLocals: Int) {
        super.visitMaxs(maxStack, maxLocals)
        // visitMaxs方法用于访问方法的最大堆栈大小和局部变量表大小
        // 该方法在Java和Kotlin中没有直接对应的语法
    }

    override fun visitEnd() {
        super.visitEnd()
        // visitEnd方法在方法访问完成时被调用
        // Java: 方法结束
        // Kotlin: 方法结束
    }
}