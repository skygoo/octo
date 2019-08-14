import sbt._

/**
  * User: Taoz
  * Date: 6/13/2017
  * Time: 9:38 PM
  */
object Dependencies {


  val slickV = "3.2.3"
  val akkaV = "2.5.22"
  val akkaHttpV = "10.1.8"
  val scalaXmlV = "1.1.0"
  val circeVersion = "0.9.3"
  val scalaJsDomV = "0.9.6"

  val akkaSeq = Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaV withSources(),
    "com.typesafe.akka" %% "akka-actor-typed" % akkaV withSources(),
    "com.typesafe.akka" %% "akka-slf4j" % akkaV,
    "com.typesafe.akka" %% "akka-stream" % akkaV,
    "com.typesafe.akka" %% "akka-stream-typed" % akkaV
  )

  val akkaHttpSeq = Seq(
    "com.typesafe.akka" %% "akka-http" % akkaHttpV,
    "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpV
  )

  val circeSeq = Seq(
    "io.circe" %% "circe-core" % circeVersion,
    "io.circe" %% "circe-generic" % circeVersion,
    "io.circe" %% "circe-parser" % circeVersion
  )

  val scalaXml = "org.scala-lang.modules" %% "scala-xml" % "1.0.6"
  val nscalaTime = "com.github.nscala-time" %% "nscala-time" % "2.16.0"
  val hikariCP = "com.zaxxer" % "HikariCP" % "2.6.2"
  val logback = "ch.qos.logback" % "logback-classic" % "1.2.3"
  val codec = "commons-codec" % "commons-codec" % "1.10"
  //  val asynchttpclient = "org.asynchttpclient" % "async-http-client" % "2.0.32"
  val ehcache = "net.sf.ehcache" % "ehcache" % "2.10.4"
  //  val netty = "io.netty" % "netty-all" % "4.1.36.Final"

  // https://mvnrepository.com/artifact/org.jitsi/ice4j
  val ice4j = "org.jitsi" % "ice4j" % "1.0"

  val backendDependencies: Seq[ModuleID] =
    Dependencies.akkaSeq ++
      Dependencies.akkaHttpSeq ++
      Dependencies.circeSeq ++
      Seq(
        Dependencies.scalaXml,
        Dependencies.nscalaTime,
        Dependencies.logback,
        Dependencies.codec,
        //        Dependencies.asynchttpclient,
        Dependencies.ehcache,
        Dependencies.ice4j
      )

  val testLibs = Seq(
    "com.typesafe.akka" %% "akka-testkit" % akkaV % "test",
    "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaV % "test",
    "org.scalatest" %% "scalatest" % "3.0.7" % "test"
  )

}
