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
//import de.johoop.findbugs4sbt.FindBugs._
//import de.johoop.findbugs4sbt.{Priority, Effort}
import play.Play.autoImport._
import PlayKeys._

object ApplicationBuild extends Build {

    val appName         = "remet-proxy"
    val appVersion      = "0.3.1"

    //val s = findbugsSettings ++ CheckstyleSettings.checkstyleTask
    val s = CheckstyleSettings.checkstyleTask

    val appDependencies = Seq(
      // Play 2.4 uses version 1.1.1 and although some transitive dependencies
      // may use a newer version we force the known working version.
      "ch.qos.logback" % "logback-classic" % "1.1.1" force(),
      "ch.qos.logback" % "logback-core" % "1.1.1" force(),
      "com.arpnetworking.logback" % "logback-steno" % "1.3.2",
      "com.arpnetworking.metrics" % "metrics-client" % "0.3.1",
      "com.arpnetworking.metrics" % "tsd-core" % "0.3.1",
      "com.fasterxml.jackson.datatype" % "jackson-datatype-joda" % "2.5.0",
      "com.google.guava" % "guava" % "18.0",
      "com.google.inject" % "guice" % "3.0"
    )

    val main = Project(appName, file("."), settings = s).enablePlugins(play.PlayJava).settings(
      version := appVersion,
      libraryDependencies ++= appDependencies,
      scalaVersion := "2.11.1",
      resolvers += "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository",
      // Needed for play 2.4-M2
      resolvers += "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases",

      // Generated unmanaged assests
      unmanagedResourceDirectories in Compile <+= baseDirectory( _ / "app/assets/unmanaged" ),

      // Compiler warnings as errors
      javacOptions ++= Seq(
        "-Xlint:all",
        "-Werror",
        "-Xlint:-try"
      )

      // Findbugs
      // TODO(vkoskela): Enable Findbugs in Play [MAI-456]
      /*
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
      */
    )

}
