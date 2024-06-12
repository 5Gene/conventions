package wing

import org.gradle.internal.impldep.org.apache.http.HttpEntity
import org.gradle.internal.impldep.org.apache.http.client.methods.HttpPost
import org.gradle.internal.impldep.org.apache.http.impl.client.CloseableHttpClient
import org.gradle.internal.impldep.org.apache.http.impl.client.HttpClients
import org.gradle.internal.impldep.org.apache.http.util.EntityUtils
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.net.HttpURLConnection
import java.net.URL

fun uploadFileToServer(url: String, authorization: String, file: File): String? {
    val boundary = "===" + System.currentTimeMillis() + "==="
    val lineEnd = "\r\n"
    val twoHyphens = "--"
    val maxBufferSize = 1024 * 1024

    val conn = URL(url).openConnection() as HttpURLConnection
    conn.doInput = true
    conn.doOutput = true
    conn.useCaches = false
    conn.requestMethod = "POST"
    conn.setRequestProperty("Authorization", authorization)
    conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=$boundary")
    conn.setRequestProperty("uploaded_file", file.name)

    val dos = DataOutputStream(conn.outputStream)

    dos.writeBytes(twoHyphens + boundary + lineEnd)
    dos.writeBytes("Content-Disposition: form-data; name=\"bundle\";filename=\"${file.name}\"$lineEnd")
    dos.writeBytes("Content-Type: application/octet-stream$lineEnd")
    dos.writeBytes(lineEnd)

    val fileInputStream = FileInputStream(file)
    val buffer = ByteArray(maxBufferSize)
    var bytesRead = fileInputStream.read(buffer, 0, maxBufferSize)

    while (bytesRead > 0) {
        dos.write(buffer, 0, bytesRead)
        bytesRead = fileInputStream.read(buffer, 0, maxBufferSize)
    }

    dos.writeBytes(lineEnd)
    dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd)

    fileInputStream.close()
    dos.flush()
    dos.close()

    val responseCode = conn.responseCode
    val responseMessage = conn.responseMessage

    println("HTTP Response: $responseCode $responseMessage")

    try {
        return conn.inputStream.bufferedReader().use { it.readText() }
    } catch (e: Exception) {
        println("File upload failed with response code $responseCode")
        return null
    }
}

fun httpPost(url: String, header: Map<String, String>, body: (() -> HttpEntity)? = null): String? {
    // 创建 HttpClient 实例
    val httpClient: CloseableHttpClient = HttpClients.createDefault()

    try {
        // 创建一个 POST 请求
        val httpPost = HttpPost(url)

        // 设置请求头，包含认证信息和 Content-Type
        header.forEach { (key, value) ->
            httpPost.setHeader(key, value)
        }
        httpPost.entity = body?.invoke()

        // 执行请求并获取响应
        httpClient.execute(httpPost).use { response ->
            // 获取响应实体
            val entity = response.entity

            // 确保实体不为空
            if (entity != null) {
                // 将响应内容转换为字符串
                val responseContent = EntityUtils.toString(entity)
                return responseContent
            }
        }
        return null
    } catch (e: Exception) {
        // 捕获并打印异常
        e.printStackTrace()
        return null
    } finally {
        // 关闭 HttpClient
        httpClient.close()
    }
}
