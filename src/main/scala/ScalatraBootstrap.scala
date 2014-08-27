import javax.servlet.ServletContext

import jp.webpay.sslack._
import org.elasticsearch.client.Client
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.scalatra._

class ScalatraBootstrap extends LifeCycle {
  var client: Client = null

  override def init(context: ServletContext) {
    client = new TransportClient().addTransportAddress(new InetSocketTransportAddress("localhost", 9300))

    context.mount(new SSlackServlet(client), "/*")
  }

  override def destroy(context: ServletContext): Unit = {
    if (client != null) {
      client.close()
    }
  }
}
