package com.osp.app

object RemoveAllMethod {
    fun method1() {
        println("method1")
    }

    fun method2(test: String) {
        println("test = [${test}]")
    }

    fun method3(vararg s: String): String {
        method1()
        method2("test")
        return "method3"
    }

    fun method4(aa: Int): Float {
        println(method3())
        method1()
        return 100F
    }

    fun methodDouble(aa: Int): Double {
        println(method3())
        method1()
        return 1.0
    }

    fun methodList(aa: Int, map: Map<String, String>): Map<String, String>? {
        println(method3())
        method1()
        return null
    }

    fun methoObj(aa: Int, ll: List<String>): Any {
        println(method3())
        method1()
        return 1
    }

    @JvmStatic
    fun methoSta(aa: Int, bb: String, d: Double): Any {
        println(method3())
        method1()
        return 1
    }

    init {
        println("9090")
        println("9090")
        println("9090")
        println("9090")
        println("9090")
    }
}