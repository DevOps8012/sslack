package jp.webpay.sslack

import com.google.gson.{JsonParser, JsonElement}
import io.searchbox.client.JestClient
import io.searchbox.core.{SearchResult, Search, Index}
import io.searchbox.indices.{IndicesExists, CreateIndex}
import io.searchbox.indices.mapping.PutMapping

import scala.collection.JavaConversions.mapAsJavaMap
import scala.collection.JavaConversions.asScalaBuffer
import org.scalatra.Params

import scala.collection.mutable
import scala.util.parsing.json.{JSONArray, JSONType, JSONObject}

class SSlackServlet(client: JestClient) extends SslackStack {
  val indexName = "slack"
  val typeName = "message"

  get("/") {
    contentType = "text/html"
    if (params.getOrElse("q", "").length > 0) {
      val search = new Search.Builder(buildQuery(params).toString())
        .addIndex(indexName).addType(typeName)
        .setParameter("from", 0).setParameter("size", 100).setParameter("explain", false).build()
      val result: SearchResult = client.execute(search)
      if (!result.isSucceeded) {
        throw new RuntimeException("Search failed: " + result.getErrorMessage)
      }
      val hits = asScalaBuffer(result.getHits(classOf[Message]))
      val sources = hits.map(_.source)
      jade("/index", "hits" -> sources)
    } else {
      jade("/index", "hits" -> mutable.Buffer[Message]())
    }

  }

  post("/slack-webhook") {
    val interested = Seq("token", "timestamp", "channel_name", "user_name", "text")
    val esData = params.filterKeys(interested.contains)
    val index = new Index.Builder(mapAsJavaMap(esData).asInstanceOf[java.util.Map[String, AnyRef]])
      .index(indexName).`type`(typeName).build()
    val result = client.execute(index)

    log("Indexed as %s %s %s:%s".format(result.getValue("_index"), result.getValue("_type"), result.getValue("_id"), result.getValue("_version")))

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
    val result = client.execute(new IndicesExists.Builder(indexName).build())
    result.isSucceeded
  }

  private def createIndex() {
    val analyzerSetting = JSONObject(Map(
      "analysis" -> JSONObject(Map(
        "tokenizer" -> JSONObject(Map("kuromoji" -> JSONObject(Map("type" -> "kuromoji_tokenizer")))),
        "analyzer" -> JSONObject(Map("analyzer" -> JSONObject(Map(
          "type" -> "custom",
          "tokenizer" -> "kuromoji"))))
      ))))

    val mappingSetting = JSONObject(Map(
      typeName -> JSONObject(Map("properties" -> JSONObject(Map(
        "token" -> JSONObject(Map("type" -> "string", "store" -> false)),
        "timestamp" -> JSONObject(Map("type" -> "float", "index" -> "no")),
        "channel_name" -> JSONObject(Map("type" -> "string", "store" -> true, "index" -> "not_analyzed")),
        "user_name" -> JSONObject(Map("type" -> "string", "store" -> true, "index" -> "not_analyzed")),
        "text" -> JSONObject(Map("type" -> "string", "store" -> true, "index" -> "analyzed", "analyzer" -> "kuromoji"))
      ))))))

    val indexResult = client.execute(new CreateIndex.Builder(indexName).settings(scalaJsonToGsonElement(analyzerSetting)).build())
    if (!indexResult.isSucceeded) {
      throw new RuntimeException(s"Could not create index $indexName: ${indexResult.getErrorMessage}")
    }
    val mappingResult = client.execute(new PutMapping.Builder(indexName, typeName, scalaJsonToGsonElement(mappingSetting)).build())
    if (!mappingResult.isSucceeded) {
      throw new RuntimeException(s"Could not add mapping $indexName/$typeName: ${mappingResult.getErrorMessage}")
    }
    log(s"Index $indexName/$typeName is created with mapping and analyzer")
  }

  private def buildQuery(params: Params): JSONType = {
    val mustBuffer = mutable.Buffer[JSONObject]()
    def addParam(nameInParam: String, nameInDocument: String) = {
      params.get(nameInParam).flatMap {
        case "" => None
        case s => Some(s)
      }.foreach { value =>
        mustBuffer += JSONObject(Map("term" -> JSONObject(Map(nameInDocument -> value))))
      }
    }

    mustBuffer += JSONObject(Map("query_string" -> JSONObject(Map(
      "query" -> params.getOrElse("q", ""),
      "default_field" -> "text",
      "default_operator" -> "AND"
    ))))
    addParam("channel", "channel_name")
    addParam("user", "user_name")

    JSONObject(Map(
      "explain" -> false,
      "query" -> JSONObject(Map("bool" -> JSONObject(Map(
        "must" -> JSONArray(mustBuffer.toList)))))))
  }

  private def scalaJsonToGsonElement(source: JSONObject): JsonElement = {
    val parser = new JsonParser()
    parser.parse(source.toString())
  }

}
