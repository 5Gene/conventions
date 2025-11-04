package june.wing

import org.gradle.api.Project
import org.gradle.api.logging.LogLevel

/**
 * 日志扩展函数
 * 提供带日志级别的日志功能，提升构建性能
 */

/**
 * 检查项目的日志级别是否启用
 */
private fun Project.isLogLevelEnabled(level: LogLevel): Boolean {
    return logger.isEnabled(level)
}

/**
 * 带日志级别的日志输出
 * 默认使用 DEBUG 级别，生产环境可通过 -PlogLevel=INFO 控制
 */
fun Project.log(msg: String, level: LogLevel = LogLevel.DEBUG) {
    if (isLogLevelEnabled(level)) {
        when (level) {
            LogLevel.DEBUG -> logger.debug("🔪 $name--> tid:${Thread.currentThread().id} $msg".yellow)
            LogLevel.INFO -> logger.info("🔪 $name--> $msg".yellow)
            LogLevel.WARN -> logger.warn("⚠️ $name--> $msg".yellow)
            LogLevel.ERROR -> logger.error("❌ $name--> $msg".red)
            else -> logger.lifecycle("🔪 $name--> $msg".yellow)
        }
    }
}

/**
 * Debug 级别日志
 */
fun Project.logDebug(msg: String) {
    log(msg, LogLevel.DEBUG)
}

/**
 * Info 级别日志
 */
fun Project.logInfo(msg: String) {
    log(msg, LogLevel.INFO)
}

/**
 * Warn 级别日志
 */
fun Project.logWarn(msg: String) {
    log(msg, LogLevel.WARN)
}

/**
 * Error 级别日志
 */
fun Project.logError(msg: String) {
    log(msg, LogLevel.ERROR)
}

/**
 * 获取日志级别配置
 * 支持通过 gradle.properties 或系统属性配置
 */
fun Project.getLogLevel(): LogLevel {
    val levelStr = findProperty("logLevel")?.toString()?.uppercase()
        ?: System.getProperty("logLevel")?.uppercase()
    
    return when (levelStr) {
        "ERROR" -> LogLevel.ERROR
        "WARN" -> LogLevel.WARN
        "INFO" -> LogLevel.INFO
        "DEBUG" -> LogLevel.DEBUG
        else -> LogLevel.DEBUG // 默认 DEBUG，但只在启用时才输出
    }
}

