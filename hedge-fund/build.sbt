name := "hedge-fund"

version := "1.0.0-SNAPSHOT"

scalaVersion := "2.11.8"

val akkaGroup = "com.typesafe.akka"
val akkaVersion = "2.4.1"
val sprayGroup = "io.spray"
val sprayVersion = "1.3.3"
val sprayJsonVersion = "1.3.2"
val scalaTestVersion = "2.2.4"

//ivyScala := ivyScala.value map { _.copy(overrideScalaVersion = true) }

resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
resolvers += Resolver.sonatypeRepo("public")
resolvers += "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies ++= List("spray-client") map {c => sprayGroup %% c % sprayVersion}
libraryDependencies ++= List("spray-json") map {c => sprayGroup %% c % sprayJsonVersion}

libraryDependencies ++= Seq(
  akkaGroup %% "akka-actor" % akkaVersion,
  akkaGroup %% "akka-testkit" % akkaVersion % "test",
  akkaGroup %% "akka-slf4j" % akkaVersion,
  "com.typesafe" % "config" % "1.3.0",
  "com.github.nscala-time" %% "nscala-time" % "2.18.0",
  "ch.qos.logback" % "logback-classic" % "1.0.0" % "runtime",
  "org.scalatest" %% "scalatest" % scalaTestVersion % "test"
)