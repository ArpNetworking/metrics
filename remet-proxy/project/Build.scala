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
import com.typesafe.sbt.SbtAspectj._
import com.typesafe.sbt.SbtAspectj.AspectjKeys._
import de.johoop.findbugs4sbt.FindBugs._
import de.johoop.findbugs4sbt.{Priority, Effort, ReportType}
import play.Play.autoImport._
import PlayKeys._

object ApplicationBuild extends Build {

    val appName = "remet-proxy"
    val appVersion = "0.3.5"
    val jacksonVersion = "2.6.2"

    val s = findbugsSettings ++ CheckstyleSettings.checkstyleTask ++ aspectjSettings

    val appDependencies = Seq(
      "com.arpnetworking.logback" % "logback-steno" % "1.9.3",
      "com.arpnetworking.metrics.extras" % "jvm-extra" % "0.3.5",
      "com.arpnetworking.metrics" % "metrics-client" % "0.3.7",
      "com.arpnetworking.metrics" % "tsd-core" % "0.3.4.GRPN.27",
      "com.fasterxml.jackson.datatype" % "jackson-datatype-guava" % jacksonVersion,
      "com.fasterxml.jackson.datatype" % "jackson-datatype-jdk7" % jacksonVersion,
      "com.fasterxml.jackson.datatype" % "jackson-datatype-jdk8" % jacksonVersion,
      "com.fasterxml.jackson.datatype" % "jackson-datatype-joda" % jacksonVersion,
      "com.google.code.findbugs" % "annotations" % "3.0.0",
      "com.google.guava" % "guava" % "18.0",
      "com.google.inject" % "guice" % "4.0"
    )

    val main = Project(appName, file("."), settings = s).enablePlugins(play.PlayJava).settings(
      version := appVersion,
      scalaVersion := "2.11.1",
      resolvers += "Local Maven Repository" at "file://"+Path.userHome.absolutePath+"/.m2/repository",
      libraryDependencies ++= appDependencies,

      // Generated unmanaged assests
      unmanagedResourceDirectories in Compile <+= baseDirectory( _ / "app/assets/unmanaged" ),

      // Compiler warnings as errors
      javacOptions ++= Seq(
        "-Xlint:all",
        "-Werror",
        "-Xlint:-try"
      ),

      // AspectJ
      binaries in Aspectj <++= update map { report =>
        report.matching(moduleFilter(organization = "com.arpnetworking.logback", name = "logback-steno"))
      },
      inputs in Aspectj <+= compiledClasses,
      products in Compile <<= products in Aspectj,
      products in Runtime <<= products in Compile,

      // Findbugs
      findbugsReportType := Some(ReportType.Html),
      findbugsReportPath := Some(crossTarget.value / "findbugs.html"),
      findbugsPriority := Priority.Low,
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
            <Class name="~_routes_.*"/>
          </Match>
          <Match>
            <Class name="~controllers\.routes.*"/>
          </Match>
          <Match>
            <Class name="~controllers\.Reverse.*"/>
          </Match>
        </FindBugsFilter>
      )
    )
}
