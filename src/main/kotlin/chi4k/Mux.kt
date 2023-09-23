//package chi4k
//
//import org.eclipse.jetty.http.HttpParser.HttpHandler
//import java.net.http.HttpRequest
//import java.net.http.HttpResponse
//import java.util.concurrent.ConcurrentHashMap
//
//// Note: This is a basic representation and might not cover all the functionality.
//// The Java Standard Library's HTTP client (since Java 11) is used for this representation.
//
//class Mux(
//    private var handler: HttpHandler? = null,
//    private var tree: Node? = Node(),
//    private var methodNotAllowedHandler: HttpHandler? = null,
//    private var parent: Mux? = null,
//    private val pool = ConcurrentHashMap<HttpRequest, Context>(),
//    private var notFoundHandler: HttpHandler? = null,
//    private val middlewares = mutableListOf<(HttpHandler) -> HttpHandler>(),
//    private var inline = false,
//) {
//    fun newMux(): Mux {
//        val mux = Mux()
//        mux.tree = Node()
//        mux.pool[HttpRequest.newBuilder().build()] = Context()
//        return mux
//    }
//
//    fun serveHTTP(response: HttpResponse<String>, request: HttpRequest) {
//        handler?.let {
//            if (it == null) {
//                notFoundHandler?.handle(request)
//                return
//            }
//
//            val rctx = pool[request]
//            rctx?.let { ctx ->
//                handler?.handle(request)
//                return
//            }
//
//            val rctxNew = Context()
//            pool[request] = rctxNew
//            handler?.handle(request)
//            pool.remove(request)
//        }
//    }
//
//    fun use(vararg middlewares: (HttpHandler) -> HttpHandler) {
//        if (handler != null) {
//            throw Exception("All middlewares must be defined before routes on a mux")
//        }
//        this.middlewares.addAll(middlewares)
//    }
//
//    fun handle(pattern: String, handler: HttpHandler) {
//        // In the provided Go code, there's an internal `handle` method that
//        // appears to take a method, a pattern, and a handler.
//        // Here, for simplicity, we've removed the method and will focus on the pattern and handler.
//
//        // TODO: Logic for adding a route to the tree using the pattern and handler.
//    }
//
//    fun handleFunc(pattern: String, handlerFn: (HttpRequest) -> HttpResponse<String>) {
//        handle(pattern, HttpHandler { request -> handlerFn(request) })
//    }
//
//    fun get(pattern: String, handlerFn: (HttpRequest) -> HttpResponse<String>) {
//        // TODO: Logic specific for GET requests.
//        handleFunc(pattern, handlerFn)
//    }
//
//    fun post(pattern: String, handlerFn: (HttpRequest) -> HttpResponse<String>) {
//        // TODO: Logic specific for POST requests.
//        handleFunc(pattern, handlerFn)
//    }
//
//    private var notFoundHandler: (HttpRequest) -> HttpResponse<String>? = null
//    private var methodNotAllowedHandler: (HttpRequest) -> HttpResponse<String>? = null
//
//    fun notFound(handlerFn: (HttpRequest) -> HttpResponse<String>) {
//        // Build NotFound handler chain
//        val m = if (inline && parent != null) parent else this
//        val hFn =
//            handlerFn // In Go, this function is transformed with middlewares. The equivalent logic will need to be implemented in Kotlin.
//
//        // Update the notFoundHandler from this point forward
//        m.notFoundHandler = hFn
//        m.updateSubRoutes { subMux ->
//            if (subMux.notFoundHandler == null) {
//                subMux.notFound(handlerFn)
//            }
//        }
//    }
//
//    fun methodNotAllowed(handlerFn: (HttpRequest) -> HttpResponse<String>) {
//        // Build MethodNotAllowed handler chain
//        val m = if (inline && parent != null) parent else this
//        val hFn =
//            handlerFn // In Go, this function is transformed with middlewares. The equivalent logic will need to be implemented in Kotlin.
//
//        // Update the methodNotAllowedHandler from this point forward
//        m.methodNotAllowedHandler = hFn
//        m.updateSubRoutes { subMux ->
//            if (subMux.methodNotAllowedHandler == null) {
//                subMux.methodNotAllowed(handlerFn)
//            }
//        }
//    }
//
//    fun with(vararg middlewares: (HttpRequest) -> HttpResponse<String>): Mux {
//        // Similarly as in handle(), we must build the mux handler once additional
//        // middleware registration isn't allowed for this stack, like now.
//        if (!inline && handler == null) {
//            updateRouteHandler()
//        }
//
//        // Copy middlewares from parent inline muxes
//        val mws = mutableListOf<Middleware>()
//        if (inline) {
//            mws.addAll(this.middlewares)
//        }
//        mws.addAll(middlewares)
//
//        val im = Mux(
//            pool = pool,
//            inline = true,
//            parent = this,
//            tree = tree,
//            middlewares = mws,
//            notFoundHandler = notFoundHandler,
//            methodNotAllowedHandler = methodNotAllowedHandler
//        )
//
//        return im
//    }
//
//    fun group(fn: ((Router) -> Unit)?): Router {
//        val im = with() as Mux
//        fn?.invoke(im)
//        return im
//    }
//
//    fun route(pattern: String, fn: ((Router) -> Unit)?): Router {
//        if (fn == null) {
//            throw IllegalArgumentException("chi: attempting to Route() a nil subrouter on '$pattern'")
//        }
//        val subRouter = NewRouter()
//        fn.invoke(subRouter)
//        mount(pattern, subRouter)
//        return subRouter
//    }
//
//    fun mount(pattern: String, handler: (HttpRequest) -> HttpResponse<String>) {
//        if (handler == null) {
//            throw IllegalArgumentException("chi: attempting to Mount() a nil handler on '$pattern'")
//        }
//
//        // Provide runtime safety for ensuring a pattern isn't mounted on an existing
//        // routing pattern.
//        // This logic would need an equivalent Kotlin implementation.
//        if (tree.findPattern("$pattern*") || tree.findPattern("$pattern/")) {
//            throw IllegalArgumentException("chi: attempting to Mount() a handler on an existing path, '$pattern'")
//        }
//
//        // Assign sub-Router's with the parent not found & method not allowed handler if not specified.
//        val subr = handler as? Mux
//        subr?.let {
//            if (it.notFoundHandler == null && this.notFoundHandler != null) {
//                it.notFound(it.notFoundHandler!!)
//            }
//            if (it.methodNotAllowedHandler == null && this.methodNotAllowedHandler != null) {
//                it.methodNotAllowed(it.methodNotAllowedHandler!!)
//            }
//        }
//
//        // The actual handler for the mount function.
//        // This would be implemented with the specific HTTP library/framework used in Kotlin.
//        val mountHandler: (HttpRequest) -> HttpResponse<String> = { req ->
//            // ... Implementation details ...
//
//            handler(req)
//        }
//
//        if (pattern.isEmpty() || pattern.last() != '/') {
//            handle(pattern, mountHandler)
//            handle("$pattern/", mountHandler)
//        }
//
//        // ... Rest of the mount logic ...
//
//        // (This is a simplification and will need adjustment depending on the HTTP library/framework used.)
//    }
//
//    fun routes(): List<Route> {
//        return tree.routes()
//    }
//
//    fun middlewares(): List<Middleware> {
//        return middlewares.toList()
//    }
//
//
//    fun match(rctx: Context, method: String, path: String): Boolean {
//        val m = methodMap[method] ?: return false
//
//        val (node, _, h) = tree.findRoute(rctx, m, path)
//
//        if (node?.subroutes != null) {
//            rctx.routePath = nextRoutePath(rctx)
//            return node.subroutes.match(rctx, method, rctx.routePath)
//        }
//
//        return h != null
//    }
//
//    fun notFoundHandler(): (HttpRequest) -> HttpResponse {
//        notFoundHandler?.let { return it }
//        return ::httpNotFound
//    }
//
//    fun methodNotAllowedHandler(vararg methodsAllowed: MethodTyp): (HttpRequest) -> HttpResponse {
//        methodNotAllowedHandler?.let { return it }
//        return methodNotAllowedHandler(*methodsAllowed)
//    }
//
//    fun handle(method: MethodTyp, pattern: String, handler: (HttpRequest) -> HttpResponse): Node {
//        if (pattern.isEmpty() || pattern[0] != '/') {
//            throw IllegalArgumentException("chi: routing pattern must begin with '/' in '$pattern'")
//        }
//
//        if (!inline && handler == null) {
//            updateRouteHandler()
//        }
//
//        val h: (HttpRequest) -> HttpResponse
//        if (inline) {
//            h = { req -> chain(middlewares, handler)(req) }
//        } else {
//            h = handler
//        }
//
//        return tree.insertRoute(method, pattern, h)
//    }
//
//    fun routeHTTP(w: HttpResponseWriter, r: HttpRequest) {
//        val rctx = r.context[RouteCtxKey] as Context
//
//        var routePath = rctx.routePath
//        if (routePath.isEmpty()) {
//            routePath = r.url.rawPath.takeIf { it.isNotEmpty() } ?: r.url.path.takeIf { it.isNotEmpty() } ?: "/"
//        }
//
//        val method = methodMap[rctx.routeMethod.takeIf { it.isNotEmpty() } ?: r.method]
//            ?: return methodNotAllowedHandler().invoke(w, r)
//
//        tree.findRoute(rctx, method, routePath).third?.invoke(w, r) ?: if (rctx.methodNotAllowed) {
//            methodNotAllowedHandler(*rctx.methodsAllowed.toTypedArray()).invoke(w, r)
//        } else {
//            notFoundHandler().invoke(w, r)
//        }
//    }
//
//    private fun nextRoutePath(rctx: Context): String {
//        val nx = rctx.routeParams.keys.size - 1
//        if (nx >= 0 && rctx.routeParams.keys[nx] == "*" && rctx.routeParams.values.size > nx) {
//            return "/${rctx.routeParams.values[nx]}"
//        }
//        return "/"
//    }
//
//    private fun updateSubRoutes(fn: (Mux) -> Unit) {
//        tree.routes().forEach { r ->
//            (r.subRoutes as? Mux)?.let(fn)
//        }
//    }
//
//    private fun updateRouteHandler() {
//        handler = chain(middlewares, ::routeHTTP)
//    }
//
//    // This is a helper function to return a 404 error. In a real-world scenario,
//    // you'd likely have a more comprehensive response structure.
//    private fun httpNotFound(req: HttpRequest): HttpResponse {
//        // This is a simplistic placeholder.
//        // Typically, you'd return a proper structured error message.
//        return HttpResponse(404, "Not Found")
//    }
//
//    // This represents an internal method to chain middleware functions.
//    private fun chain(middlewares: List<(HttpRequest) -> HttpResponse>, handler: (HttpRequest) -> HttpResponse): (HttpRequest) -> HttpResponse {
//        // This assumes each middleware calls the next one in the chain.
//        // The final handler is called at the end.
//        return { req ->
//            var currentHandler = handler
//            for (middleware in middlewares.reversed()) {
//                val next = currentHandler
//                currentHandler = { middlewareReq -> middleware(next(middlewareReq)) }
//            }
//            currentHandler(req)
//        }
//    }
//
//    companion object {
//        fun methodNotAllowedHandler(vararg methodsAllowed: MethodTyp): (HttpResponseWriter, HttpRequest) -> Unit {
//            return { w, r ->
//                methodsAllowed.forEach { m ->
//                    w.header.add("Allow", reverseMethodMap[m]!!)
//                }
//                w.writeStatus(405)
//                w.write(null)
//            }
//        }
//    }
//
//    // ... other member methods
//}
//
//// Again, placeholders for `HttpRequest`, `HttpResponseWriter`, `HttpResponse`, `Context`, `Node`, etc. should be filled with appropriate constructs based on the chosen Kotlin web framework.
//
//
//// Note: The above translation is quite basic and doesn't cover a lot of complexities
//// and functionalities from the Go version, like the sync pool, HTTP methods, route patterns, etc.
