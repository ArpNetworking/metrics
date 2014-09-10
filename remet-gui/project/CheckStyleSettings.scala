/**
 * Copyright 2014 Groupon.com
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

import java.security.Permission
import sbt._
import Keys._

// Adapted from https://github.com/ymasory/sbt-code-quality.g8
object CheckstyleSettings {

  object CheckstyleFailedException extends Exception
  val checkstyle = TaskKey[Unit]("checkstyle", "run checkstyle, placing results in target/checkstyle")
  val checkstyleTask = checkstyle <<=
    (streams, baseDirectory, sourceDirectory in Compile, target) map {
      (streams, base, src, target) =>
      import com.puppycrawl.tools.checkstyle.Main.{ main => CsMain }
      val outputDir = (target / "checkstyle").mkdirs
      val outputFile = (target / "checkstyle" / "checkstyle-report.xml").getAbsolutePath
      val inputDir = src.getAbsolutePath
      val buildDir = (base / ".." / "build").toPath.normalize().toAbsolutePath.toFile
      val args = List(
        "-c", (buildDir / "checkstyle.xml").toString,
        "-f", "xml",
        "-r", inputDir,
        "-o", outputFile
      )

      System.setProperty("samedir", buildDir.toString)


      trappingExits {
        CsMain(args.toArray)
      } match {
        case 0 =>
        case _ => throw CheckstyleFailedException
      }
    }

  def trappingExits(thunk: => Unit): Int = {
    val originalSecManager = System.getSecurityManager
    case class NoExitsException(status: Int) extends SecurityException
    System setSecurityManager new SecurityManager() {


      override def checkPermission(perm: Permission): Unit = { }

      override def checkExit(status: Int): Unit = {
        super.checkExit(status)
        throw NoExitsException(status)
      }
    }
    try {
      thunk
      0
    } catch {
      case e:  NoExitsException => e.status
      case _ : Throwable => -1
    } finally {
      System setSecurityManager originalSecManager
    }
  }
}
