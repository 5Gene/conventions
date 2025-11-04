package june.wing

import org.gradle.api.Project

/**
 * SourceSet 相关扩展函数
 * 用于配置源码目录
 */

/**
 * 为项目添加源码目录
 * 支持 Android、Kotlin、Java 项目
 */
fun Project.srcDirs(vararg setSrcDirs: Any) {
    val projectName = name
    androidExtension?.apply {
        srcDirs(*setSrcDirs)
    } ?: kotlinExtension?.apply {
        sourceSets.getByName("main").kotlin {
            logDebug("project:$projectName $this add src dirs ${setSrcDirs.joinToString()} ")
            srcDirs(*setSrcDirs)
        }
    } ?: javaExtension?.apply {
        sourceSets.getByName("main").java {
            logDebug("project:$projectName $this add src dirs ${setSrcDirs.joinToString()} ")
            srcDirs(*setSrcDirs)
        }
    }
}

/**
 * 为 Android Extension 添加源码目录
 */
fun AndroidCommonExtension.srcDirs(vararg setSrcDirs: Any) {
    sourceSets.getByName("main") {
        kotlin {
            logDebug("android ${this.name} add src dirs ${setSrcDirs.joinToString()} ")
            srcDirs(*setSrcDirs)
        }
    }
}

