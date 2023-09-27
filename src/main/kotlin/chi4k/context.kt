package chi4k

data class RouteParams(
    var keys: MutableList<String> = ArrayList(),
    var values: MutableList<String> = ArrayList(),
) {
    fun add(key: String, value: String) {
        keys.add(key)
        values.add(value)
    }
}

data class Context(
    var Routes: Routes? = null,
    private var parentCtx: Any? = null,  // Placeholder for Go's context.Context. Adjust as needed.
    var RoutePath: String = "",
    var RouteMethod: String = "",
    var URLParams: RouteParams = RouteParams(),
    var routeParams: RouteParams = RouteParams(),
    var routePattern: String = "",
    var routePatterns: MutableList<String> = mutableListOf(),
    var methodNotAllowed: Boolean = false,
    var methodsAllowed: MutableList<MethodTyp> = mutableListOf()  // Placeholder. Replace with actual type.

) {
    fun reset() {
        Routes = null
        RoutePath = ""
        RouteMethod = ""
        routePatterns.clear()
        URLParams.keys.clear()
        URLParams.values.clear()
        routePattern = ""
        routeParams.keys.clear()
        routeParams.values.clear()
        methodNotAllowed = false
        parentCtx = null
    }

    fun urlParam(key: String): String {
        for (k in URLParams.keys.size - 1 downTo 0) {
            if (URLParams.keys[k] == key) {
                return URLParams.values[k]
            }
        }
        return ""
    }

    fun routePattern(): String {
        var routePattern = routePatterns.joinToString("")
        routePattern = replaceWildcards(routePattern)
        routePattern = routePattern.trimEnd('/', '/')
        routePattern = routePattern.trimEnd('/')
        return routePattern
    }
}

fun replaceWildcards(p: String): String {
    if (p.contains("/*/")) {
        return replaceWildcards(p.replace("/*/", "/"))
    }
    return p
}

fun urlParam(r: Any, key: String): String { // r is a placeholder. Replace it with the actual type if you know it
    val rctx = routeContext(r)
    if (rctx != null) {
        return rctx.urlParam(key)
    }
    return ""
}

fun urlParamFromCtx(ctx: Any, key: String): String { // ctx is a placeholder. Replace it with the actual type if you know it
    val rctx = routeContext(ctx)
    if (rctx != null) {
        return rctx.urlParam(key)
    }
    return ""
}

fun routeContext(ctx: Any): Context? { // ctx is a placeholder. Replace it with the actual type if you know it
    val value = ctx as? Context  // This is a placeholder conversion. Update as per your need
    return value
}

fun newRouteContext(): Context = Context()

data class ContextKey(val name: String) {
    override fun toString() = "chi context value $name"
}

val RouteCtxKey = ContextKey("RouteContext")
