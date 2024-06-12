package wing

import org.gradle.internal.impldep.org.apache.http.HttpEntity
import org.gradle.internal.impldep.org.apache.http.client.methods.HttpPost
import org.gradle.internal.impldep.org.apache.http.impl.client.CloseableHttpClient
import org.gradle.internal.impldep.org.apache.http.impl.client.HttpClients
import org.gradle.internal.impldep.org.apache.http.util.EntityUtils
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL


fun post(url: String, authorization: String, json: String? = null): String? {
    return request("POST", url, authorization, json)
}

fun request(method: String = "POST", url: String, authorization: String, json: String? = null): String? {
    try {
        // 创建 URL 对象
        val url = URL(url)
        // 打开连接
        val conn = url.openConnection() as HttpURLConnection

        // 设置请求方法为 POST
        conn.requestMethod = method

        // 设置请求头
        conn.setRequestProperty("Content-Type", "application/json; utf-8")
        conn.setRequestProperty("Authorization", authorization)
        conn.setRequestProperty("Accept", "application/json")

        if (json != null) {// 允许写入数据
            conn.doOutput = true
            // 发送请求体
            conn.outputStream.use { os: OutputStream ->
                val input: ByteArray = json.toByteArray(Charsets.UTF_8)
                os.write(input, 0, input.size)
            }
        }

        val responseCode = conn.responseCode
        try {
            return conn.inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            e.printStackTrace()
            println("File upload failed with response code $responseCode")
            return null
        }
    } catch (e: Exception) {
        e.printStackTrace()
        return null
    }
}


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
