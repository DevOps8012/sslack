import javax.servlet.ServletContext

import io.searchbox.client.config.HttpClientConfig
import io.searchbox.client.{JestClient, JestClientFactory}
import jp.webpay.sslack.SSlackServlet
import org.scalatra._

class ScalatraBootstrap extends LifeCycle {
  var client: JestClient = null

  override def init(context: ServletContext) {
    val elasticsearchHttpUrl = sys.env.getOrElse("SEARCHBOX_SSL_URL", "http://localhost:9200")
    val clientConfig = new HttpClientConfig.Builder(elasticsearchHttpUrl).multiThreaded(true).build()
    val factory = new JestClientFactory()
    factory.setHttpClientConfig(clientConfig)
    client = factory.getObject

    context.mount(new SSlackServlet(client), "/*")
  }

  override def destroy(context: ServletContext): Unit = {
    if (client != null) {
      client.shutdownClient()
    }
  }
}
