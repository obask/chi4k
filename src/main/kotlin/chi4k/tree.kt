package chi4k

import org.eclipse.jetty.http.HttpParser.HttpHandler

// Radix tree implementation below is a based on the original work by
// Armon Dadgar in https://github.com/armon/go-radix/blob/master/radix.go
// (MIT licensed). It's been heavily modified for use as a HTTP routing tree.

object HttpMethod {
    const val CONNECT = "CONNECT"
    const val DELETE = "DELETE"
    const val GET = "GET"
    const val HEAD = "HEAD"
    const val OPTIONS = "OPTIONS"
    const val PATCH = "PATCH"
    const val POST = "POST"
    const val PUT = "PUT"
    const val TRACE = "TRACE"
}

@Suppress("EnumEntryName")
enum class MethodTyp(val value: Int) {
    mSTUB(1 shl 0),
    mCONNECT(1 shl 1),
    mDELETE(1 shl 2),
    mGET(1 shl 3),
    mHEAD(1 shl 4),
    mOPTIONS(1 shl 5),
    mPATCH(1 shl 6),
    mPOST(1 shl 7),
    mPUT(1 shl 8),
    mTRACE(1 shl 9),
    mALL(1 shl 10);
}

val methodMap = mapOf(
    HttpMethod.CONNECT to MethodTyp.mCONNECT,
    HttpMethod.DELETE to MethodTyp.mDELETE,
    HttpMethod.GET to MethodTyp.mGET,
    HttpMethod.HEAD to MethodTyp.mHEAD,
    HttpMethod.OPTIONS to MethodTyp.mOPTIONS,
    HttpMethod.PATCH to MethodTyp.mPATCH,
    HttpMethod.POST to MethodTyp.mPOST,
    HttpMethod.PUT to MethodTyp.mPUT,
    HttpMethod.TRACE to MethodTyp.mTRACE,
)

val reverseMethodMap = mapOf(
    MethodTyp.mCONNECT to HttpMethod.CONNECT,
    MethodTyp.mDELETE to HttpMethod.DELETE,
    MethodTyp.mGET to HttpMethod.GET,
    MethodTyp.mHEAD to HttpMethod.HEAD,
    MethodTyp.mOPTIONS to HttpMethod.OPTIONS,
    MethodTyp.mPATCH to HttpMethod.PATCH,
    MethodTyp.mPOST to HttpMethod.POST,
    MethodTyp.mPUT to HttpMethod.PUT,
    MethodTyp.mTRACE to HttpMethod.TRACE
)

data class Endpoint(
    var handler: HttpHandler? = null,
    var pattern: String = "",
    var paramKeys: List<String> = listOf()
)

// Assuming you have Kotlin data classes or equivalent for MethodTyp, node, endpoint, http.Handler

fun MutableMap<MethodTyp, Endpoint>.value(method: MethodTyp): Endpoint {
    return this[method] ?: Endpoint().also { this[method] = it }
}



// Sort the list of nodes by label
fun List<Node>.sortNodes(): List<Node> {
    return this.sortedBy { it.label }.tailSort()
}

private fun List<Node>.tailSort(): List<Node> {
    for (i in this.size - 1 downTo 0) {
        if (this[i].typ > NodeTyp.NT_STATIC && this[i].tail == '/') {
            return this.swap(i, this.size - 1)
        }
    }
    return this
}

private fun List<Node>.swap(i: Int, j: Int): List<Node> {
    val mutableList = this.toMutableList()
    val temp = mutableList[i]
    mutableList[i] = mutableList[j]
    mutableList[j] = temp
    return mutableList
}

fun nodesFindEdge(nodes: List<Node>, label: Char): Node? {
    val index = nodes.binarySearchBy(label) { it.label }
    if (index < 0) return null
    return nodes[index]
}

data class Route(
    val subRoutes: Routes?,
    val handlers: Map<String, HttpHandler>,
    val pattern: String
)

typealias WalkFunc = (method: String, route: String, handler: HttpHandler, middlewares: List<Middleware>) -> Unit

fun walk(
    r: Routes,
    walkFn: WalkFunc,
    parentRoute: String,
    vararg parentMw: (HttpHandler) -> HttpHandler
): Error? {
    for (route in r.routes()) {
        val mws = parentMw.toMutableList()
        mws.addAll(r.middlewares())

        val subRoutes = route.subRoutes
        if (subRoutes != null) {
            walk(subRoutes, walkFn, parentRoute + route.pattern, *mws.toTypedArray())
            continue
        }

        for ((method, handler) in route.handlers) {
            if (method == "*") {
                // Ignore a "catchAll" method, since we pass down all the specific methods for each route.
                continue
            }

            var fullRoute = parentRoute + route.pattern
            fullRoute = fullRoute.replace("/*/", "/")

            when (handler) {
                is ChainHandler -> {
                    val middlewares: List<Middleware> = mws + handler.middlewares
                    walkFn(method, fullRoute, handler.endpoint, middlewares)
                }
                else -> {
                    walkFn(method, fullRoute, handler, mws)
                }
            }
        }
    }

    return null
}


