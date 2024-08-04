package wing

import java.lang.reflect.Field
import java.lang.reflect.Method

fun Any.proxy(name: String, proxy: Any) {
    val filed = this.javaClass.getClassDeclaredField(name)
    filed.set(this, proxy)
}

fun <T> Class<T>.getClassDeclaredField(name: String): Field {
    return try {
        getDeclaredField(name).apply { isAccessible = true }
    } catch (e: Exception) {
        e.printStackTrace()
        superclass.getClassDeclaredField(name)
    }
}

fun <T> Class<T>.getClassDeclaredMethod(name: String, vararg parameterTypes: Class<*>): Method {
    return try {
        getDeclaredMethod(name, *parameterTypes).apply { isAccessible = true }
    } catch (e: Exception) {
        e.printStackTrace()
        superclass.getClassDeclaredMethod(name)
    }
}

@Suppress("UNCHECKED_CAST")
fun <T> Any.declareFiledGet(name: String): T {
    val field: Field = this::class.java.getClassDeclaredField(name)
    return field.get(this) as T
}

fun <T> Any.declaredMethodInvoke(name: String, params: Map<Class<*>, Any>): T {
    val method = this.javaClass.getClassDeclaredMethod(name, *params.keys.toTypedArray())
    return method.invoke(this, *params.values.toTypedArray()) as T
}
