package wing

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.internal.file.copy.CopyAction
import org.gradle.api.provider.Property
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.AbstractCopyTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.bundling.Zip
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
import org.gradle.plugins.signing.SigningExtension
import org.jetbrains.kotlin.com.google.gson.Gson
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Base64

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

fun Project.signingPublications(publishing: PublishingExtension, signingKey: String? = null) {
    if (!pluginManager.hasPlugin("signing")) {
        pluginManager.apply("signing")
    }
    extensions.getByType<SigningExtension>().apply {
        val signingKey = System.getenv("ORG_GRADLE_PROJECT_signingKey") ?: signingKey
        val signingPassword = System.getenv("ORG_GRADLE_PROJECT_signingPassword") ?: "19910113"
        useInMemoryPgpKeys(signingKey, signingPassword)
        sign(publishing.publications["Spark"])
    }
}


/**
 * - 1 配置publish
 * - 2 通过singing签名
 * - 3 签名后通过task上传
 */
fun Project.publishMavenCentral(libDescription: String, component: String = "release", signingKey: String? = null, emptySourSets: Boolean = false) {
    val projectName = name
    //配置publish任务
    val publishing = publish5hmlA(libDescription, component, emptySourSets)

    //配置签名
    signingPublications(publishing, signingKey)
    //配置压缩任务，后续上传需要
    //1 publishSparkPublicationToLocalRepoRepository
    //2 zipForSignedPublication
    //3 publishToMavenCentral
    // 1 发布,签名
    //这个task会自动依赖signSparkPublication
    val publishToLocalRepo = tasks["publishSparkPublicationToLocalRepoRepository"].doFirst {
        //执行之前先清空之前发布的内容
        File(LOCAL_REPO_PATH).deleteRecursively()
    }
    // 2 打包,依赖任务publishSparkPublicationToLocalRepoRepository
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
    println("✨ publishToMavenCentral任务配置成功! 执行: ./gradlew publishToMavenCentral")
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

fun TaskContainer.registerJavadocJar() {
    register<Jar>("javadocJar") {
        //tasks.named("javadoc")任务生成javadoc,空的javadoc这里就不执行任务即可
        from(named("javadoc"))
        archiveClassifier.set("javadoc")
    }
}

context(Project)
fun TaskContainer.registerSourceJar(component: String, emptySourSets: Boolean = false) {
    register<Jar>("sourcesJar") {
        archiveClassifier.set("sources")
        if (!emptySourSets) {
            try {
                if (component == "java") {
                    from(javaExtension!!.sourceSets["main"].allSource)
                } else {
                    from(androidExtension!!.sourceSets["main"].java)
                    from(androidExtension!!.sourceSets["main"].kotlin)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}

fun Project.publishJava5hmlA(libDescription: String): PublishingExtension {
    return publish5hmlA(libDescription, "java")
}

fun Project.publish5hmlA(libDescription: String, component: String = "release", emptySourSets: Boolean = false): PublishingExtension {
    if (!pluginManager.hasPlugin("maven-publish")) {
        pluginManager.apply("maven-publish")
    }
    tasks.registerJavadocJar()
    tasks.registerSourceJar(component, emptySourSets)
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
                if (component == "release") {
                    afterEvaluate {
                        from(components.getByName(component))
                    }
                } else {
//                  下面配置会出现 Cannot publish module metadata because an artifact from the 'java' component has been removed
//                  afterEvaluate {
//                      from(components.getByName("java"))
//                  }
                    from(components[component])
                }
                //必须是jar所以要把javadoc打包成jar
                artifact(tasks.named("javadocJar"))
                artifact(tasks.named("sourceJar"))

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


/**
 * - 继承**AbstractCopyTask**类可以接收上一个task产生的文件
 * - 自定义的Task必须是open或者abstract,否则是无效Task,因为Gradle要继承此类生成代码
 */
abstract class PublishToMavenCentralTask : AbstractCopyTask() {

    @get:Input
    abstract val groupId: Property<String>

    @get:Input
    abstract val repositoryUsername: Property<String>

    @get:Input
    abstract val repositoryPassword: Property<String>

    private fun doUploadToMavenCentral(zipFile: File) {
        val userToken = Base64.getEncoder().encode(
            "${repositoryUsername.get()}:${repositoryPassword.get()}".toByteArray(),
        ).toString(Charsets.UTF_8)
        val authorization = "Bearer $userToken"

        val uploadName = "${groupId.get()}:${zipFile.name.replace("zip", DateTimeFormatter.ofPattern("yyyyMMddHHmm").format(LocalDateTime.now()))}"
        println("to upload to MavenCentral > $uploadName")
        //Uploading a Deployment Bundle,    publishingType=USER_MANAGED 手动发布
        val uploadUrl = "https://central.sonatype.com/api/v1/publisher/upload?name=$uploadName&publishingType=AUTOMATIC"
//        val uploadResult = uploadingDeploymentBundle(uploadUrl, authorization, zipFile)
        val uploadResult = uploadFileToServer(uploadUrl, authorization, zipFile)
        println("uploadingDeploymentBundle -> result: $uploadResult")
        //28570f16-da32-4c14-bd2e-c1acc0782365,拿到id
        val deploymentId = uploadResult
        val statusUrl = "https://central.sonatype.com/api/v1/publisher/status?id=$deploymentId"
        val statusResult = httpPost(statusUrl, mapOf("Authorization" to authorization))
        try {
            val statusMap = Gson().fromJson(statusResult, Map::class.java)
            println("deploymentState: ${statusMap["deploymentState"]}")
        } catch (e: Exception) {
            println(statusResult)
        }
        //{
        //  "deploymentId": "28570f16-da32-4c14-bd2e-c1acc0782365",
        //  "deploymentName": "central-bundle.zip",
        //  "deploymentState": "PUBLISHED",
        //  "purls": [
        //    "pkg:maven/com.sonatype.central.example/example_java_project@0.0.7"
        //  ]
        //}
        //执行结束，后输出日志，
        println("🎉 ${zipFile.name.removeSuffix(".zip")} 发布成功，点击链接🔗查看: https://central.sonatype.com/publishing/deployments")
    }

    override fun createCopyAction(): CopyAction {
        return CopyAction { stream ->
            stream.process {
                //这里每个文件和每个目录都会回调过来
                println("file from pre task, next to upload to MavenCentral > ${it.file}")

                doUploadToMavenCentral(it.file)
            }
            org.gradle.workers.internal.DefaultWorkResult.SUCCESS
        }
    }
}