enum class NodeTyp {
    NT_STATIC,   // equivalent to /home
    NT_REGEXP,   // equivalent to /{id:[0-9]+}
    NT_PARAM,    // equivalent to /{user}
    NT_CATCH_ALL // equivalent to /api/v1/*
}

data class SegmentDetails(
    val type: NodeTyp,
    val key: String,
    val regex: String,
    val tail: Char,
    val startIndex: Int,
    val endIndex: Int,
)

fun patNextSegment(pattern: String): SegmentDetails {
    val ps = pattern.indexOf("{")
    val ws = pattern.indexOf("*")

    if (ps < 0 && ws < 0) {
        return SegmentDetails(NodeTyp.NT_STATIC, "", "", '/', 0, pattern.length)
    }

    if (ps >= 0 && ws >= 0 && ws < ps) {
        throw IllegalArgumentException("Wildcard '*' must be the last pattern in a route, otherwise use a '{param}'")
    }

    var tail = '/' // Default endpoint tail to '/' char

    if (ps >= 0) {
        // Param/Regexp pattern is next
        var type = NodeTyp.NT_PARAM
        var pe = ps
        var cc = 0
        for ((i, c) in pattern.substring(ps).withIndex()) {
            if (c == '{') cc++
            else if (c == '}') {
                cc--
                if (cc == 0) {
                    pe = ps + i
                    break
                }
            }
        }

        if (pe == ps) {
            throw IllegalArgumentException("Route param closing delimiter '}' is missing")
        }

        var key = pattern.substring(ps + 1, pe)
        pe++ // set end to next position

        if (pe < pattern.length) {
            tail = pattern[pe]
        }

        var regex = ""
        if (key.contains(":")) {
            type = NodeTyp.NT_REGEXP
            regex = key.substringAfter(":")
            key = key.substringBefore(":")
        }

        if (regex.isNotEmpty()) {
            if (!regex.startsWith('^')) regex = "^$regex"
            if (!regex.endsWith('$')) regex += "$"
        }

        return SegmentDetails(type, key, regex, tail, ps, pe)
    }

    // Wildcard pattern as finale
    if (ws < pattern.length - 1) {
        throw IllegalArgumentException("Wildcard '*' must be the last value in a route. trim trailing text or use a '{param}' instead")
    }
    return SegmentDetails(NodeTyp.NT_CATCH_ALL, "*", "", '/', ws, pattern.length)
}

fun patParamKeys(pattern: String): List<String> {
    var pat = pattern
    val paramKeys = mutableListOf<String>()
    while (true) {
        val (ptyp, paramKey, _, _, _, e) = patNextSegment(pat)
        if (ptyp == NodeTyp.NT_STATIC) {
            return paramKeys
        }
        if (paramKey in paramKeys) {
            throw IllegalArgumentException("Routing pattern '$pattern' contains duplicate param key, '$paramKey'")
        }
        paramKeys.add(paramKey)
        pat = pat.substring(e)
    }
}

// Finds the length of the shared prefix of two strings
fun longestPrefix(k1: String, k2: String): Int {
    val max = minOf(k1.length, k2.length)
    for (i in 0..<max) {
        if (k1[i] != k2[i]) {
            return i
        }
    }
    return max
}

fun methodTypString(method: MethodTyp): String {
    return methodMap.entries.firstOrNull { it.value == method }?.key ?: ""
}

typealias Nodes = MutableList<Node>

fun Nodes.xSort() {
    this.sortWith(compareBy { it.label })
    tailSort()
}

fun Nodes.tailSort() {
    for (i in this.size - 1 downTo 0) {
        if (this[i].typ != NodeTyp.NT_STATIC && this[i].tail == '/') {
            val temp = this[i]
            this[i] = this[this.size - 1]
            this[this.size - 1] = temp
            return
        }
    }
}

fun Nodes.findEdge(label: Char): Node? {
    val num = this.size
    var idx = 0
    var i = 0
    var j = num - 1
    while (i <= j) {
        idx = i + (j - i) / 2
        when {
            label > this[idx].label -> i = idx + 1
            label < this[idx].label -> j = idx - 1
            else -> i = num // breaks condition
        }
    }
    if (this[idx].label != label) {
        return null
    }
    return this[idx]
}
