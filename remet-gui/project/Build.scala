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

import com.typesafe.sbt.SbtAspectj._
import com.typesafe.sbt.SbtAspectj.AspectjKeys._
import com.typesafe.sbt.jse.JsEngineImport.JsEngineKeys
import de.johoop.findbugs4sbt.FindBugs._
import de.johoop.findbugs4sbt.{Effort, Priority, ReportType}
import net.virtualvoid.sbt.graph.Plugin.graphSettings
import play.sbt.routes.RoutesKeys.routesGenerator
import play.routes.compiler.InjectedRoutesGenerator
import sbt._
import Keys._

object ApplicationBuild extends Build {

    val appName = "remet-gui"
    val appVersion = "0.4.1"
    val akkaVersion = "2.3.14"
    val jacksonVersion = "2.6.2"

    val s = findbugsSettings ++ CheckstyleSettings.checkstyleTask ++ aspectjSettings ++ graphSettings

    val appDependencies = Seq(
      "com.arpnetworking.build" % "build-resources" % "1.0.2",
      "com.arpnetworking.logback" % "logback-steno" % "1.9.3",
      "com.arpnetworking.metrics" % "metrics-client" % "0.3.7",
      "com.arpnetworking.metrics" %% "metrics-portal" % "0.4.3",
      "com.arpnetworking.metrics" %% "metrics-portal" % "0.4.3" classifier "assets",
      "com.arpnetworking.metrics" % "tsd-core" % "0.3.6",
      "com.fasterxml.jackson.core" % "jackson-databind" % jacksonVersion,
      "com.fasterxml.jackson.datatype" % "jackson-datatype-guava" % jacksonVersion,
      "com.fasterxml.jackson.datatype" % "jackson-datatype-jdk7" % jacksonVersion,
      "com.fasterxml.jackson.datatype" % "jackson-datatype-jdk8" % jacksonVersion,
      "com.fasterxml.jackson.datatype" % "jackson-datatype-joda" % jacksonVersion,
      "com.google.code.findbugs" % "annotations" % "3.0.0",
      "com.google.guava" % "guava" % "18.0",
      "net.sf.oval" % "oval" % "1.82",
      "com.github.tomakehurst" % "wiremock" % "1.57" % "test"
    )

    val main = Project(appName, file("."), settings = s).enablePlugins(play.sbt.PlayJava).settings(

      // Generated unmanaged assests
      unmanagedResourceDirectories in Compile <+= baseDirectory( _ / "app/assets/unmanaged" ),

      // Extract build resources
      compile in Compile <<= (compile in Compile).dependsOn(Def.task {
        val jar = (update in Compile).value
          .select(configurationFilter("compile"))
          .filter(_.name.startsWith("build-resources"))
          .head
        IO.unzip(jar, (target in Compile).value / "build-resources")
        Seq.empty[File]
      }),

      // Compiler warnings as errors
      javacOptions ++= Seq(
        "-Xlint:all",
        "-Werror",
        "-Xlint:-path",
        "-Xlint:-try"
      ),

      JsEngineKeys.engineType := JsEngineKeys.EngineType.Node,
      routesGenerator := InjectedRoutesGenerator,

      version := appVersion,

      scalaVersion := "2.11.6",
      resolvers += Resolver.mavenLocal,

      libraryDependencies ++= appDependencies,

      // AspectJ
      binaries in Aspectj <++= update map { report =>
        report.matching(moduleFilter(organization = "com.arpnetworking.logback", name = "logback-steno"))
      },
      inputs in Aspectj <+= compiledClasses,
      products in Compile <<= products in Aspectj,
      products in Runtime <<= products in Compile,

      // Findbugs
      findbugsReportType := Some(ReportType.Html),
      findbugsReportPath := Some(target.value / "findbugs" / "findbugs.html"),
      findbugsPriority := Priority.Low,
      findbugsEffort := Effort.Maximum,
      findbugsExcludeFilters := Some(
        <FindBugsFilter>
          <Match>
            <Class name="~views\.html\..*"/>
          </Match>
          <Match>
            <Class name="~models.ebean.*"/>
          </Match>
          <Match>
            <Class name="~router.Routes.*"/>
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
