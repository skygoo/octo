name := "octo"

version := "0.1"

scalaVersion := "2.12.8"

//resolvers += Resolver.mavenLocal

resolvers += "Jisti ORG Snapshots" at "https://github.com/jitsi/jitsi-maven-repository/raw/master/snapshots/"

//resolvers += "Jisti ORG Releases" at "https://github.com/jitsi/jitsi-maven-repository/raw/master/releases/"

val projectMainClass = "org.seekloud.octo.Boot"

lazy val root = (project in file("."))
  .settings(
    mainClass in reStart := Some(projectMainClass),
    javaOptions in reStart += "-Xmx2g"
  )
  .settings(
    name := "octo"
  )
  .settings(
    //pack
    // If you need to specify main classes manually, use packSettings and packMain
    //packSettings,
    // [Optional] Creating `hello` command that calls org.mydomain.Hello#main(Array[String])
    packMain := Map("octo" -> projectMainClass),
    packJvmOpts := Map("octo" -> Seq("-Xmx512m", "-Xms128m", "-XX:+HeapDumpOnOutOfMemoryError")),
    packExtraClasspath := Map("octo" -> Seq("."))
  )
  .settings(
    libraryDependencies ++= Dependencies.backendDependencies,
    libraryDependencies ++= Dependencies.testLibs,
    libraryDependencies ++= Dependencies.jitsiLibs,
    libraryDependencies ++= Dependencies.bcpLibs
  )