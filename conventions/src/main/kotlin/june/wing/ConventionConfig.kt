package june.wing

import org.gradle.api.Project

/**
 * 集中管理 Gradle Conventions 插件的配置常量
 * 减少魔法值，提高可维护性和类型安全性
 */
object ConventionConfig {
    // Android 相关配置属性
    const val PROP_ROOM_ENABLED = "config.android.room"
    const val PROP_DEPENDENCIES_FORCE = "config.android.dependencies.force"
    
    // 项目配置属性
    const val PROP_JAVA_VERSION = "config.project.java.version"
    const val DEFAULT_JAVA_VERSION = 17
    
    // 构建缓存配置属性
    const val PROP_BUILD_CACHE_DIR = "build.cache.root.dir"
    const val DEFAULT_BUILD_CACHE_PATH = ":/0buildCache/"
    
    // Version Catalog 名称
    const val VERSION_CATALOG_NAME = "vcl"
    
    /**
     * 检查是否启用 Room 配置
     */
    fun Project.isRoomEnabled(): Boolean {
        return findProperty(PROP_ROOM_ENABLED) == "true"
    }
    
    /**
     * 获取 Java 版本，默认 17
     */
    fun Project.getJavaVersion(): Int {
        return property(PROP_JAVA_VERSION, DEFAULT_JAVA_VERSION).toInt()
    }
    
    /**
     * 获取构建缓存根目录
     */
    fun Project.getBuildCacheRootDir(): String? {
        return (findProperty(PROP_BUILD_CACHE_DIR) 
            ?: System.getenv(PROP_BUILD_CACHE_DIR))?.toString()
    }
    
    /**
     * 检查是否强制依赖
     */
    fun Project.isDependenciesForce(): Boolean {
        return findProperty(PROP_DEPENDENCIES_FORCE) == "true"
    }
}

