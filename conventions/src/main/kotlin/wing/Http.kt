import java.io.File
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL


const val formDataLineEnd = "\r\n"

fun String.asAuthorizationHeader() = mutableMapOf("Authorization" to this)

fun File.asMultipart(key: String): MultipartFileBody = MultipartFileBody(key, this)


/**
 * Content-Disposition: form-data; name="bundle"; filename="auto-service.zip"
 */
fun File.asContentDisposition(key: String) = "Content-Disposition: form-data; name=\"$key\";filename=\"$name\""

/**
 * Content-Disposition: form-data; name="description"
 */
fun String.asContentDisposition() = "Content-Disposition: form-data; name=\"$this\""

/**
 * Content-Type: xxxx
 */
fun String.asContentType() = "Content-Type: $this"

/**
 * Content-Type: application/octet-stream
 */
fun File.asContentType() = "Content-Type: application/octet-stream"

fun Long.asContentLength() = "Content-Length: $this"

/**
 * multipart/form-data, 表单上传数据
 *
 * ## 知识补充
 * 在HTTP协议中，multipart/form-data 是一种常用的编码格式，用于上传文件和提交表单数据。边界字符串（boundary）是这个格式的一个关键组成部分，
 * 用于分隔不同的表单字段和文件内容。每个分段的数据都以边界字符串开始，并以边界字符串结束，HTTP服务器通过识别这些边界来解析每个部分的内容。
 * - 边界字符串**boundary**的作用
 *      - 分隔不同部分：在multipart/form-data中，表单数据和文件内容被分为多个部分，每个部分都由边界字符串分隔开。例如，上传一个文件可能包含表单字段和文件内容两个部分，每个部分都以边界字符串开始和结束
 *      - 唯一性：边界字符串需要是唯一的，以避免表单内容中出现相同的字符串而导致解析错误。通常，边界字符串是由客户端随机生成的。
 * - 为什么需要边界字符串
 *      - 单纯的Content-Disposition头部信息并不能完整地描述multipart/form-data的结构。Content-Disposition只是描述了一个部分的内容类型和其他相关信息，但并不能指示部分的开始和结束。边界字符串则明确地分隔了每个部分，使得服务器能够正确地解析和处理每个部分。
 * #### 具体的multipart/form-data请求示例如下:
 * ```http
 *      HTTP 请求头, 请求头中告诉服务器,boundary具体内容是什么
 * POST /upload HTTP/1.1
 * Host: example.com
 * Content-Type: multipart/form-data; boundary=WebKitFormBoundary7MA4YWxkTrZu0gW
 * Content-Length: <calculated length>
 *
 *     HTTP 请求体,请求体中,使用boundary边界字符串则明确地分隔了每个部分
 * --WebKitFormBoundary7MA4YWxkTrZu0gW
 * Content-Disposition: form-data; name="description"
 *
 * This is an example file 这是表单的第1个值(description:This is an example file)
 * --WebKitFormBoundary7MA4YWxkTrZu0gW
 * Content-Disposition: form-data; name="what"
 *
 * 你好 这是表单的第2个值(what:你好)
 * --WebKitFormBoundary7MA4YWxkTrZu0gW
 * Content-Disposition: form-data; name="bundle"; filename="auto-service.zip"
 * Content-Type: application/octet-stream
 *
 * <binary content of the file> 这是表单的第3个值(bundle:文件流)
 * --WebKitFormBoundary7MA4YWxkTrZu0gW--
 * ```
 * ### 格式为
 *  - boundary起始边界：用于标识一个新的部分的开始。
 *  - 头信息：描述该部分的内容（例如，Content-Disposition和Content-Type）。
 *  - \r 空行
 *  - 内容：实际的数据部分，例如文件内容或表单字段值。
 *  - boundary结束边界：标识multipart/form-data的结束。
 */
interface MultipartBody {
    fun contentType(): String? = null
    fun contentDisposition(): String
    fun contentLength(): String? = null

