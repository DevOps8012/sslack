package jp.webpay.sslack

import org.elasticsearch.client.Client
import scala.collection.JavaConversions.mapAsJavaMap
import org.elasticsearch.index.query.{QueryBuilder, QueryStringQueryBuilder, QueryBuilders}
import org.elasticsearch.common.xcontent.XContentFactory
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.search.SearchHit
import org.scalatra.Params

class SSlackServlet(client: Client) extends SslackStack {
  val indexName = "slack"
  val typeName = "message"

  get("/") {
    contentType = "text/html"
    if (params.getOrElse("q", "").length > 0) {
      val result: SearchResponse = client.prepareSearch(indexName).setTypes(typeName).setQuery(buildQuery(params))
        .setFrom(0).setSize(100).setExplain(false)
        .execute().actionGet()
      jade("/index", "hits" -> result.getHits.hits())
    } else {
      jade("/index", "hits" -> Array[SearchHit]())
    }

  }

  post("/slack-webhook") {
    val interested = Seq("token", "timestamp", "channel_name", "user_name", "text")
    val esData = params.filterKeys(interested.contains)
    val result = client.prepareIndex(indexName, typeName)
      .setSource(mapAsJavaMap(esData).asInstanceOf[java.util.Map[String, AnyRef]])
      .execute()
      .actionGet()

    log("Indexed as %s %s %s:%d".format(result.getIndex, result.getType, result.getId, result.getVersion))

    "ok"
  }

  override def init(): Unit = {
    super.init()

    if (isIndexExist) {
      log(s"Index $indexName/$typeName already exists")
    } else {
      createIndex()
    }
  }

  private def isIndexExist: Boolean = {
    val result = client.admin().indices()
      .prepareExists(indexName)
      .execute().actionGet()
    result.isExists
  }

  private def createIndex() {
    val properties = XContentFactory.jsonBuilder().prettyPrint().startObject()
      .startObject("properties")
        .startObject("token")
          .field("type", "string").field("store", false)
        .endObject()
        .startObject("timestamp")
          .field("type", "float").field("index", "no")
        .endObject()
        .startObject("channel_name")
          .field("type", "string").field("store", true).field("index", "not_analyzed")
        .endObject()
        .startObject("user_name")
          .field("type", "string").field("store", true).field("index", "not_analyzed")
        .endObject()
        .startObject("text")
          .field("type", "string").field("store", true).field("index", "analyzed").field("analyzer", "kuromoji")
        .endObject()
      .endObject()
    .endObject()
    val analyzerSetting =XContentFactory.jsonBuilder().prettyPrint().startObject()
      .startObject("analysis")
        .startObject("tokenizer")
          .startObject("kuromoji").field("type", "kuromoji_tokenizer").endObject()
        .endObject()
        .startObject("analyzer")
          .startObject("analyzer")
            .field("type", "custom").field("tokenizer", "kuromoji")
          .endObject()
        .endObject()
      .endObject()
    .endObject()


    val result = client.admin().indices().prepareCreate(indexName)
      .addMapping(typeName, properties)
      .setSettings(analyzerSetting)
      .execute().actionGet()
    if (!result.isAcknowledged)
      throw new RuntimeException(s"Could not create index for $indexName/$typeName")
    log(s"Index $indexName/$typeName is created with mapping and analyzer")
  }

  private def buildQuery(params: Params): QueryBuilder = {
    import org.elasticsearch.index.query.QueryBuilders._
    var qb = boolQuery()

    val channel = params.getOrElse("channel", "")
    val user = params.getOrElse("user", "")
    val q = params.getOrElse("q", "")

    if (channel.length > 0)
      qb = qb.must(termQuery("channel_name", channel))

    if (user.length > 0)
      qb = qb.must(termQuery("user_name", user))

    if (q.length > 0)
      qb = qb.must(queryString(q).field("text").defaultOperator(QueryStringQueryBuilder.Operator.AND))

    qb
  }
}
