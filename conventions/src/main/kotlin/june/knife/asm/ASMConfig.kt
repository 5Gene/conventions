package june.knife.asm

import java.io.Serializable


data class ModifyConfig(
    val targetMethod: MethodData,
    val methodAction: Action,
) : Serializable

data class MethodData(
    /**
     * com.pack.Class
     */
    val fullClass: String,
    /**
     * com/pack/Class
     */
    val internalClass: String,
    val methodName: String,
    val descriptor: String,
) : Serializable

sealed class Action(val name: String) : Serializable {
    /**
     * ## 删除调用
     * ### =>`class.method.descriptor`
     * -  =>`*`.println.*
     * -  =>java.io.PrintStream#println#*
     *
     * 要考虑有返回值的情况，可能后需要用到, 建议使用**【替换】**
     * ```
     * fun want_change(){
     *      val a = A()
     *      a.method(param)//删除调用
     *      B.method(param) => toNewClass.method(param)
     * }
     * ```
     *
     * @param methodData 要移除方法体中具体哪个方法的调用
     */
    data class RemoveInvoke(val methodData: MethodData) : Action("RemoveInvoke") {
        override fun toString(): String {
            return "RemoveInvoke($methodData)"
        }
    }

    /**
     * ## 乾坤大挪移
     * ### =>`[class|*].[method|*].[descriptor|*]->toNewClass`
     * -  =>`*`.println.*->com.change.NewClass
     * -  =>java.io.PrintStream#println#*->com.change.NewClass
     * ```
     * fun want_change(){
     *      val a = A()
     *      a.method(param) => toNewClass.method(a:A, param)
     *      B.method(param) => toNewClass.method(param)
     * }
     * ```
     *
     * @param methodData 方法体中具体哪个方法的调用, 重定向到 toNewClass
     */
    data class ChangeInvoke(val methodData: MethodData, val toNewClass: String) : Action("ChangeInvoke") {
        override fun toString(): String {
            return "ChangeInvoke($methodData to $toNewClass)"
        }
    }

    object EmptyBody : Action("EmptyBody") {
        private fun readResolve(): Any = EmptyBody

        override fun toString(): String {
            return "EmptyMethodBody"
        }
    }

    /**
     * ## TryCatchBody
     * ### =>`[TryCatchBody|trycatchbody|trycatch]
     * -  =>trycatchbody
     * -  =>TryCatchBody
     * ```
     * fun want_change(){
     *    try {
     *      val a = A()
     *      a.method(param)
     *      B.method(param)
     *    } catch (e: Exception) {
     *        e.printStackTrace()
     *    }
     * }
     * ```
     *
     */
    object TryCatchBody : Action("TryCatchBody") {
        private fun readResolve(): Any = TryCatchBody

        override fun toString(): String {
            return "TryCatchBody"
        }
    }


    /**
     * ## TraceBody
     * ### =>`[TraceBody|Trace|trace]
     * -  =>TraceBody
     * -  =>Trace
     * ```
     * fun want_change(){
     *    try {
     *      val a = A()
     *      a.method(param)
     *      B.method(param)
     *    } catch (e: Exception) {
     *        e.printStackTrace()
     *    }
     * }
     * ```
     *
     */
    object TraceBody : Action("TraceBody") {
        private fun readResolve(): Any = TraceBody

        override fun toString(): String {
            return "TraceBody"
        }
    }
}

internal fun asmLog(level: Int = 0, msg: String) {
    println("> ${Thread.currentThread().id} |-${"----".repeat(level)} $msg")
}

private fun String.isIgnore(): Boolean = this == "*"

private fun String.compareContains(other: String): Boolean = this == other || this.contains(other)

fun <A : Action> List<A>.findAction(owner: String, name: String?, descriptor: String?): A? = find {
    val methodData = if (it is Action.ChangeInvoke) it.methodData else if (it is Action.RemoveInvoke) it.methodData else null
    if (methodData == null) {
        false
    } else {
        val ignoreDescriptor = methodData.descriptor.isIgnore()
        val ignoreInternalClass = methodData.internalClass.isIgnore()
        if (ignoreDescriptor && ignoreInternalClass) {
            methodData.methodName == name
        } else if (ignoreDescriptor) {
            methodData.methodName == name && owner.compareContains(methodData.internalClass)
        } else if (ignoreInternalClass) {
            methodData.methodName == name && methodData.descriptor == descriptor
        } else {
            methodData.methodName == name && methodData.descriptor == descriptor && owner.compareContains(methodData.internalClass)
        }
    }
}

private fun String.toMethodData(): MethodData {
    val (clz, method, desc) = this.split("#")
    return MethodData(clz.replace("/", "."), clz.replace(".", "/"), method, desc)
}

private fun String.toMethodAction(): Action {
    if (this.lowercase() == "trycatch") {
        return Action.TryCatchBody
    }
    if (this.lowercase() == "emptybody" || this.lowercase() == "empty") {
        return Action.EmptyBody
    }
    if (this.lowercase() == "tracebody" || this.lowercase() == "trace") {
        return Action.TraceBody
    }
    if (this.contains("->")) {
        val (methodStr, toClz) = this.split("->")
        return Action.ChangeInvoke(methodStr.toMethodData(), toClz.replace(".", "/"))
    }
    if (this.contains("#")) {
        return Action.RemoveInvoke(this.toMethodData())
    }
    return Action.EmptyBody
}

internal fun String.toModifyConfig(): ModifyConfig {
    // "target.class#method#(I)V=>PrintStream#println#(I)V->dest/clazz"
    if (!contains("=>")) {
        return ModifyConfig(toMethodData(), Action.EmptyBody)
    }
    val (targetMethodStr, methodActionStr) = split("=>")
    val targetMethod = targetMethodStr.toMethodData()

    if (methodActionStr.isEmpty()) {
        return ModifyConfig(targetMethod, Action.EmptyBody)
    }
    return ModifyConfig(targetMethod, methodActionStr.toMethodAction())
}