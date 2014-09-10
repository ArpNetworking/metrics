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
import sbt._
import Keys._
import de.johoop.findbugs4sbt.FindBugs._
import de.johoop.findbugs4sbt.{Priority, Effort}
import play.Play.autoImport._
import PlayKeys._

object ApplicationBuild extends Build {

    val appName         = "remet-proxy"
    val appVersion      = "0.2.1-SNAPSHOT"
    
    val s = findbugsSettings ++ CheckstyleSettings.checkstyleTask

    val appDependencies = Seq(
      javaCore,
      "com.arpnetworking.metrics" % "tsd-core" % "0.2.1.GRPN.5"
    )

    val main = Project(appName, file("."), settings = s).enablePlugins(play.PlayJava).settings(
    version := appVersion,
    libraryDependencies ++= appDependencies,
    scalaVersion := "2.11.1",

      // Compiler warnings as errors
      javacOptions ++= Seq(
        "-Xlint:all",
        "-Werror"
      ),

      // Findbugs
      findbugsEffort := Effort.Maximum,
      findbugsPriority := Priority.Low,
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
