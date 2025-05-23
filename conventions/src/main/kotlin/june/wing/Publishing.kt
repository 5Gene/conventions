package june.wing

import com.android.build.api.dsl.LibraryExtension
import june.tasks.PublishToMavenCentralTask
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.bundling.Zip
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.repositories
import org.gradle.plugins.signing.SigningExtension
import java.io.File
import java.util.Locale

internal const val LOCAL_REPO_PATH = ".maven"
internal const val LOCAL_REPO_NAME = "JuneLocal"

/**
 * - 1 配置publish
 * - 2 通过singing签名
 * - 3 签名后通过task上传
 */
fun Project.publishJavaMavenCentral(libDescription: String, withSource: Boolean = false) =
    publishMavenCentral(libDescription, "java", withSource)

fun Project.publishAndroidMavenCentral(libDescription: String) = publishMavenCentral(libDescription, "debug", false)

fun Project.publishJava5hmlA(libDescription: String, withSource: Boolean = false): PublishingExtension {
    return publish5hmlA(libDescription, "java", withSource)
}

//<editor-fold desc="MavenCentral-publish">
/**
 * ## 发布 library 的基本配置
 * - 配置发布仓库地址，配置了发布到GitHub，本地Maven路径为：build/repo
 * - 配置了名为 Spark 的 publication
 * - 配置发布的component
 * - 配置是否需要sourceJar
 * - 配置library的描述
 */
fun Project.publish5hmlA(
    libDescription: String,
    component: String = "debug",
    withSource: Boolean = false
): PublishingExtension {
    val projectName = name
    if (!pluginManager.hasPlugin("maven-publish")) {
        pluginManager.apply("maven-publish")
    }
    androidLibExtension?.apply {
        androidLibPublishing(component)
        if (!withSource) {
            //android相关task
            //debugSourcesJar 是 Android Gradle Plugin 默认任务，打包的内容仅包含与 debug 变体相关的源码文件。
            //whenTaskAdded -> debugSourcesJar > Jar_Decorated.class
            //whenTaskAdded -> releaseSourcesJar > Jar_Decorated.class

            //whenTaskAdded -> sourceDebugJar > SourceJarTask_Decorated.class
            //whenTaskAdded -> javaDocDebugJar > JavaDocJarTask_Decorated.class
            jarTaskEmptyJar(
                "source${component.firstCharCapitalize()}Jar",
                "javaDoc${component.firstCharCapitalize()}Jar",
            ) {
                tasks.findByName("javaDoc${component.firstCharCapitalize()}Generation")?.let { task ->
                    task.enabled = false
                }
            }
        }
    } ?: javaExtension?.apply {
        javaLibPublishing()
        if (!withSource) {
            jarTaskEmptyJar(
                "sourcesJar",
                "javadocJar",
            ) {
                tasks.findByName("javaDocGeneration")?.let { task ->
                    task.enabled = false
                }
            }
        }
    }

    val gitUrl: String by url()
    val publishingExtension = setPublishing {
        publications {
            repositories {
                maven {
                    name = "GithubPackages"
                    url = uri("https://maven.pkg.github.com/5hmlA/sparkj")
                    credentials {
                        username = System.getenv("GITHUB_USER")
                        password = System.getenv("GITHUB_TOKEN")
                    }
                }
                maven {
                    name = LOCAL_REPO_NAME
                    setUrl(LOCAL_REPO_PATH)
                }
            }
            register("Spark", MavenPublication::class.java) {
                groupId = group.toString().lowercase()
                //artifactId = name
                version = this@publish5hmlA.version.toString()
                val foundComponent = components.any { it.name == component }
                if (foundComponent) {
                    components.forEach {
                        println("【$projectName】components-> ${it.name}")
                    }
                    from(components[component])
                } else {
                    afterEvaluate {
                        components.forEach {
                            println("【$projectName】afterEvaluate-> components-> ${it.name}")
                        }
                        from(components[component])
                        //下面的方式在pom中【不会】自动生成依赖
                        //artifact(tasks.getByName("bundleDebugAar"))
                    }
                }

                //下面配置会出现 Cannot publish module metadata because an artifact from the 'java' component has been removed
                //afterEvaluate {
                //    from(components.getByName("java"))
                //}

                //如果需要包含其他文件，比如 ProGuard 映射文件
                // artifact("$buildDir/outputs/mapping/release/mapping.txt") {
                //     classifier = "mapping"
                // }
                pom {
                    description = libDescription
                    url = gitUrl.removeSuffix(".git")
                    licenses {
                        license {
                            name = "The Apache License, Version 2.0"
                            url = "http://www.apache.org/licenses/LICENSE-2.0.txt"
                        }
                    }
                    developers {
                        developer {
                            id.set("5hmlA")
                            name.set("ZuYun")
                            email.set("jonsa.jzy@gmail.com")
                            url.set("https://github.com/5hmlA")
                        }
                    }
                    scm {
                        connection.set("scm:git:$gitUrl")
                        developerConnection.set("scm:git:ssh:${gitUrl.substring(6)}")
                        url.set(gitUrl.removeSuffix(".git"))
                    }
                }
            }
        }
    }
    return publishingExtension
}