    /**
     * Content-Disposition: form-data; name="bundle"; filename="auto-service.zip"
     * Content-Type: application/octet-stream
     *
     * <binary content of the file> 这是表单的第3个值(bundle:文件流)
     */
    fun writeTo(out: OutputStream) {
        val contentDisposition = "${contentDisposition()}$formDataLineEnd"
        out.write(contentDisposition.toByteArray())
        print(contentDisposition)
        contentType()?.let {
            val contentType = "$it$formDataLineEnd"
            out.write(contentType.toByteArray())
            print(contentType)
        }
        contentLength()?.let {
            val contentLength = "${it}$formDataLineEnd"
            out.write(contentLength.toByteArray())
            print(contentLength)
        }
        out.write(formDataLineEnd.toByteArray())
        println(formDataLineEnd)
    }
}

open class MultipartStringBody(val key: String, val value: String) : MultipartBody {

    /**
     * text/plain
     * application/json
     * text/xml
     */
    override fun contentType(): String? = null

    override fun contentDisposition() = key.asContentDisposition()

    override fun writeTo(out: OutputStream) {
        super.writeTo(out)
        out.write("$value$formDataLineEnd".toByteArray())
        println("write str: $value")
    }
}

class MultipartJsonBody(key: String, value: String) : MultipartStringBody(key, value) {
    override fun contentType(): String = "application/json".asContentType()
}

class MultipartFileBody(private val key: String, private val file: File) : MultipartBody {

    /**
     * application/octet-stream
     */
    override fun contentType(): String = file.asContentType()

    override fun contentDisposition() = file.asContentDisposition(key)

    override fun contentLength() = file.length().asContentLength()

    override fun writeTo(out: OutputStream) {
        super.writeTo(out)
        file.inputStream().use {
            it.copyTo(out)
        }
        out.write(formDataLineEnd.toByteArray())
        println("write file ${file.name}")
    }
}

data class Request(val url: String, val method: String = "POST", val params: Map<String, String>? = null) {
    val headers: MutableMap<String, String> = mutableMapOf()
    val multipartBodys: MutableList<MultipartBody> = mutableListOf()
    fun addHeaders(header: Map<String, String>): Request {
        headers.putAll(header)
        return this
    }

    fun addMultipartBody(body: MultipartBody): Request {
        multipartBodys.add(body)
        return this
    }
}

fun multipart(url: String, header: Map<String, String>, params: Map<String, String>? = null, file: MultipartFileBody): String? {
    return request(Request(url, "POST", params).addHeaders(header).addMultipartBody(file))
}

fun post(url: String, header: Map<String, String>, params: Map<String, String>? = null): String? {
    return request(Request(url, "POST", params).addHeaders(header))
}

/**
 * ## 知识补充
 * 在HTTP协议中，multipart/form-data 是一种常用的编码格式，用于上传文件和提交表单数据。边界字符串（boundary）是这个格式的一个关键组成部分，
 * 用于分隔不同的表单字段和文件内容。每个分段的数据都以边界字符串开始，并以边界字符串结束，HTTP服务器通过识别这些边界来解析每个部分的内容。
 * - 边界字符串**boundary**的作用
 *      - 分隔不同部分：在multipart/form-data中，表单数据和文件内容被分为多个部分，每个部分都由边界字符串分隔开。例如，上传一个文件可能包含表单字段和文件内容两个部分，每个部分都以边界字符串开始和结束
 *      - 唯一性：边界字符串需要是唯一的，以避免表单内容中出现相同的字符串而导致解析错误。通常，边界字符串是由客户端随机生成的。
 * - 为什么需要边界字符串
 *      - 单纯的Content-Disposition头部信息并不能完整地描述multipart/form-data的结构。Content-Disposition只是描述了一个部分的内容类型和其他相关信息，但并不能指示部分的开始和结束。边界字符串则明确地分隔了每个部分，使得服务器能够正确地解析和处理每个部分。
 * #### 具体的multipart/form-data请求示例如下:
 * ```http
 *      HTTP 请求头, 请求头中告诉服务器,boundary具体内容是什么
 * POST /upload HTTP/1.1
 * Host: example.com
 * Content-Type: multipart/form-data; boundary=WebKitFormBoundary7MA4YWxkTrZu0gW
 * Content-Length: <calculated length>
 *
 *     HTTP 请求体,请求体中,使用boundary边界字符串则明确地分隔了每个部分
 * --WebKitFormBoundary7MA4YWxkTrZu0gW
 * Content-Disposition: form-data; name="description"
 *
 * This is an example file 这是表单的第1个值(description:This is an example file)
 * --WebKitFormBoundary7MA4YWxkTrZu0gW
 * Content-Disposition: form-data; name="what"
 *
 * 你好 这是表单的第2个值(what:你好)
 * --WebKitFormBoundary7MA4YWxkTrZu0gW
 * Content-Disposition: form-data; name="bundle"; filename="auto-service.zip"
 * Content-Type: application/octet-stream
 *
 * <binary content of the file> 这是表单的第3个值(bundle:文件流)
 * --WebKitFormBoundary7MA4YWxkTrZu0gW--
 * ```
 * ### 格式为
 *  - boundary起始边界：用于标识一个新的部分的开始。
 *  - 头信息：描述该部分的内容（例如，Content-Disposition和Content-Type）。
 *  - \r 空行
 *  - 内容：实际的数据部分，例如文件内容或表单字段值。
 *  - boundary结束边界：标识multipart/form-data的结束。
 */
