import com.test.Http
import com.test.Http.asAuthorizationHeader
import java.io.File
import java.util.Base64

val authorization = "Bearer UGt6ay9MRFA6aWJqd25CQlArdFcwNU1wNC9BMWNTS1hyZGNYWTRDOHk2TElHTUZsUXNOcXMK"

val defDeploymentId = "7dadf850-6adc-4d3c-ae67-c48b6a93a180"

fun geneToken(): String {
    //echo "Pkzk/LDP:ibjwnBBP+tW05Mp4/A1cSKXrdcXY4C8y6LIGMFlQsNqs" | base64
    //ZXhhbXBsZV91c2VybmFtZTpleGFtcGxlX3Bhca3N3b3JkCg==
    val userToken = Base64.getEncoder().encode(
        "Pkzkxxxx:xxxxtW05Mp4/A1cSKXrdcXY4C8y6LIGMFlQsNqs".toByteArray(),
    ).toString(Charsets.UTF_8)
    return "Bearer $userToken"
}

fun getStatus(deploymentId: String = defDeploymentId) {
    val statusUrl = "https://central.sonatype.com/api/v1/publisher/status?id=$deploymentId"
    val statusResult = Http.post(url = statusUrl, authorization.asAuthorizationHeader())
    println(statusResult)
}

fun drop(deploymentId: String = defDeploymentId) {
    val delUrl = "https://central.sonatype.com/api/v1/publisher/deployment/$deploymentId"
    val statusResult = request(Request(url = delUrl, method = "DELETE").addHeaders(authorization.asAuthorizationHeader()))
//    Http.apply {
//        val statusResult = Http.request(Http.Request(url = delUrl, method = "DELETE").addHeaders(authorization.asAuthorizationHeader()))
//        println(statusResult)
//    }
}

fun upload(file: File) {
    val autoService = "AutoService-1.1.2.${System.currentTimeMillis()}"
    val uploadUrl = "https://central.sonatype.com/api/v1/publisher/upload?name=$autoService&publishingType=USER_MANAGED"
    multipart(uploadUrl, authorization.asAuthorizationHeader(), file = file.asMultipart("bundle"))
//    Http.apply {
//        val uploadResult = Http.request(
//            Http.Request(uploadUrl).addHeaders(authorization.asAuthorizationHeader())
//                .addMultipartBody(file.asMultipart("bundle"))
//        )
//        println("uploadingDeploymentBundle -> result: $uploadResult")
//    }

}