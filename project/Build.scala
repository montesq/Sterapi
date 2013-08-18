import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "Sterapi"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    "org.reactivemongo" %% "play2-reactivemongo" % "0.9"
  )


  val main = play.Project(appName, appVersion, appDependencies).settings(
    resolvers += "typesafe" at "http://repo.typesafe.com/typesafe/repo"
  )

}
