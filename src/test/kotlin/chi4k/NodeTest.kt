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
        val h: TestHttpHandler?,
        val k: List<String>,
        val v: List<String>
    )

    @Test
    fun testTree() {
        val hStub = TestHttpHandler()
        val hIndex = TestHttpHandler()
        val hFavicon = TestHttpHandler()
        val hArticleList = TestHttpHandler()
        val hArticleNear = TestHttpHandler()
        val hArticleShow = TestHttpHandler()
        val hArticleShowRelated = TestHttpHandler()
        val hArticleShowOpts = TestHttpHandler()
        val hArticleSlug = TestHttpHandler()
        val hArticleByUser = TestHttpHandler()
        val hUserList = TestHttpHandler()
        val hUserShow = TestHttpHandler()
        val hAdminCatchall = TestHttpHandler()
        val hAdminAppShow = TestHttpHandler()
        val hAdminAppShowCatchall = TestHttpHandler()
        val hUserProfile = TestHttpHandler()
        val hUserSuper = TestHttpHandler()
        val hUserAll = TestHttpHandler()
        val hHubView1 = TestHttpHandler()
        val hHubView2 = TestHttpHandler()
        val hHubView3 = TestHttpHandler()


        val tr = Node()

        Node.insertRoute(tr, MethodTyp.mGET, "/", hIndex)
        Node.insertRoute(tr, MethodTyp.mGET, "/favicon.ico", hFavicon)

        Node.insertRoute(tr, MethodTyp.mGET, "/pages/*", hStub)

        Node.insertRoute(tr, MethodTyp.mGET, "/article", hArticleList)
        Node.insertRoute(tr, MethodTyp.mGET, "/article/", hArticleList)

        Node.insertRoute(tr, MethodTyp.mGET, "/article/near", hArticleNear)
        Node.insertRoute(tr, MethodTyp.mGET, "/article/{id}", hStub)
        Node.insertRoute(tr, MethodTyp.mGET, "/article/{id}", hArticleShow)
        Node.insertRoute(tr, MethodTyp.mGET, "/article/{id}", hArticleShow) // duplicate will have no effect
        Node.insertRoute(tr, MethodTyp.mGET, "/article/@{user}", hArticleByUser)

        Node.insertRoute(tr, MethodTyp.mGET, "/article/{sup}/{opts}", hArticleShowOpts)
        Node.insertRoute(tr, MethodTyp.mGET, "/article/{id}/{opts}", hArticleShowOpts) // overwrite above route, latest wins

        Node.insertRoute(tr, MethodTyp.mGET, "/article/{iffd}/edit", hStub)
        Node.insertRoute(tr, MethodTyp.mGET, "/article/{id}//related", hArticleShowRelated)
//        Node.insertRoute(tr, MethodTyp.mGET, "/article/slug/{month}/-/{day}/{year}", hArticleSlug)

        Node.insertRoute(tr, MethodTyp.mGET, "/admin/user", hUserList)
        Node.insertRoute(tr, MethodTyp.mGET, "/admin/user/", hStub) // will get replaced by next route
        Node.insertRoute(tr, MethodTyp.mGET, "/admin/user/", hUserList)

        Node.insertRoute(tr, MethodTyp.mGET, "/admin/user//{id}", hUserShow)
        Node.insertRoute(tr, MethodTyp.mGET, "/admin/user/{id}", hUserShow)

        Node.insertRoute(tr, MethodTyp.mGET, "/admin/apps/{id}", hAdminAppShow)
        Node.insertRoute(tr, MethodTyp.mGET, "/admin/apps/{id}/*", hAdminAppShowCatchall)

        Node.insertRoute(tr, MethodTyp.mGET, "/admin/*", hStub) // catchall segment will get replaced by next route
        Node.insertRoute(tr, MethodTyp.mGET, "/admin/*", hAdminCatchall)

        Node.insertRoute(tr, MethodTyp.mGET, "/users/{userID}/profile", hUserProfile)
        Node.insertRoute(tr, MethodTyp.mGET, "/users/super/*", hUserSuper)
        Node.insertRoute(tr, MethodTyp.mGET, "/users/*", hUserAll)

        Node.insertRoute(tr, MethodTyp.mGET, "/hubs/{hubID}/view", hHubView1)
        Node.insertRoute(tr, MethodTyp.mGET, "/hubs/{hubID}/view/*", hHubView2)

//        val sr = Mux()
//        sr.Get("/users", hHubView3)
//        tr.InsertRoute(mGET, "/hubs/{hubID}/*", sr)
//        tr.InsertRoute(mGET, "/hubs/{hubID}/users", hHubView3)

        val tests = listOf(
            TestStruct("/", hIndex, listOf(), listOf()),
            TestStruct("/favicon.ico", hFavicon, listOf(), listOf()),

            TestStruct("/pages", null, listOf(), listOf()),
            TestStruct("/pages/", hStub, listOf("*"), listOf("")),
            TestStruct("/pages/yes", hStub, listOf("*"), listOf("yes")),

            TestStruct("/article", hArticleList, listOf(), listOf()),
            TestStruct("/article/", hArticleList, listOf(), listOf()),
            TestStruct("/article/near", hArticleNear, listOf(), listOf()),
            TestStruct("/article/neard", hArticleShow, listOf("id"), listOf("neard")),
            TestStruct("/article/123", hArticleShow, listOf("id"), listOf("123")),
            TestStruct("/article/123/456", hArticleShowOpts, listOf("id", "opts"), listOf("123", "456")),
            TestStruct("/article/@peter", hArticleByUser, listOf("user"), listOf("peter")),
            TestStruct("/article/22//related", hArticleShowRelated, listOf("id"), listOf("22")),
            TestStruct("/article/111/edit", hStub, listOf("iffd"), listOf("111")),
//            TestStruct("/article/slug/sept/-/4/2015", hArticleSlug, listOf("month", "day", "year"), listOf("sept", "4", "2015")),
            TestStruct("/article/:id", hArticleShow, listOf("id"), listOf(":id")),

            TestStruct("/admin/user", hUserList, listOf(), listOf()),
            TestStruct("/admin/user/", hUserList, listOf(), listOf()),
            TestStruct("/admin/user/1", hUserShow, listOf("id"), listOf("1")),
            TestStruct("/admin/user//1", hUserShow, listOf("id"), listOf("1")),
            TestStruct("/admin/hi", hAdminCatchall, listOf("*"), listOf("hi")),
            TestStruct("/admin/lots/of/:fun", hAdminCatchall, listOf("*"), listOf("lots/of/:fun")),
            TestStruct("/admin/apps/333", hAdminAppShow, listOf("id"), listOf("333")),
            TestStruct("/admin/apps/333/woot", hAdminAppShowCatchall, listOf("id", "*"), listOf("333", "woot")),

            TestStruct("/hubs/123/view", hHubView1, listOf("hubID"), listOf("123")),
            TestStruct("/hubs/123/view/index.html", hHubView2, listOf("hubID", "*"), listOf("123", "index.html")),
            TestStruct("/hubs/123/users", hHubView3, listOf("hubID"), listOf("123")),

            TestStruct("/users/123/profile", hUserProfile, listOf("userID"), listOf("123")),
            TestStruct("/users/super/123/okay/yes", hUserSuper, listOf("*"), listOf("123/okay/yes")),
            TestStruct("/users/123/okay/yes", hUserAll, listOf("*"), listOf("123/okay/yes")),
        )

        for ((index, tt) in tests.withIndex()) {
            val rctx = Context()
            val (_, handlers, _) = tr.findRoute(rctx, MethodTyp.mGET, tt.r)
            val handler: HttpHandler? = handlers?.get(MethodTyp.mGET)?.handler

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
