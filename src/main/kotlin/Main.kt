import org.eclipse.jetty.http.HttpHeader
import org.eclipse.jetty.server.Connector
import org.eclipse.jetty.server.Handler.Abstract
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.Response
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.util.BufferUtil
import org.eclipse.jetty.util.Callback
import org.eclipse.jetty.util.thread.QueuedThreadPool
import java.util.concurrent.Executors

fun main() {
    val threadPool = QueuedThreadPool()
//    threadPool.virtualThreadsExecutor = Executors.newVirtualThreadPerTaskExecutor()

    threadPool.setName("server")
    val server = Server(threadPool)
    val connector = ServerConnector(server)
    connector.port = 8000
    server.addConnector(connector)
    server.setHandler(object : Abstract() {
        override fun handle(request: Request, response: Response, callback: Callback): Boolean {
            response.status = 200
            response.headers.add(HttpHeader.CONTENT_TYPE, "text/plain");
            response.write(true, BufferUtil.toBuffer("Hello World\n"), Callback.NOOP)
            callback.succeeded()
            return true
        }
    })
    server.start()
}
