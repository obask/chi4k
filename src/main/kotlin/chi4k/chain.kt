package chi4k

import org.eclipse.jetty.http.HttpField
import org.eclipse.jetty.http.HttpParser.HttpHandler
import java.nio.ByteBuffer


object Chain {
    // Chain returns a Middlewares type from a list of middleware handlers.
    fun chain(vararg middlewares: Middleware): List<Middleware> {
        return middlewares.asList()
    }
}

class ChainHandler(
    val endpoint: HttpHandler,
    private val chain: HttpHandler,
    val middlewares: List<Middleware>
) : HttpHandler {

    override fun content(item: ByteBuffer?): Boolean {
        TODO("Not yet implemented")
    }

    override fun headerComplete(): Boolean {
        TODO("Not yet implemented")
    }

    override fun contentComplete(): Boolean {
        TODO("Not yet implemented")
    }

    override fun messageComplete(): Boolean {
        TODO("Not yet implemented")
    }

    override fun parsedHeader(field: HttpField?) {
        TODO("Not yet implemented")
    }

    override fun earlyEOF() {
        TODO("Not yet implemented")
    }
}

// chain builds a HttpHandler composed of an inline middleware stack and endpoint
// handler in the order they are passed.
private fun chain(middlewares: List<Middleware>, endpoint: HttpHandler): HttpHandler {
    // Return ahead of time if there aren't any middlewares for the chain
    if (middlewares.isEmpty()) {
        return endpoint
    }

    // Wrap the end handler with the middleware chain
    var h: HttpHandler = middlewares.last()(endpoint)
    for (i in middlewares.size - 2 downTo 0) {
        h = middlewares[i](h)
    }

    return h
}
