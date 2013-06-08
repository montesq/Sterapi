import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "Sterapi"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    "org.reactivemongo" %% "play2-reactivemongo" % "0.9",
    "be.objectify" %% "deadbolt-scala" % "2.1-RC2"
  )


  val main = play.Project(appName, appVersion, appDependencies).settings(
    resolvers += Resolver.url("Objectify Play Repository", url("http://schaloner.github.com/releases/"))(Resolver.ivyStylePatterns),
    resolvers += Resolver.url("Objectify Play Repository - snapshots", url("http://schaloner.github.com/snapshots/"))(Resolver.ivyStylePatterns),
    resolvers += "typesafe" at "http://repo.typesafe.com/typesafe/repo"
  )

}
