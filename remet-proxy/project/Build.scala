import sbt._
import Keys._
import play.Project._
import com.arpnetworking.typescript.TypeScriptPlugin._

object ApplicationBuild extends Build {

    val appName         = "remet"
    val appVersion      = "1.0-SNAPSHOT"
    val s = Defaults.defaultSettings // ++ Seq(typescriptSettings:_*)

    val appDependencies = Seq(
      javaCore
    )

    val main = play.Project(appName, appVersion, appDependencies, settings = s).settings(
      // Add your own project settings here      
    )

}
