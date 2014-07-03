/**
 * Copyright 2014 Brandon Arp
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.typesafe.sbt.rjs.Import._
import com.typesafe.sbt.web.Import._
import com.typesafe.sbt.digest.Import._
import com.typesafe.sbt.gzip.Import._
import com.typesafe.sbt.web.js.JS
import sbt._
import com.arpnetworking.sbt.typescript.Import.TypescriptKeys
import com.typesafe.sbt.jse.JsEngineImport.JsEngineKeys
import RjsKeys._
import sbt._
import Keys._
import de.johoop.findbugs4sbt.FindBugs._
import de.johoop.findbugs4sbt.Effort
import de.johoop.findbugs4sbt.ReportType
import scala.Some
import scala.Some

object ApplicationBuild extends Build {

    val appName         = "remet-gui"
    val appVersion      = "0.2.1-SNAPSHOT"

    val s = findbugsSettings ++ CheckstyleSettings.checkstyleTask

    val appDependencies = Seq(
      "org.webjars" % "bootstrap" % "3.2.0",
      "org.webjars" % "d3js" % "3.4.8",
      "org.webjars" % "durandal" % "2.0.1-1",
      "org.webjars" % "flotr2" % "d43f8566e8",
      "org.webjars" % "font-awesome" % "4.1.0",
      "org.webjars" % "jquery" % "2.1.1",
      "org.webjars" % "jquery-ui" % "1.11.0",
      "org.webjars" % "jquery-ui-themes" % "1.10.3",
      "org.webjars" % "knockout" % "3.1.0",
      "org.webjars" % "requirejs-text" % "2.0.10-1",
      "org.webjars" % "rickshaw" % "1.4.5",
      "org.webjars" % "underscorejs" % "1.6.0-3"
    )

    val main = Project(appName, file("."), settings = s).enablePlugins(play.PlayJava).settings(

      // Compiler warnings as errors
      javacOptions ++= Seq(
        "-Xlint:all",
        "-Werror"
      ),

      libraryDependencies ++= appDependencies,

      JsEngineKeys.engineType := JsEngineKeys.EngineType.Node,

      TypescriptKeys.moduleKind := "amd",

      mainConfig := "start_app",
      mainModule := "start_app",
      buildProfile := JS.Object("wrapShim" -> true),
      pipelineStages := Seq(rjs, digest, gzip),
      modules += JS.Object("name" -> "classes/GraphViewModel"),

      version := appVersion,

      scalaVersion := "2.11.1",

      // Findbugs
      findbugsEffort := Effort.Maximum,
      findbugsExcludeFilters := Some(
        <FindBugsFilter>
          <Match>
            <Class name="~views\.html\..*"/>
          </Match>
          <Match>
            <Class name="~Routes.*"/>
          </Match>
          <Match>
            <Class name="~controllers\.routes.*"/>
          </Match>
        </FindBugsFilter>
      )
     )

}
