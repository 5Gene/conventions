package june.wing

import org.gradle.api.Project
import org.gradle.api.execution.TaskExecutionGraph
import org.gradle.jvm.tasks.Jar
import java.nio.file.Path
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries

/**
 * 通用工具扩展函数
 */

/**
 * 集合转字符串
 */
fun Collection<*>.toStr(): String {
    return toTypedArray().contentToString()
}

/**
 * 检查路径是否为 Gradle 项目
 */
fun Path.isGradleProject(): Boolean = if (!isDirectory()) false else listDirectoryEntries().any {
    it.toString().endsWith("build.gradle.kts")
}

/**
 * 字符串装饰扩展属性
 */
val String.lookDown: String
    get() = "👇👇👇👇👇👇👇👇👇👇👇👇👇👇👇 $this 👇👇👇👇👇👇👇👇👇👇👇👇👇👇👇"

val String.lookup: String
    get() = "👆👆👆👆👆👆👆👆👆👆👆👆👆👆👆 $this 👆👆👆👆👆👆👆👆👆👆👆👆👆👆👆"

/**
 * 把已有的Jar类型任务修改为不打包任何内容
 */
fun Project.jarTaskEmptyJar(vararg jarTaskNames: String, whenReady: (TaskExecutionGraph.() -> Unit)? = null) {
    val projectName = name
    gradle.taskGraph.whenReady {
        jarTaskNames.forEach {
            val task = (tasks.findByName(it) as? Jar)?.exclude("**/*")
            if (task == null) {
                logWarn("【jarTaskEmptyJar】 Task with name '$it' not found in project:$projectName")
            } else {
                logDebug("【jarTaskEmptyJar】 Task with name '$it' is empty in project:$projectName")
            }
        }
        whenReady?.invoke(this)
    }
}

/**
 * 任务添加的时候打印日志
 */
fun Project.logTasks() {
    tasks.whenTaskAdded {
        logDebug("whenTaskAdded -> $name > ${this::class.simpleName}.class ")
        dependsOn.forEach {
            logDebug("  dependsOn: $it")
        }
    }
}

/**
 * 全局常量
 */
val isCI: Boolean by lazy {
    System.getenv("CI") == "true" || System.getenv("GITHUB_ACTIONS") == "true" || System.getenv("JENKINS_HOME") != null
}

val beijingTimeVersion: String by lazy {
    java.time.LocalDateTime.now(java.time.ZoneId.of("Asia/Shanghai"))
        .format(java.time.format.DateTimeFormatter.ofPattern("yyyy.MM.dd"))
}

