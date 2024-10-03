package tasks

import asAuthorizationHeader
import asMultipart
import multipart
import org.gradle.api.internal.file.copy.CopyAction
import org.gradle.api.provider.Property
import org.gradle.api.tasks.AbstractCopyTask
import org.gradle.api.tasks.Input
import post
import java.io.File
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

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
        println(authorization)
        val time = DateTimeFormatter.ofPattern("yyyyMMddHHmm").format(ZonedDateTime.now(ZoneId.of("Asia/Shanghai")))
        val uploadName = "${groupId.get()}:${zipFile.name.replace("zip", time)}"
        println("to upload to MavenCentral > $uploadName")
        //Uploading a Deployment Bundle,    publishingType=USER_MANAGED 手动发布
        val uploadUrl = "https://central.sonatype.com/api/v1/publisher/upload?name=$uploadName&publishingType=AUTOMATIC"
//        val uploadResult = uploadingDeploymentBundle(uploadUrl, authorization, zipFile)
        val uploadResult = multipart(uploadUrl, authorization.asAuthorizationHeader(), file = zipFile.asMultipart("bundle"))
        println("uploadingDeploymentBundle -> result: $uploadResult")
        //28570f16-da32-4c14-bd2e-c1acc0782365,拿到id
        val deploymentId = uploadResult
        val statusUrl = "https://central.sonatype.com/api/v1/publisher/status?id=$deploymentId"
        val statusResult = post(statusUrl, authorization.asAuthorizationHeader())
        println("result: $statusResult")
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