fun request(request: Request): String? {
    try {
        // 创建 URL 对象
        if (request.params.isNullOrEmpty()) {
            URL(request.url)
        } else {
            val params = request.params.entries.joinToString("&") { "${it.key}=${it.value}" }
            URL("${request.url}?$params")
        }
        val url = URL(request.url)
        // 打开连接
        val conn = url.openConnection() as HttpURLConnection

        // 设置请求方法为 POST
        conn.requestMethod = request.method

        // 设置请求头
//        conn.setRequestProperty("Content-Type", "application/json; utf-8")
//        conn.setRequestProperty("Authorization", authorization)
//        conn.setRequestProperty("Accept", "application/json")
        request.headers.forEach { (key, value) ->
            conn.setRequestProperty(key, value)
        }
        if (request.multipartBodys.isNotEmpty()) {
            conn.doInput = true
            conn.doOutput = true
            conn.useCaches = false
            //写入表单数据
            val twoHyphens = "--"
            val boundary = "===${System.currentTimeMillis()}==="
//            val boundary = "xxxxxxxxxxxxxxxxxxxxxxxxxxx"
            //请求头标注Content-Type,并明确boundary
            conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=$boundary")
            // --xxxxxxxxxxxxxxxxxxxxxxxxxxx 必须要--开头
            // Content-Disposition: form-data; name="description"
            //
            // This is an example file 这是表单的第1个值(description:This is an example file)
            // --xxxxxxxxxxxxxxxxxxxxxxxxxxx
            // Content-Disposition: form-data; name="what"
            //
            // 你好 这是表单的第2个值(what:你好)
            // --xxxxxxxxxxxxxxxxxxxxxxxxxxx
            // Content-Disposition: form-data; name="bundle"; filename="auto-service.zip"
            // Content-Type: application/octet-stream
            //
            // <binary content of the file> 这是表单的第3个值(bundle:文件流)
            // --xxxxxxxxxxxxxxxxxxxxxxxxxxx-- 结尾必须要--开头--结束
            conn.outputStream.use {
                //开始只有一个--也就是--boundary\r\n
                val headBoundary = (twoHyphens + boundary + formDataLineEnd).toByteArray()
                request.multipartBodys.forEach { multipartBody ->
                    it.write(headBoundary)
                    multipartBody.writeTo(out = it)
                }
                //最后必须是 --boundary--\r\n 结束 注意首位的--
                val bottomBoundary = (twoHyphens + boundary + twoHyphens + formDataLineEnd).toByteArray()
                it.write(bottomBoundary)
                it.flush()
            }
        }

        val responseCode = conn.responseCode

        println("request -> response code: $responseCode > ${conn.responseMessage}")
        try {
            return conn.inputStream.bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            e.printStackTrace()
            println("request -> failed with response code: $responseCode > ${conn.responseMessage}")
            return null
        }
    } catch (e: Exception) {
        e.printStackTrace()
        println("request -> failed with exception: ${e.message}")
        return null
    }
}
