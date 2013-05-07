import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  val appName         = "access-log"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    "org.mongodb" %% "casbah" % "2.6.0"
  )

  val main = play.Project(appName, appVersion, appDependencies).settings(
  )

}