/**
 * 签名配置
 */
private fun Project.signingPublications(publishing: PublishingExtension) {
    if (!pluginManager.hasPlugin("signing")) {
        pluginManager.apply("signing")
    }
    //gpg --armor --export-secret-key 查看signingKey
    //https://stackoverflow.com/questions/70929152/gradle-signing-plugin
    extensions.getByType<SigningExtension>().apply {
        val signingKey = System.getenv("SIGN_GPG_KEY")
        val signingPassword = System.getenv("SIGN_GPG_PASSWORD")
        useInMemoryPgpKeys(signingKey, signingPassword)
        sign(publishing.publications["Spark"])
    }
}

/**
 * ## 配置 library 发布到 MavenCentral
 * - 需要给publications签名，给jar,doc.jar,pom.xml等文件签名
 * - 需要配置MavenCentral的账号密码
 * - 发布到MavenCentral必须要有javadoc.jar,source.jar
 */
fun Project.publishMavenCentral(libDescription: String, component: String = "debug", withSource: Boolean = false) {
    val projectName = name

    //配置publish任务, 发布到MavenCentral必须要有sources.jar和javadoc.jar
    val publishing = publish5hmlA(libDescription, component, withSource)

    //配置签名
    signingPublications(publishing)
    //配置压缩任务，后续上传需要
    //1 publishSparkPublicationToLocalRepoRepository
    //2 zipForSignedPublication
    //3 publishToMavenCentral
    // 1 发布,签名
    //这个task会自动依赖signSparkPublication
    //https://docs.gradle.org/current/userguide/kotlin_dsl.html
//    val publishSparkPublicationToLocalRepoRepository by tasks.existing {
//        //执行之前先清空之前发布的内容
//        File(LOCAL_REPO_PATH).deleteRecursively()
//    }
    val publishToLocalRepo = tasks["publishSparkPublicationTo${LOCAL_REPO_NAME}Repository"].doFirst {
        //执行之前先清空之前发布的内容
        File(LOCAL_REPO_PATH).deleteRecursively()
    }
    // 2 打包,依赖任务publishSparkPublicationToLocalRepoRepository
//    val zipForSignedPublication by tasks.registering(Zip::class) {
//        group = "5hmla"
//        dependsOn(publishToLocalRepo)
//        archiveBaseName = projectName
//        destinationDirectory.set(file("build/zip"))
//        from(LOCAL_REPO_PATH) {
//            include("**/*")
//        }
//    }
    val zipForSignedPublicationTask = tasks.register<Zip>("zipForSignedPublication") {
        group = "5hmla"
        dependsOn(publishToLocalRepo)
        archiveBaseName = projectName
        destinationDirectory.set(file("build/zip"))
        from(LOCAL_REPO_PATH) {
            include("**/*")
        }
    }

    // 3 上传任务 publishToMavenCentral
    tasks.register<PublishToMavenCentralTask>("publishToMavenCentral") {
        group = "5hmla"
        groupId = project.group.toString()
        repositoryUsername = System.getenv("mavenCentralUsername")
        repositoryPassword = System.getenv("mavenCentralPassword")
        from(zipForSignedPublicationTask)
    }

    //最后, 执行task即可 ./gradlew publishToMavenCentral
    println("✨ publishToMavenCentral任务配置成功! ./gradlew publishToMavenCentral")
}
//</editor-fold>


fun Project.setPublishing(config: PublishingExtension.() -> Unit): PublishingExtension {
    return extensions.getByType<PublishingExtension>().apply(config)
}

private fun LibraryExtension.androidLibPublishing(component: String = "debug") {
    println("✨ androidLibPublishing")
    publishing {
        singleVariant(component) {
            withJavadocJar()
            withSourcesJar()
        }
    }
}


private fun JavaPluginExtension.javaLibPublishing() {
    println("✨ javaLibPublishing")
    //kotlin找不到JavadocJar和SourcesJar任务
    withJavadocJar()
    withSourcesJar()
}

fun Project.addLocalRepository() {
    repositories {
        maven {
            name = LOCAL_REPO_NAME
            setUrl(LOCAL_REPO_PATH)
        }
    }
}

//查看某个task的依赖关系
//tasks["your task"].taskDependencies.getDependencies(tasks["your task"]).forEach {
//    println(it.name)
//}
fun Task.showDependencies(action: ((Task) -> Unit)? = null) {
    taskDependencies.getDependencies(this).forEach {
        if (action != null) {
            action.invoke(it)
        } else {
            println("$name dependsOn: ${it.name}")
        }
    }
}

fun String.showDependencies(tasks: TaskContainer) {
    tasks.findByName(this)?.showDependencies()
}

/**
 * 第一个字符大写
 */
fun String.firstCharCapitalize(): String = this.replaceFirstChar { it.titlecase(Locale.getDefault()) }

const val GroupIdMavenCentral = "io.github.5gene"
const val GroupIdGradlePlugin = "io.github.5hmlA"

