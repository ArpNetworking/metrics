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

import com.arpnetworking.sbt.typescript.Import.TypescriptKeys
import com.typesafe.sbt.rjs.Import._
import com.typesafe.sbt.web.Import._
import com.typesafe.sbt.digest.Import._
import com.typesafe.sbt.gzip.Import._
import com.typesafe.sbt.web.js.JS
import com.typesafe.sbt.jse.JsEngineImport.JsEngineKeys
import RjsKeys._
import play.PlayImport._
import sbt._
import Keys._
//import de.johoop.findbugs4sbt.FindBugs._
//import de.johoop.findbugs4sbt.Effort

object ApplicationBuild extends Build {

    val appName         = "remet-gui"
    val appVersion      = "0.3.0"

    //val s = findbugsSettings ++ CheckstyleSettings.checkstyleTask
    val s = CheckstyleSettings.checkstyleTask

    val appDependencies = Seq(
      javaWs,
      "com.arpnetworking.metrics" % "tsd-core" % "0.3.0",
      "com.arpnetworking.metrics" % "metrics-client" % "0.3.0",
      "com.fasterxml.jackson.core" % "jackson-databind" % "2.5.0",
      "com.google.guava" % "guava" % "18.0",
      "com.google.inject" % "guice" % "3.0",
      "com.typesafe.akka" % "akka-cluster_2.11" % "2.3.4",
      "com.typesafe.akka" % "akka-contrib_2.11" % "2.3.4",
      "com.typesafe.akka" % "akka-slf4j_2.11" % "2.3.4",
      "org.elasticsearch" % "elasticsearch" % "1.3.2",
      "org.webjars" % "bean" % "1.0.14",
      "org.webjars" % "bootstrap" % "3.2.0",
      "org.webjars" % "d3js" % "3.4.8",
      "org.webjars" % "durandal" % "2.1.0",
      "org.webjars" % "flotr2" % "d43f8566e8",
      "org.webjars" % "font-awesome" % "4.1.0",
      "org.webjars" % "jQRangeSlider" % "5.7.0",
      "org.webjars" % "jquery" % "2.1.1",
      "org.webjars" % "jquery-ui" % "1.11.1",
      "org.webjars" % "jquery-ui-themes" % "1.11.0",
      "org.webjars" % "knockout" % "3.1.0",
      "org.webjars" % "requirejs-text" % "2.0.10-1",
      "org.webjars" % "typeaheadjs" % "0.10.4-1",
      "org.webjars" % "underscorejs" % "1.6.0-3",
      "com.github.tomakehurst" % "wiremock" % "1.47" % "test"
    )

    val main = Project(appName, file("."), settings = s).enablePlugins(play.PlayJava).settings(

      // Generated unmanaged assests
      unmanagedResourceDirectories in Compile <+= baseDirectory( _ / "app/assets/unmanaged" ),

      // Compiler warnings as errors
      javacOptions ++= Seq(
        "-Xlint:all",
        "-Werror",
        "-Xlint:-path",
        "-Xlint:-try"
      ),

      libraryDependencies ++= appDependencies,

      JsEngineKeys.engineType := JsEngineKeys.EngineType.Node,

      TypescriptKeys.moduleKind := "amd",

      mainConfig := "start_app",
      mainModule := "start_app",
      buildProfile := JS.Object("wrapShim" -> true),
      pipelineStages := Seq(rjs, digest, gzip),
      modules += JS.Object("name" -> "classes/shell"),

      version := appVersion,

      scalaVersion := "2.11.1",
      resolvers += "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository",
      // Needed for play 2.4-M2
      resolvers += "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases"

      // Findbugs
      // TODO(vkoskela): Enable Findbugs in Play [MAI-456]
      /*
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
      */
     )

}
