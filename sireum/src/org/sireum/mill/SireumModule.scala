/*
 Copyright (c) 2018, Robby, Kansas State University
 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions are met:

 1. Redistributions of source code must retain the above copyright notice, this
    list of conditions and the following disclaimer.
 2. Redistributions in binary form must reproduce the above copyright notice,
    this list of conditions and the following disclaimer in the documentation
    and/or other materials provided with the distribution.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.sireum.mill

import mill._
import mill.scalalib._
import mill.scalajslib._
import mill.scalalib.publish._
import ammonite.ops._

trait SireumModule extends mill.scalalib.JavaModule {

  final def scalaVer = SireumModule.scalaVersion

  final def javacOpts =
    Seq("-source", "1.8", "-target", "1.8", "-encoding", "utf8")

  final def scalacOpts =
    Seq(
      "-target:jvm-1.8",
      "-deprecation",
      "-Yrangepos",
      "-Ydelambdafy:method",
      "-feature",
      "-unchecked",
      "-Xfatal-warnings"
    )

  def platformSegment: String

  def additionalSourceDirs = T.sources()

  def additionalTestSourceDirs = T.sources()

  override def repositories = super.repositories ++ Seq(
    coursier.maven.MavenRepository("https://jitpack.io")
  )
}

object SireumModule {

  val publishVersion: String = {
    val v = System.getenv("VERSION")
    if (v != null) v else "SNAPSHOT"
  }

  object Developers {

    val robby = Developer("robby-phd", "Robby", "https://github.com/robby-phd")

    val jason = Developer("jasonbelt", "Robby", "https://github.com/jasonbelt")

    val hari = Developer("thari", "Robby", "https://github.com/thari")

  }

  lazy val (properties, propertiesFile) = {
    import java.io._
    val propFile = pwd / "versions.properties"
    println(s"Loading Sireum dependency versions from $propFile ...")
    val ps = new java.util.Properties
    val f = propFile.toIO
    val fr = new FileReader(f)
    ps.load(fr)
    fr.close()
    (ps, f)
  }

  private def property(key: String): String = {
    val value = properties.getProperty(key)
    if (value == null) {
      throw new Error(
        s"Need to supply property '$key' in '${propertiesFile.getCanonicalPath}'.")
    }
    value
  }

  lazy val scalaVersion = property("org.sireum.version.scala")

  lazy val scalacPluginVersion = property("org.sireum.version.scalac-plugin")

  lazy val scalaJsVersion = property("org.sireum.version.scalajs")

  lazy val scalaTestVersion = property("org.sireum.version.scalatest")

  lazy val spireVersion = property("org.sireum.version.spire")

  lazy val scalaMetaVersion = property("org.sireum.version.scalameta")

  lazy val diffVersion = property("org.sireum.version.diff")

  lazy val scalaJsDomVersion = property("org.sireum.version.scalajsdom")

  lazy val scalaJsJQueryVersion = property("org.sireum.version.scalajsjquery")

  lazy val scalaTagsVersion = property("org.sireum.version.scalatags")

  lazy val parboiled2Version = property("org.sireum.version.parboiled2")

  lazy val java8CompatVersion = property("org.sireum.version.java8compat")

  lazy val ammoniteOpsVersion = property("org.sireum.version.ammonite-ops")

  lazy val utestVersion = property("org.sireum.version.utest")

  lazy val nuProcessVersion = property("org.sireum.version.nuprocess")

  sealed trait Project

  object Project {

    trait Jvm extends ScalaModule with SireumModule { outer =>

      final override def scalaVersion = T { scalaVer }

      final override def javacOptions = T { javacOpts }

      final override def scalacOptions = T { scalacOpts }

      def platformSegment: String

      def deps: Seq[Jvm]

      def testIvyDeps: Agg[Dep]

      def testScalacPluginIvyDeps: Agg[Dep]

      def testFrameworks: Seq[String]

      private def defaultSourceDirs = T.sources(
        millSourcePath / "src" / "main" / "scala",
        millSourcePath / "src" / "main" / "java"
      )

      final override def sources = T.sources(
        defaultSourceDirs() ++ additionalSourceDirs()
      )

      def tests: Tests

      trait Tests extends super.Tests {

        final override def millSourcePath =
          super.millSourcePath / up / up / platformSegment / "src" / "test"

        final override def ivyDeps = T { outer.testIvyDeps.distinct }

        final override def scalacPluginIvyDeps = T {
          outer.testScalacPluginIvyDeps
        }

        final override def testFrameworks = T { outer.testFrameworks.distinct }

        private def defaultSourceDirs = T.sources(
          millSourcePath / "scala",
          millSourcePath / "java"
        )

        final override def sources = T.sources(
          defaultSourceDirs() ++ additionalTestSourceDirs()
        )
      }
    }

    trait Js extends ScalaJSModule with SireumModule { outer =>

      final override def scalaVersion = T { scalaVer }

      final override def javacOptions = T { javacOpts }

      final override def scalacOptions = T { scalacOpts }

      def deps: Seq[Js]

      def testIvyDeps: Agg[Dep]

      def testScalacPluginIvyDeps: Agg[Dep]

      def testFrameworks: Seq[String]

      private def defaultSourceDirs = T.sources(
        millSourcePath / "src" / "main" / "scala",
        millSourcePath / up / "shared" / "src" / "main" / "scala"
      )

      final override def sources = T.sources(
        defaultSourceDirs() ++ additionalSourceDirs()
      )

      final override def nodeJSConfig = T {
        val size = System.getenv("NODEJS_MAX_HEAP")
        val config = super.nodeJSConfig()
        if (size != null)
          config.copy(args = config.args ++ List(s"--max-old-space-size=$size"))
        else
          config
      }

      def tests: Tests

      trait Tests extends super.Tests {

        final override def millSourcePath =
          super.millSourcePath / up / up / "js" / "src" / "test"

        final override def ivyDeps = T { outer.testIvyDeps.distinct }

        final override def scalacPluginIvyDeps = T {
          (outer.testScalacPluginIvyDeps ++ super.scalacPluginIvyDeps()).distinct
        }

        final override def testFrameworks = T { outer.testFrameworks.distinct }

        private def defaultSourceDirs = T.sources(
          millSourcePath / "scala",
          millSourcePath / up / up / up / "shared" / "src" / "test" / "scala"
        )

        final override def sources = T.sources(
          defaultSourceDirs() ++ additionalTestSourceDirs()
        )

        final override def nodeJSConfig = T { outer.nodeJSConfig() }
      }

    }

    trait Publish extends PublishModule {

      def description: String
      def subUrl: String
      def developers: Seq[Developer]

      override def publishVersion: T[String] = T { SireumModule.publishVersion }

      final def m2 = T {
        val pa = publishArtifacts()
        val ad = pa.meta.group.split("\\.").foldLeft(T.ctx().dest)((a, b) => a / b) / pa.meta.id / pa.meta.version
        mkdir(ad)
        for ((f, n) <- pa.payload) cp(f.path, ad / n)
      }

      override def pomSettings = PomSettings(
        description = description,
        organization = "org.sireum",
        url = s"https://github.com/sireum/$subUrl",
        licenses = Seq(
          License("BSD 2-Clause \"Simplified\" License",
            "BSD-2-Clause",
            s"https://github.com/sireum/$subUrl/blob/master/license.txt",
            isOsiApproved = true,
            isFsfLibre = false,
            "repo")),
        versionControl = VersionControl.github("sireum", subUrl),
        developers = developers
      )
    }

    trait JvmPublish extends Jvm with Publish {

      def deps: Seq[JvmPublish]

    }

    trait JsPublish extends Js with Publish {

      def deps: Seq[JsPublish]

    }

    trait CrossJvmJs extends mill.Module {

      def shared: Jvm

      def jvm: Jvm

      def js: Js

      def deps: Seq[CrossJvmJs]

      def jvmDeps: Seq[Jvm]

      def jsDeps: Seq[Js]

      def ivyDeps: Agg[Dep]

      def scalacPluginIvyDeps: Agg[Dep]

      def testIvyDeps: Agg[Dep]

      def jvmTestIvyDeps: Agg[Dep]

      def jsTestIvyDeps: Agg[Dep]

      def testScalacPluginIvyDeps: Agg[Dep]

      def jvmTestFrameworks: Seq[String]

      def jsTestFrameworks: Seq[String]
    }

    trait CrossJvmJsPublish extends CrossJvmJs {

      def shared: JvmPublish

      def jvm: JvmPublish

      def js: JsPublish

      def deps: Seq[CrossJvmJsPublish]

      def jvmDeps: Seq[JvmPublish]

      def jsDeps: Seq[JsPublish]
    }

  }

  trait Shared extends Project.Jvm {

    final override def platformSegment = "shared"

  }

  trait Jvm extends Project.Jvm {

    final override def platformSegment = "jvm"

  }

  trait Js extends Project.Js {

    final override def platformSegment = "js"

    final override def scalaJSVersion = T { scalaJsVersion }
  }

  trait SharedPublish extends Project.JvmPublish {

    final override def platformSegment = "shared"

  }

  trait JvmPublish extends Project.JvmPublish {

    final override def platformSegment = "jvm"

  }

  trait JsPublish extends Project.JsPublish {

    final override def platformSegment = "js"

    final override def scalaJSVersion = T { scalaJsVersion }
  }

  trait JvmOnly extends Jvm { outer =>

    final override def millSourcePath = super.millSourcePath / platformSegment

    def crossDeps: Seq[CrossJvmJs]

    override def moduleDeps = mDeps

    final def mDeps =
      ((for (dep <- crossDeps)
        yield Seq(dep.shared, dep.jvm)).flatten ++ deps).distinct

    object tests extends Tests {

      final override def moduleDeps =
        (Seq(outer) ++ (for (dep <- mDeps) yield Seq(dep, dep.tests)).flatten).distinct

    }
  }

  trait JsOnly extends Js { outer =>

    final override def millSourcePath = super.millSourcePath / platformSegment

    def crossDeps: Seq[CrossJvmJs]

    final override def moduleDeps = mDeps

    final def mDeps = ((for (dep <- crossDeps) yield dep.js) ++ deps).distinct

    object tests extends Tests {

      final override def moduleDeps =
        (Seq(outer) ++ (for (dep <- mDeps) yield Seq(dep, dep.tests)).flatten).distinct

    }
  }

  trait CrossJvmJs extends Project.CrossJvmJs { outer =>

    object shared extends Shared {

      final override def ivyDeps = T { outer.ivyDeps.distinct }

      final override def scalacPluginIvyDeps = T { outer.scalacPluginIvyDeps.distinct }

      final override def testIvyDeps = (outer.testIvyDeps ++ outer.jvmTestIvyDeps).distinct

      final override def testScalacPluginIvyDeps = outer.testScalacPluginIvyDeps.distinct

      final override def testFrameworks = outer.jvmTestFrameworks.distinct

      final override def deps = Seq()

      final override def moduleDeps = mDeps

      final def mDeps = (for (dep <- outer.deps) yield dep.shared).distinct

      object tests extends Tests {

        final override def moduleDeps =
          (Seq(shared) ++ (for (dep <- mDeps) yield Seq(dep, dep.tests)).flatten).distinct

      }
    }

    object jvm extends Jvm {

      final override def ivyDeps = T { outer.ivyDeps.distinct }

      final override def scalacPluginIvyDeps = T { outer.scalacPluginIvyDeps.distinct }

      final override def testIvyDeps = (outer.testIvyDeps ++ outer.jvmTestIvyDeps).distinct

      final override def testScalacPluginIvyDeps = outer.testScalacPluginIvyDeps.distinct

      final override def testFrameworks = outer.jvmTestFrameworks.distinct

      final override def deps = Seq()

      override def moduleDeps = mDeps

      final def mDeps =
        (Seq(shared) ++ (for (dep <- outer.deps)
          yield Seq(dep.shared, dep.jvm)).flatten ++ jvmDeps).distinct

      object tests extends Tests {

        final override def moduleDeps =
          (Seq(jvm) ++ (for (dep <- mDeps) yield Seq(dep, dep.tests)).flatten).distinct

      }
    }

    object js extends Js {

      final override def ivyDeps = T { outer.ivyDeps }

      final override def scalacPluginIvyDeps = T {
        (outer.scalacPluginIvyDeps ++ super.scalacPluginIvyDeps()).distinct
      }

      final override def testIvyDeps = (outer.testIvyDeps ++ outer.jsTestIvyDeps).distinct

      final override def testScalacPluginIvyDeps = outer.testScalacPluginIvyDeps.distinct

      final override def testFrameworks = outer.jsTestFrameworks.distinct

      final override def deps = Seq()

      final override def moduleDeps = mDeps

      final def mDeps = ((for (dep <- outer.deps) yield dep.js) ++ jsDeps).distinct

      object tests extends Tests {

        final override def moduleDeps =
          (Seq(js) ++ (for (dep <- mDeps) yield Seq(dep, dep.tests)).flatten).distinct

      }
    }

  }

  trait CrossJvmJsPublish extends Project.CrossJvmJsPublish { outer =>

    def developers: Seq[Developer]

    def publishVersion: String = SireumModule.publishVersion

    def description: String

    def subUrl: String

    def artifactNameOpt: Option[String] = None

    def sharedArtifactNameOpt: Option[String] = None

    def jvmArtifactNameOpt: Option[String] = None

    def jsArtifactNameOpt: Option[String] = None

    object shared extends SharedPublish {

      final override def subUrl = outer.subUrl

      final override def description: String = outer.description

      final override def ivyDeps = T { outer.ivyDeps.distinct }

      final override def scalacPluginIvyDeps = T { outer.scalacPluginIvyDeps.distinct }

      final override def testIvyDeps = (outer.testIvyDeps ++ outer.jvmTestIvyDeps).distinct

      final override def testScalacPluginIvyDeps = outer.testScalacPluginIvyDeps.distinct

      final override def testFrameworks = outer.jvmTestFrameworks.distinct

      final override def developers = outer.developers.distinct

      final override def publishVersion = outer.publishVersion

      final override def deps = Seq()

      final override def moduleDeps = mDeps

      final override def artifactName: T[String] = T {
        sharedArtifactNameOpt match {
          case Some(name) => artifactNameCheck(name)
          case _ =>
            artifactNameOpt match {
              case Some(name) => artifactNameCheck(name)
              case _ => super.artifactName()
            }
        }
      }

      final def artifactNameCheck(name: String): String = {
        assert(name != null, s"Cannot publish ${millModuleSegments.parts.mkString(".")}")
        name
      }

      final def mDeps = (for (dep <- outer.deps) yield dep.shared).distinct

      object tests extends Tests {

        final override def moduleDeps =
          (Seq(shared) ++ (for (dep <- mDeps) yield Seq(dep, dep.tests)).flatten).distinct

      }

    }

    object jvm extends JvmPublish {

      final override def subUrl = outer.subUrl

      final override def description: String = outer.description

      final override def ivyDeps = T { outer.ivyDeps.distinct }

      final override def scalacPluginIvyDeps = T {
        (outer.scalacPluginIvyDeps ++ super.scalacPluginIvyDeps()).distinct
      }

      final override def testIvyDeps = (outer.testIvyDeps ++ outer.jvmTestIvyDeps).distinct

      final override def testScalacPluginIvyDeps = outer.testScalacPluginIvyDeps.distinct

      final override def testFrameworks = outer.jvmTestFrameworks.distinct

      final override def developers = outer.developers.distinct

      final override def publishVersion = outer.publishVersion

      final override def deps = Seq()

      final override def artifactName: T[String] = T {
        jvmArtifactNameOpt match {
          case Some(name) => artifactNameCheck(name)
          case _ =>
            artifactNameOpt match {
              case Some(name) => artifactNameCheck(name)
              case _ => super.artifactName()
            }
        }
      }

      override def moduleDeps = mDeps

      final def artifactNameCheck(name: String): String = {
        assert(name != null, s"Cannot publish ${millModuleSegments.parts.mkString(".")}")
        name
      }

      final def mDeps =
        (Seq(shared) ++ (for (dep <- outer.deps)
          yield Seq(dep.shared, dep.jvm)).flatten ++ jvmDeps).distinct

      object tests extends Tests {

        final override def moduleDeps =
          (Seq(jvm) ++ (for (dep <- mDeps) yield Seq(dep, dep.tests)).flatten).distinct

      }

    }

    object js extends JsPublish {

      final override def subUrl = outer.subUrl

      final override def description: String = outer.description

      final override def ivyDeps = T { outer.ivyDeps.distinct }

      final override def scalacPluginIvyDeps = T {
        (outer.scalacPluginIvyDeps ++ super.scalacPluginIvyDeps()).distinct
      }

      final override def testIvyDeps = (outer.testIvyDeps ++ outer.jsTestIvyDeps).distinct

      final override def testScalacPluginIvyDeps = outer.testScalacPluginIvyDeps.distinct

      final override def testFrameworks = outer.jsTestFrameworks.distinct

      final override def developers = outer.developers.distinct

      final override def publishVersion = outer.publishVersion

      final override def deps = Seq()

      final override def moduleDeps = mDeps

      final override def artifactName: T[String] = T {
        jsArtifactNameOpt match {
          case Some(name) => artifactNameCheck(name)
          case _ =>
            artifactNameOpt match {
              case Some(name) => artifactNameCheck(name)
              case _ => super.artifactName()
            }
        }
      }

      final def artifactNameCheck(name: String): String = {
        assert(name != null, s"Cannot publish ${millModuleSegments.parts.mkString(".")}")
        name
      }

      final def mDeps = ((for (dep <- outer.deps) yield dep.js) ++ jsDeps).distinct

      object tests extends Tests {

        final override def moduleDeps =
          (Seq(js) ++ (for (dep <- mDeps) yield Seq(dep, dep.tests)).flatten).distinct

      }
    }

  }

  implicit class AggDistinct[T](val agg: Agg[T]) extends AnyVal {
    def distinct: Agg[T] = {
      Agg.empty ++ agg.toSeq.distinct
    }
  }

}
