package chi4k

data class HttpRequest(val url: Url, val method: String, val context: Map<Any, Any>)
data class HttpResponse(val statusCode: Int, val body: String)
interface HttpResponseWriter {
    var header: Headers
    fun writeStatus(status: Int)
    fun write(body: String?)
}

// Placeholder for URL and Headers data structure
data class Url(val rawPath: String, val path: String)
data class Headers(val values: MutableMap<String, String>) {
    fun add(name: String, value: String) {
        values[name] = value
    }
}
