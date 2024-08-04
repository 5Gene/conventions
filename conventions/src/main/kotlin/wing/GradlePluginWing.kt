package wing

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.buildscript
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.kotlin
import org.gradle.kotlin.dsl.repositories
import org.gradle.plugin.devel.GradlePluginDevelopmentExtension
import org.gradle.plugin.devel.PluginDeclaration

fun Project.gradlePluginSet(emptySource: Boolean = true, action: Action<in NamedDomainObjectContainer<PluginDeclaration>>) {
    if (emptySource) {
        sourceJarEmpty()
    }

    if (!pluginManager.hasPlugin("com.gradle.plugin-publish")) {
        buildscript {
            repositories {
                maven {
                    url = uri("https://plugins.gradle.org/m2/")
                }
            }
            dependencies {
                classpath("com.gradle.publish:plugin-publish-plugin:1.2.1")
            }
        }
        pluginManager.apply("com.gradle.plugin-publish")
        pluginManager.apply("maven-publish")
    }

    //For both the JVM and Android projects, it's possible to define options using the project Kotlin extension DSL:
    kotlinExtension?.apply {
        compilerOptions {
            freeCompilerArgs.add("-Xcontext-receivers")
            apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0)
            languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0)
        }
    }

    dependencies {
        add("compileOnly", kotlin(module = "gradle-plugin-api"))
    }

    //插件推送之前 先去掉不符合规范的插件
    tasks.findByName("publishPlugins")?.doFirst {
        //doFirst on task ':conventions:publishPlugins'
        ">> doFirst on $this ${this.javaClass}".print()
        //不太明白为什么这里也报错 Extension of type 'GradlePluginDevelopmentExtension' does not exist
        //因为取错对象的extensions了，这里的this是com.gradle.publish.PublishTask_Decorated, 这个task也有extensions
        val plugins = rootProject.extensions.getByType<GradlePluginDevelopmentExtension>().plugins
        plugins.removeIf {
            //移除不能上传的插件
            it.displayName.isNullOrEmpty()
        }
        plugins.forEach {
            "- plugin to publish > ${it.name} ${it.id} ${it.displayName}".print()
        }
    }

    val gitUrl by url()

    extensions.getByType(org.gradle.plugin.devel.GradlePluginDevelopmentExtension::class.java).apply {
        website = gitUrl
        vcsUrl = gitUrl

        plugins(action)
        //因为通过 xxx.gradle.kts创建的预编译脚本 会自动创建plugin但是没设置displayName和description
        //所以这里判断补充必要数据否则发布不了，执行 [plugin portal -> publishPlugins]的时候会报错

        plugins.forEach {
            "- plugin -- ${it.name} ${it.id} ${it.displayName}".print()
        }

    }

    tasks.getByName("publishPlugins").doLast {
        "插件发布成功，点击🔗查看：https://plugins.gradle.org/".print()

        "插件地址: https://plugins.gradle.org/u/ZuYun".print()
        //    https://plugins.gradle.org/docs/mirroring
        //    The URL to mirror is https://plugins.gradle.org/m2/
        "插件下载地址: https://plugins.gradle.org/m2/io/github/5hmlA/".print()
    }
}