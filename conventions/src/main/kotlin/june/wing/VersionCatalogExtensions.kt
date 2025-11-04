package june.wing

import org.gradle.api.artifacts.VersionCatalog
import kotlin.jvm.optionals.getOrNull

/**
 * VersionCatalog 扩展函数
 * 提供版本目录的便捷访问方法
 */

/**
 * 从 VersionCatalog 中查找版本字符串
 * @param catalog VersionCatalog 实例，可为 null
 * @param alias 版本别名
 * @return 版本字符串，如果不存在返回 null
 */
fun VersionCatalog?.findVersionStr(alias: String): String? = 
    this?.findVersion(alias)?.getOrNull()?.toString()

/**
 * 获取版本字符串（必须存在）
 * @throws IllegalStateException 如果版本不存在
 */
fun VersionCatalog.getVersion(alias: String): String = 
    findVersion(alias).get().requiredVersion

