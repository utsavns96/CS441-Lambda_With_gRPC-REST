
name := "RestClient"

version := "0.1"

scalaVersion := "2.13.7"

val logbackVersion = "1.3.0-alpha10"
val sfl4sVersion = "2.0.0-alpha5"
val typesafeConfigVersion = "1.4.1"
val apacheCommonIOVersion = "2.11.0"
val scalacticVersion = "3.2.9"
val akkaVersion = "2.7.0"
val akkaHttpVersion = "10.4.0"

resolvers += Resolver.jcenterRepo

libraryDependencies ++= Seq(
	"ch.qos.logback" % "logback-core" % logbackVersion,
	"ch.qos.logback" % "logback-classic" % logbackVersion,
	"org.slf4j" % "slf4j-api" % sfl4sVersion,
	"com.typesafe" % "config" % typesafeConfigVersion,
	"commons-io" % "commons-io" % apacheCommonIOVersion,
	"org.scalactic" %% "scalactic" % scalacticVersion,
	"org.scalatest" %% "scalatest" % scalacticVersion % Test,
	"org.scalatest" %% "scalatest-featurespec" % scalacticVersion % Test,
	"com.typesafe" % "config" % typesafeConfigVersion,
	"com.typesafe.akka" %% "akka-stream" % akkaVersion,
	"com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
	"com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
	"net.liftweb" %% "lift-json" % "3.5.0"
)
