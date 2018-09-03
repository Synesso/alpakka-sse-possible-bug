lazy val root = (project in file("."))
  .settings(
    name := "sse-test",
    organization := "io.github.synesso",
    scalaVersion := "2.12.6",
    libraryDependencies := List(
      "com.lightbend.akka" %% "akka-stream-alpakka-sse" % "0.20",
      "ch.qos.logback" % "logback-classic" % "1.2.3"
    )
  )

