import com.mojolly.scalate.ScalatePlugin.ScalateKeys._
import com.mojolly.scalate.ScalatePlugin._
import org.scalatra.sbt._
import sbt.Keys._
import sbt._
import com.earldouglas.xsbtwebplugin.PluginKeys.warPostProcess

object SslackBuild extends Build {
  val Organization = "jp.webpay"
  val Name = "SSlack"
  val Version = "0.1.0-SNAPSHOT"
  val ScalaVersion = "2.11.1"
  val ScalatraVersion = "2.3.0"

  val secureKey = settingKey[Boolean]("secure")

  val secureWebXml = <web-app xmlns="http://java.sun.com/xml/ns/javaee"
             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xsi:schemaLocation="http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_3_0.xsd"
             version="3.0">
    <listener>
      <listener-class>org.scalatra.servlet.ScalatraListener</listener-class>
    </listener>

    <security-constraint>
      <web-resource-collection>
        <web-resource-name>A Protected Page</web-resource-name>
        <url-pattern>/*</url-pattern>
      </web-resource-collection>
      <auth-constraint>
        <role-name>user</role-name>
      </auth-constraint>
      <user-data-constraint>
        <transport-guarantee>CONFIDENTIAL</transport-guarantee>
      </user-data-constraint>
    </security-constraint>

    <security-constraint>
      <web-resource-collection>
        <web-resource-name>Webhook endpoint</web-resource-name>
        <url-pattern>/slack-webhook</url-pattern>
      </web-resource-collection>
      <user-data-constraint>
        <transport-guarantee>CONFIDENTIAL</transport-guarantee>
      </user-data-constraint>
    </security-constraint>

    <login-config>
      <auth-method>BASIC</auth-method>
      <realm-name>SSlack Realm</realm-name>
    </login-config>
  </web-app>

  val replaceWebXmlTask: Def.Initialize[Task[() => Unit]] = (target, secureKey) map { (target, isSecure) => { () =>
    import java.io.PrintWriter

    if (isSecure) {
      val webXmlFile = target / "webapp" / "WEB-INF" / "web.xml"
      val p = new PrintWriter(webXmlFile)
      try {
        p.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        p.write(secureWebXml.toString())
      } finally {
        p.close()
      }
    }
  }}

  lazy val project = Project (
    "sslack",
    file("."),
    settings = Defaults.defaultSettings ++ ScalatraPlugin.scalatraWithJRebel ++ scalateSettings ++ Seq(
      organization := Organization,
      name := Name,
      version := Version,
      scalaVersion := ScalaVersion,
      resolvers += Classpaths.typesafeReleases,
      libraryDependencies ++= Seq(
        "org.scalatra" %% "scalatra" % ScalatraVersion,
        "org.scalatra" %% "scalatra-scalate" % ScalatraVersion,
        "org.scalatra" %% "scalatra-specs2" % ScalatraVersion % "test",
        "ch.qos.logback" % "logback-classic" % "1.0.6" % "runtime",
        "org.eclipse.jetty" % "jetty-webapp" % "8.1.8.v20121106" % "container",
        "org.eclipse.jetty.orbit" % "javax.servlet" % "3.0.0.v201112011016" % "container;provided;test" artifacts (Artifact("javax.servlet", "jar", "jar")),

        "org.elasticsearch" % "elasticsearch" % "1.2.2"
      ),
      scalateTemplateConfig in Compile <<= (sourceDirectory in Compile){ base =>
        Seq(
          TemplateConfig(
            base / "webapp" / "WEB-INF" / "templates",
            Seq.empty,  /* default imports should be added here */
            Seq(
              Binding("context", "_root_.org.scalatra.scalate.ScalatraRenderContext", importMembers = true, isImplicit = true)
            ),  /* add extra bindings here */
            Some("templates")
          )
        )
      },
      secureKey := false,
      warPostProcess in Compile <<= replaceWebXmlTask
    )
  )
}
