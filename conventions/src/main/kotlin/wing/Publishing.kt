package wing

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.Zip
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
import java.io.ByteArrayOutputStream

const val LOCAL_REPO_PATH = "build/repo"
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


/**
 * - 1 配置publish
 * - 2 通过singing签名
 * - 3 签名后通过task上传
 */
fun Project.publishMavenCentral(libDescription: String, component: String = "release") {
    //配置publish任务
    publish5hmlA(libDescription, component)

    //配置压缩任务，后续上传需要
    val projectName = name
    //创建压缩文件task,此task依赖发布library
    tasks.register<Zip>("zipForPublish") {
//        group = "5hmlA"
        dependsOn(tasks["publishSparkPublicationToLocalRepoRepository"])
        archiveBaseName = projectName
        destinationDirectory.set(file(LOCAL_REPO_PATH))
        from("repos") {
            include("**/*")
        }
    }

    //配置 sign
    if (!pluginManager.hasPlugin("signing")) {
        pluginManager.apply("signing")
    }
    //https://docs.gradle.org/current/userguide/signing_plugin.html#sec:publishing_the_signatures
    extensions.getByType<org.gradle.plugins.signing.SigningExtension>().apply {
        //配置sign任务依赖zipForPublish任务，而zipForPublish任务依赖publishSparkPublicationToLocalRepoRepository任务
        //也就是执行signZipForPublish任务会先发布library然后压缩文件，然后前面
        sign(tasks["zipForPublish"])
    }
    //实现任务上传到MavenCentral, 此上传task要依赖signZipForPublish
}


//<editor-fold desc="maven-publish">
fun Project.gitUrl(): String {
    val stdout = ByteArrayOutputStream()
    exec {
        commandLine("git", "config", "--get", "remote.origin.url")
        standardOutput = stdout
    }
    val remoteUrl = stdout.toString().trim()
    println("Remote URL: ${remoteUrl.removeSuffix(".git")}")
    return remoteUrl
}

fun Project.publishJava5hmlA(libDescription: String): PublishingExtension {
    return publish5hmlA(libDescription, "java")
}

fun Project.publish5hmlA(libDescription: String, component: String = "release"): PublishingExtension {
    if (!pluginManager.hasPlugin("maven-publish")) {
        pluginManager.apply("maven-publish")
    }
    val gitUrl = gitUrl()
    val publishingExtension = extensions.getByType<PublishingExtension>()
    publishingExtension.apply {
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
                    name = "LocalRepo"
                    setUrl(LOCAL_REPO_PATH)
                }
            }
            register("Spark", MavenPublication::class.java) {
                groupId = group.toString().lowercase()
                //artifactId = name
                version = this@publish5hmlA.version.toString()
                afterEvaluate {
                    from(components[component])
                }

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
//</editor-fold>
