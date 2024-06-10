package wing

import java.lang.reflect.Method


fun Any.getDeclaredMethod2(name: String, vararg parameterTypes: Class<*>): Method {
    val method = this.javaClass.getDeclaredMethod(name, *parameterTypes)
    method.isAccessible = true
    return method
}

@Suppress("UNCHECKED_CAST")
fun <T> Any.getDeclaredField2(name: String): T {
    val field = this::class.java.getDeclaredField(name)
    field.isAccessible = true
    return field.get(this) as T
}