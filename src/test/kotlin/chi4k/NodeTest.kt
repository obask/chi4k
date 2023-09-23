package chi4k

import org.eclipse.jetty.http.HttpField
import org.eclipse.jetty.http.HttpParser.HttpHandler
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer

class TestHttpHandler : HttpHandler {
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

class RouteTests {
    data class TestStruct(
        val r: String,
        val h: TestHttpHandler,
        val k: List<String>,
        val v: List<String>
    )

    @Test
    fun testTree() {
        val hStub = TestHttpHandler()
        val hIndex = TestHttpHandler()
        val hFavicon = TestHttpHandler()

        // ... declare other handlers similarly ...

        val tr = Node()

        tr.insertRoute(MethodTyp.mGET, "/", hIndex)
        tr.insertRoute(MethodTyp.mGET, "/favicon.ico", hFavicon)
        tr.insertRoute(MethodTyp.mGET, "/pages/*", hStub)
        // ... insert other routes similarly ...

        val tests = listOf(
            TestStruct(r = "/", h = hIndex, k = emptyList(), v = emptyList()),
            // ... insert other test data similarly ...
        )

        for ((index, tt) in tests.withIndex()) {
            val rctx = Context()
            val (_, handlers, _) = tr.findRoute(rctx, MethodTyp.mGET, tt.r)
            val handler = handlers?.get(MethodTyp.mGET)

            val paramKeys = rctx.routeParams.keys
            val paramValues = rctx.routeParams.values

            assertEquals(tt.h, handler, "input [$index]: find '${tt.r}' expecting handler")
            assertEquals(tt.k, paramKeys, "input [$index]: find '${tt.r}' expecting paramKeys")
            assertEquals(tt.v, paramValues, "input [$index]: find '${tt.r}' expecting paramValues")
        }
    }
}

// Mock data classes for `Request` and `Response`
data class Request(val path: String)
data class Response(var body: String)
