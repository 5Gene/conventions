package demo

import drop
import org.junit.Test


class Deploy(val deploymentId: String)
class MavenDeploy(val deployment: Deploy)

//找到notification请求 -> Fetch/XHR类型请求
val json = """xx""".trim()

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {

//        getStatus("3db2667a-01a6-498e-abbc-8200034116d5")
//        val file = File("""D:\code\dfj\auto-service\auto-service\build\zip\auto-service-0.0.6.zip""")
//        upload(file)
//        val lisst:ArrayList<MavenDeploy> = Gson().fromJson(json, object : TypeToken<ArrayList<MavenDeploy>>() {}.type)
//        lisst.forEach {
//            try {
//                drop2(it.deployment.deploymentId)
//            } catch (e: Exception) {
//            }
//        }
        drop("ad889c94-b80f-4770-be93-219d2bcb78a1")
    }
}