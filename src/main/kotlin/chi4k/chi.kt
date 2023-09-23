package chi4k

import org.eclipse.jetty.http.HttpParser.HttpHandler


// Define a type alias for Middleware for convenience.
typealias Middleware = (HttpHandler) -> HttpHandler

object Chi {
    // NewRouter returns a new Mux object that implements the Router interface.
//    fun newRouter(): Mux {
//        return Mux()
//    }
}

interface Router {
    val routes: Routes

    // Use appends one or more middlewares onto the Router stack.
    fun use(vararg middlewares: Middleware)

    // With adds inline middlewares for an endpoint handler.
    fun with(vararg middlewares: Middleware): Router

    // Group adds a new inline-Router along the current routing path,
    // with a fresh middleware stack for the inline-Router.
    fun group(fn: (Router) -> Unit): Router

    // Route mounts a sub-Router along a `pattern` string.
    fun route(pattern: String, fn: (Router) -> Unit): Router

    // Mount attaches another HttpHandler along ./pattern/*
    fun mount(pattern: String, handler: HttpHandler)

    // Handle and HandleFunc adds routes for `pattern` that matches
    // all HTTP methods.
    fun handle(pattern: String, handler: HttpHandler)
    fun handleFunc(pattern: String, handler: (HttpHandler) -> Unit)

    // Method and MethodFunc adds routes for `pattern` that matches
    // the `method` HTTP method.
    fun method(method: String, pattern: String, handler: HttpHandler)
    fun methodFunc(method: String, pattern: String, handler: (HttpHandler) -> Unit)

    // HTTP-method routing along `pattern`
    fun connect(pattern: String, handler: (HttpHandler) -> Unit)
    fun delete(pattern: String, handler: (HttpHandler) -> Unit)
    fun get(pattern: String, handler: (HttpHandler) -> Unit)
    fun head(pattern: String, handler: (HttpHandler) -> Unit)
    fun options(pattern: String, handler: (HttpHandler) -> Unit)
    fun patch(pattern: String, handler: (HttpHandler) -> Unit)
    fun post(pattern: String, handler: (HttpHandler) -> Unit)
    fun put(pattern: String, handler: (HttpHandler) -> Unit)
    fun trace(pattern: String, handler: (HttpHandler) -> Unit)

    // NotFound defines a handler to respond whenever a route could
    // not be found.
    fun notFound(handler: (HttpHandler) -> Unit)

    // MethodNotAllowed defines a handler to respond whenever a method is
    // not allowed.
    fun methodNotAllowed(handler: (HttpHandler) -> Unit)
}

interface Routes {
    // Routes returns the routing tree in an easily traversable structure.
    fun routes(): List<Route>

    // Middlewares returns the list of middlewares in use by the router.
    fun middlewares(): List<Middleware>

    // Match searches the routing tree for a handler that matches
    // the method/path - similar to routing a http request, but without
    // executing the handler thereafter.
    fun match(rctx: Context, method: String, path: String): Boolean
}

