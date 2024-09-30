/*
 * Copyright 2023 The Android Open Source Project
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

@file:Suppress("UNCHECKED_CAST")

package wing

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.dsl.VariantDimension
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import kotlin.io.path.isDirectory
import kotlin.io.path.listDirectoryEntries
import kotlin.jvm.optionals.getOrNull


fun Project.log(msg: String) {
    //🎉 📣 🎗️ 🔥 📜 💯 📸 🎲 🚀 💡 🔔 🔪 🐼 ✨
    //    println("🎗️ $name >>> $msg".yellow)
    println("🔪 $name--> tid:${Thread.currentThread().id} $msg".yellow)
}

internal val Project.vlibs
    get(): VersionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

//要兼容 application和library 这里的泛型必须 用*全匹配
typealias AndroidCommonExtension = CommonExtension<*, *, *, *, *, *>

//要兼容 application和library 这里的泛型必须 用*全匹配
typealias AndroidComponentsExtensions = AndroidComponentsExtension<CommonExtension<*, *, *, *, *, *>, *, *>

/**
 * @deprecated Use {@code androidComponents} instead
 */
val Project.androidExtension
    get(): AndroidCommonExtension? = extensions.findByName("android") as? AndroidCommonExtension

val Project.androidLibExtension
    get(): LibraryExtension? = extensions.findByName("android") as? LibraryExtension

val Project.javaExtension
    get(): JavaPluginExtension? = extensions.findByType(JavaPluginExtension::class.java)

val Project.kotlinExtension
    get(): KotlinJvmProjectExtension? = extensions.findByType(KotlinJvmProjectExtension::class.java)

val Project.androidExtensionComponent
    get(): AndroidComponentsExtensions? = extensions.findByName("androidComponents") as? AndroidComponentsExtensions

val Project.isAndroidApplication
    get(): Boolean = androidExtension is ApplicationExtension

val Project.isAndroidApp
    get(): Boolean = androidExtensionComponent is ApplicationAndroidComponentsExtension

val Project.isAndroidLibrary
    get(): Boolean = androidExtension is LibraryExtension

val Project.isAndroidLib
    get(): Boolean = androidExtensionComponent is LibraryAndroidComponentsExtension

fun VariantDimension.defineStr(name: String, value: String) {
    buildConfigField("String", name, "\"$value\"")
}

fun VariantDimension.defineBool(name: String, value: Boolean) {
    buildConfigField("boolean", name, value.toString())
}

fun VariantDimension.defineInt(name: String, value: Int) {
    buildConfigField("int", name, value.toString())
}

fun VariantDimension.defineFloat(name: String, value: Int) {
    buildConfigField("float", name, value.toString())
}

fun VariantDimension.defineResStr(name: String, value: String) {
    //使用方式 getResources().getString(R.string.name) 值为value
    resValue("string", name, value)
}

fun VariantDimension.defineResInt(name: String, value: String) {
    //使用方式 getResources().getInteger(R.string.name) 值为value
    resValue("integer", name, value)
}

fun VariantDimension.defineResBool(name: String, value: String) {
    //使用方式 getResources().getBoolean(R.string.name) 值为value
    resValue("bool", name, value)
}

fun Project.changeAPkName(name: String) {
    setProperty("archivesBaseName", name)
}

fun Collection<*>.toStr(): String {
    return toTypedArray().contentToString()
}

fun AndroidCommonExtension.kspSourceSets() {
    srcDirs(
        "build/generated/ksp/main/kotlin",
        "build/generated/ksp/main/java"
    )
}

fun AndroidCommonExtension.srcDirs(vararg srcDirs: Any) {
    sourceSets.getByName("main") {
        kotlin {
            srcDirs(srcDirs)
        }
    }
}

fun RepositoryHandler.chinaRepos() {
    maven {
        name = "tencent"
        isAllowInsecureProtocol = true
        setUrl("https://mirrors.tencent.com/nexus/repository/maven-public/")
    }
    google()
    mavenCentral()
    //限定指定规则的group只访问5hmlA仓库
    exclusiveContent {
        forRepository {
            maven {
                name = "5hmlA"
                isAllowInsecureProtocol = true
                setUrl("https://maven.pkg.github.com/5hmlA/sparkj")
                credentials {
                    // https://www.sojson.com/ascii.html
                    username = "5hmlA"
                    password =
                        "\u0067\u0068\u0070\u005f\u004f\u0043\u0042\u0045\u007a\u006a\u0052\u0069\u006e\u0043\u0065\u0048\u004c\u0068\u006b\u0052\u0036\u0056\u0061\u0041\u0074\u0068\u004f\u004a\u0059\u0042\u0047\u0044\u0073\u0049\u0032\u0070\u0064\u0064\u0069\u0066"
                }
            }
        }
        filter {
            includeGroup("osp.june")
            includeGroup("osp.sparkj")
        }
    }

}

fun java.nio.file.Path.isGradleProject(): Boolean = if (!isDirectory()) false else listDirectoryEntries().any {
    it.toString() == "build.gradle.kts"
}

//class ProjectRead(project: Project) : ReadOnlyProperty<Project, String> {
//    override fun getValue(thisRef: Project, property: KProperty<*>): String {
//        return thisRef.properties[property.name]?.toString() ?: System.getenv(property.name)
//    }
//}

val String.lookDown: String
    get() = "👇👇👇👇👇👇👇👇👇👇👇👇👇👇👇 $this 👇👇👇👇👇👇👇👇👇👇👇👇👇👇👇"

val String.lookup: String
    get() = "👆👆👆👆👆👆👆👆👆👆👆👆👆👆👆 $this 👆👆👆👆👆👆👆👆👆👆👆👆👆👆👆"

fun Project.property(name: String, def: Any): String {
    return (findProperty(name) ?: def).toString()
}

/**
 * 把已有的sourcesJar任务修改为不打包任何内容
 */
fun Project.sourceJarEmpty() {
    afterEvaluate {
        //把已有的sourcesJar任务排查所有内容
        tasks.named<Jar>("sourcesJar") {
            exclude("**/*")
        }
    }
}

/**
 * 任务添加的时候打印日志
 */
fun Project.logTasks() {
    tasks.whenTaskAdded {
        println("whenTaskAdded -> $name > ${this::class.simpleName}.class ")
        dependsOn.forEach {
            println(it)
        }
    }
}


fun VersionCatalog?.findVersionStr(alias: String) = this?.findVersion(alias)?.getOrNull()?.toString()

fun VersionCatalog.getVersion(alias: String) = findVersion(alias).get().requiredVersion