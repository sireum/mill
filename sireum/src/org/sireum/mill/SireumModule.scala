/*
 Copyright (c) 2019, Robby, Kansas State University
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
import os._

trait SireumModule extends mill.scalalib.JavaModule {

  final def scalaVer = SireumModule.scalaVersion

  final def javacOpts =
    Seq("-source", "1.8", "-target", "1.8", "-encoding", "utf8", "-XDignore.symbol.file", " -Xlint:-options",
      "-Xlint:deprecation", "-proc:none")

  final def scalacOpts =
    Seq(
      "-release",
      "8",
      "-deprecation",
      "-Yrangepos",
      "-Ydelambdafy:method",
      "-feature",
      "-unchecked",
      "-Xfatal-warnings",
      "-language:postfixOps"
    )

  def platformSegment: String

  def additionalSourceDirs = T.sources()

  def additionalTestSourceDirs = T.sources()

  override def repositories = super.repositories ++ SireumModule.repositories
}

object SireumModule {

  val date: String = new java.text.SimpleDateFormat("yyyyMMdd").format(new java.util.Date)

  val isSourceDep: Boolean = "false" != System.getenv("SIREUM_SOURCE_BUILD")

  val repositories: Seq[coursier.Repository] =
    Seq(coursier.maven.MavenRepository("https://jitpack.io")) ++
      ((sys.env.get("GITHUB_USERNAME"), sys.env.get("GITHUB_TOKEN")) match {
        case (Some(username), Some(token)) =>
          Seq(coursier.maven.MavenRepository("https://maven.pkg.github.com/sireum",
              authentication = Some(coursier.core.Authentication(username, token))))
        case _ => Seq()
      })

  object Developers {

    val robby = Developer("robby-phd", "Robby", "https://github.com/robby-phd")

    val jason = Developer("jasonbelt", "Jason Belt", "https://github.com/jasonbelt")

    val hari = Developer("thari", "Hariharan Thiagarajan", "https://github.com/thari")

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

  def publishVersion: String =
    try proc('git, 'log, "-1", "--date=format:%Y%m%d", "--pretty=format:4.%cd.%h", pwd).call().out.lines.head.trim catch {
      case _: Throwable => "SNAPSHOT"
    }

  def jitpackVersion: String =
    try proc('git, 'log, "-1", "--format=%H", pwd).call().out.lines.head.trim.substring(0, 10) catch {
      case _: Throwable => "SNAPSHOT"
    }

  def ghLatestCommit(owner: String, repo: String, branch: String): String = {
    val out = proc('git, "ls-remote", s"https://github.com/$owner/$repo.git", pwd).call().out
    for (line <- out.lines if line.contains(s"refs/heads/$branch"))
      return line.substring(0, 10)
    throw new RuntimeException(s"Could not determine latest commit for https://github.com/$owner/$repo.git branch $branch!")
  }

  def jpLatest(isCross: Boolean, owner: String, repo: String, lib: String = "",
               branchOrHash: Either[String, String] = Left("master")): Dep = {
    val hash = branchOrHash match {
      case Left(branch) => SireumModule.ghLatestCommit(owner, repo, branch)
      case Right(h) => h
    }
    val l = if ("" == lib) repo else lib
    owner match {
      case "sireum" => if (isCross) ivy"org.sireum.$repo::$l::$hash" else ivy"org.sireum.$repo::$l:$hash"
      case _ => if (isCross) ivy"com.github.$owner.$repo::$l::$hash" else ivy"com.github.$owner.$repo::$l:$hash"
    }
  }

  final def jitPack(owner: String, repo: String, lib: String, hash: String = jitpackVersion): Unit = {
    val dirFile = java.nio.file.Files.createTempDirectory(null).toFile.getAbsoluteFile
    dirFile.deleteOnExit()
    val dir = Path(dirFile)
    write(dir / "build.sc",
      s"""import mill._, scalalib._, org.sireum.mill.SireumModule._
         |object jptest extends ScalaModule {
         |  def scalaVersion = "${SireumModule.scalaVersion}"
         |  def ivyDeps = Agg(
         |    jpLatest(isCross = false, "$owner", "$repo", "$lib", Right("$hash"))
         |  )
         |  def repositories = super.repositories ++ Seq(
         |    coursier.maven.MavenRepository("https://jitpack.io/")
         |  )
         |}""".stripMargin)
    copy(Path(propertiesFile.getAbsoluteFile), dir / propertiesFile.getName)
    if (scala.util.Properties.isWin)
      proc("cmd", "/c", new java.io.File(getClass.getProtectionDomain.getCodeSource.getLocation.toURI).getCanonicalPath,
        "jptest.compile", dir).call()
    else
      proc("sh", new java.io.File(getClass.getProtectionDomain.getCodeSource.getLocation.toURI).getCanonicalPath,
        "jptest.compile", dir).call()
  }

  private def property(key: String): String = {
    val value = properties.getProperty(key.replace(':', '%'))
    if (value == null) {
      throw new Error(
        s"Need to supply property '$key' in '${propertiesFile.getCanonicalPath}'.")
    }
    value
  }

  lazy val scalaVersion = property("org.scala-lang:scala-library:")

  lazy val scalaTestVersion = property("org.scalatest::scalatest::")

  lazy val asmVersion = property("org.ow2.asm:asm:")

  lazy val diffVersion = property("com.sksamuel.diff:diff:")

  lazy val githubVersion = property("org.kohsuke:github-api:")

  lazy val java8CompatVersion = property("org.scala-lang.modules::scala-java8-compat:")

  lazy val javaFxVersion = property("org.openjfx:javafx:")

  lazy val nuProcessVersion = property("com.zaxxer:nuprocess:")

  lazy val osLibVersion = property("com.lihaoyi::os-lib:")

  lazy val parCollectionVersion = property("org.scala-lang.modules::scala-parallel-collections:")

  lazy val scalacPluginVersion = property("org.sireum::scalac-plugin:")

  lazy val scalaJsDomVersion = property("org.scala-js::scalajs-dom::")

  lazy val scalaJsJQueryVersion = property("be.doraene::scalajs-jquery::")

  lazy val scalaMetaVersion = property("org.scalameta::scalameta::")

  lazy val scalaTagsVersion = property("com.lihaoyi::scalatags::")

  lazy val scalaJsVersion = property("org.scala-js:::scalajs-compiler:")

  lazy val antlr3Version = property("org.antlr:antlr-runtime:")

  lazy val antlr4Version = property("org.antlr:antlr4-runtime:")

  lazy val automatonVersion = property("org.sireum:automaton:")

  lazy val presentasiJfxVersion = property("org.sireum:presentasi-jfx:")

  lazy val scalafmtVersion = property("org.scalameta::scalafmt-cli:")

  lazy val apacheCompressVersion = property("org.apache.commons:commons-compress:")

  sealed trait Project

  object Project {

    trait Jvm extends ScalaModule with SireumModule { outer =>

      final override def scalaVersion = T { scalaVer }

      final override def javacOptions = T { javacOpts }

      final override def scalacOptions = T { scalacOpts }

      final override def docJar = T {
        val outDir = T.ctx().dest

        val javadocDir = outDir / 'javadoc
        os.makeDir.all(javadocDir)

        mill.eval.Result.Success(mill.modules.Jvm.createJar(Agg(javadocDir))(outDir))
      }

      def platformSegment: String

      def deps: Seq[Jvm]

      def testDeps: Seq[JavaModule] = Seq()

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

        override def repositories = super.repositories ++ SireumModule.repositories
      }
    }

    trait Js extends ScalaJSModule with SireumModule { outer =>

      final override def scalaVersion = T { scalaVer }

      final override def javacOptions = T { javacOpts }

      final override def scalacOptions = T { scalacOpts }

      final override def docJar = T {
        val outDir = T.ctx().dest

        val javadocDir = outDir / 'javadoc
        os.makeDir.all(javadocDir)

        mill.eval.Result.Success(mill.modules.Jvm.createJar(Agg(javadocDir))(outDir))
      }

      def deps: Seq[Js]

      def testDeps: Seq[ScalaJSModule] = Seq()

      def testIvyDeps: Agg[Dep]

      def testScalacPluginIvyDeps: Agg[Dep]

      def testFrameworks: Seq[String]

      private def defaultSourceDirs = T.sources(
        millSourcePath / "src" / "main" / "scala"
      )

      final override def sources = T.sources(
        defaultSourceDirs() ++ additionalSourceDirs()
      )

      final override def jsEnvConfig = T {
        import mill.scalajslib.api.JsEnvConfig
        val size = System.getenv("NODEJS_MAX_HEAP")
        val config = super.jsEnvConfig()
        if (size != null)
          config match {
            case config: JsEnvConfig.NodeJs => config.copy(args = config.args ++ List(s"--max-old-space-size=$size"))
            case config: JsEnvConfig.JsDom => config
            case config: JsEnvConfig.Phantom => config
          }
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
          millSourcePath / "scala"
        )

        final override def sources = T.sources(
          defaultSourceDirs() ++ additionalTestSourceDirs()
        )

        final override def jsEnvConfig = T { outer.jsEnvConfig() }

        override def repositories = super.repositories ++ SireumModule.repositories
      }

    }

    trait Publish extends PublishModule {

      def description: String
      def subUrl: String
      def developers: Seq[Developer]

      override def publishVersion: T[String] = T { SireumModule.publishVersion }

      final def m2 = T {
        val pa = publishArtifacts()
        val group: Seq[String] = pa.meta.group.split("\\.") match {
          case Array("org", "sireum", _, rest @ _*) => Seq("org", "sireum") ++ rest
          case g => g.toSeq
        }
        val ad = group.foldLeft(T.ctx().dest)((a, b) => a / b) / pa.meta.id / pa.meta.version
        makeDir(ad)
        for ((f, n) <- pa.payload) copy(f.path, ad / n)
      }

      override def pomSettings = PomSettings(
        description = description,
        organization = "org.sireum",
        url = s"https://github.com/sireum/$subUrl",
        licenses = Seq(
          License("BSD 2-Clause \"Simplified\" License",
            "BSD-2-Clause",
            s"https://github.com/sireum/kekinian/blob/master/license.txt",
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

      def testDeps: Seq[JavaModule] = Seq()

      def jvmTestDeps: Seq[JavaModule] = Seq()

      def jsTestDeps: Seq[ScalaJSModule] = Seq()

      def ivyDeps: Agg[Dep]

      def jvmIvyDeps: Agg[Dep] = Agg.empty

      def jsIvyDeps: Agg[Dep] = Agg.empty

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

  trait JvmPublishOnly extends JvmPublish { outer =>

    override def millSourcePath = super.millSourcePath / platformSegment

    def crossDeps: Seq[Project.CrossJvmJsPublish]

    final override def moduleDeps = mDeps

    final def mDeps =
      ((for (dep <- crossDeps)
        yield Seq(dep.shared, dep.jvm)).flatten ++ deps).distinct

    trait Tests extends super.Tests {

      final override def moduleDeps =
        (Seq(outer) ++ (for (dep <- mDeps) yield Seq(dep, dep.tests) ++ dep.testDeps).flatten ++
          testDeps ++ outer.testDeps).distinct

    }

  }

  trait JsPublish extends Project.JsPublish {

    final override def platformSegment = "js"

    final override def scalaJSVersion = T { scalaJsVersion }
  }

  trait JsPublishOnly extends JsPublish { outer =>

    override def millSourcePath = super.millSourcePath / platformSegment

    def crossDeps: Seq[Project.CrossJvmJsPublish]

    final override def moduleDeps = mDeps

    final def mDeps = ((for (dep <- crossDeps) yield dep.js) ++ deps).distinct

    trait Tests extends super.Tests {

      final override def moduleDeps =
        (Seq(outer) ++ (for (dep <- mDeps) yield Seq(dep, dep.tests) ++ dep.testDeps).flatten ++
          testDeps ++ outer.testDeps).distinct

    }
  }

  trait JvmOnly extends Jvm { outer =>

    override def millSourcePath = super.millSourcePath / platformSegment

    def crossDeps: Seq[Project.CrossJvmJs]

    override def moduleDeps = mDeps

    final def mDeps =
      ((for (dep <- crossDeps)
        yield Seq(dep.shared, dep.jvm)).flatten ++ deps).distinct

    object tests extends Tests {

      final override def moduleDeps =
        (Seq(outer) ++ (for (dep <- mDeps) yield Seq(dep, dep.tests) ++ dep.testDeps).flatten ++
          testDeps ++ outer.testDeps).distinct

    }
  }

  trait JsOnly extends Js { outer =>

    override def millSourcePath = super.millSourcePath / platformSegment

    def crossDeps: Seq[Project.CrossJvmJs]

    final override def moduleDeps = mDeps

    final def mDeps = ((for (dep <- crossDeps) yield dep.js) ++ deps).distinct

    object tests extends Tests {

      final override def moduleDeps =
        (Seq(outer) ++ (for (dep <- mDeps) yield Seq(dep, dep.tests) ++ dep.testDeps).flatten ++
          testDeps ++ outer.testDeps).distinct

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
          (Seq(shared) ++ (for (dep <- mDeps) yield Seq(dep, dep.tests) ++ dep.testDeps).flatten ++
            testDeps ++ outer.testDeps).distinct

      }
    }

    object jvm extends Jvm {

      final override def ivyDeps = T { (outer.ivyDeps ++ outer.jvmIvyDeps).distinct }

      final override def scalacPluginIvyDeps = T { outer.scalacPluginIvyDeps.distinct }

      final override def testIvyDeps = (outer.testIvyDeps ++ outer.jvmIvyDeps ++ outer.jvmTestIvyDeps).distinct

      final override def testScalacPluginIvyDeps = outer.testScalacPluginIvyDeps.distinct

      final override def testFrameworks = outer.jvmTestFrameworks.distinct

      final override def deps = Seq()

      override def moduleDeps = mDeps

      final def mDeps =
        (Seq(shared) ++ (for (dep <- outer.deps)
          yield Seq(dep.shared, dep.jvm)).flatten ++ jvmDeps).distinct

      object tests extends Tests {

        final override def moduleDeps =
          (Seq(jvm) ++ (for (dep <- mDeps) yield Seq(dep, dep.tests) ++ dep.testDeps).flatten ++
            testDeps ++ jvmTestDeps).distinct

      }
    }

    object js extends Js {

      final override def ivyDeps = T { (outer.ivyDeps ++ outer.jsIvyDeps).distinct }

      final override def scalacPluginIvyDeps = T {
        (outer.scalacPluginIvyDeps ++ super.scalacPluginIvyDeps()).distinct
      }

      final override def testIvyDeps = (outer.testIvyDeps ++ outer.jvmIvyDeps ++ outer.jsTestIvyDeps).distinct

      final override def testScalacPluginIvyDeps = outer.testScalacPluginIvyDeps.distinct

      final override def testFrameworks = outer.jsTestFrameworks.distinct

      final override def deps = Seq()

      final override def moduleDeps = mDeps

      final def mDeps = ((for (dep <- outer.deps) yield dep.js) ++ jsDeps).distinct

      def sharedSources = T.sources(
        millSourcePath / up / "shared" / "src" / "main" / "scala"
      )

      override def allSourceFiles = T {
        def isHiddenFile(path: os.Path) = path.last.startsWith(".")
        for {
          root <- allSources() ++ sharedSources()
          if os.exists(root.path)
          path <- (if (os.isDir(root.path)) os.walk(root.path) else Seq(root.path))
          if os.isFile(path) && ((path.ext == "scala" || path.ext == "java") && !isHiddenFile(path))
        } yield PathRef(path)
      }

      object tests extends Tests {

        final override def moduleDeps =
          (Seq(js) ++ (for (dep <- mDeps) yield Seq(dep, dep.tests) ++ dep.testDeps).flatten ++
            testDeps ++ jsTestDeps).distinct

        def sharedSources = T.sources(
          millSourcePath / up / up / up / "shared" / "src" / "test" / "scala"
        )

        override def allSourceFiles = T {
          def isHiddenFile(path: os.Path) = path.last.startsWith(".")
          for {
            root <- allSources() ++ sharedSources()
            if os.exists(root.path)
            path <- (if (os.isDir(root.path)) os.walk(root.path) else Seq(root.path))
            if os.isFile(path) && ((path.ext == "scala" || path.ext == "java") && !isHiddenFile(path))
          } yield PathRef(path)
        }

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
          (Seq(shared) ++ (for (dep <- mDeps) yield Seq(dep, dep.tests) ++ dep.testDeps).flatten ++
            testDeps ++ outer.testDeps).distinct

      }

    }

    object jvm extends JvmPublish {

      final override def subUrl = outer.subUrl

      final override def description: String = outer.description

      final override def ivyDeps = T { (outer.ivyDeps ++ outer.jvmIvyDeps).distinct }

      final override def scalacPluginIvyDeps = T {
        (outer.scalacPluginIvyDeps ++ super.scalacPluginIvyDeps()).distinct
      }

      final override def testIvyDeps = (outer.testIvyDeps ++ outer.jvmIvyDeps ++ outer.jvmTestIvyDeps).distinct

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

      final def crossDeps: Seq[Project.CrossJvmJsPublish] = Seq()

      final def mDeps =
        (Seq(shared) ++ (for (dep <- outer.deps)
          yield Seq(dep.shared, dep.jvm)).flatten ++ jvmDeps).distinct

      object tests extends Tests {

        final override def moduleDeps =
          (Seq(jvm) ++ (for (dep <- mDeps) yield Seq(dep, dep.tests) ++ dep.testDeps).flatten ++
            testDeps ++ outer.jvmTestDeps).distinct

      }

    }

    object js extends JsPublish {

      final override def subUrl = outer.subUrl

      final override def description: String = outer.description

      final override def ivyDeps = T { (outer.ivyDeps ++ outer.jsIvyDeps).distinct }

      final override def scalacPluginIvyDeps = T {
        (outer.scalacPluginIvyDeps ++ super.scalacPluginIvyDeps()).distinct
      }

      final override def testIvyDeps = (outer.testIvyDeps ++ outer.jsIvyDeps ++ outer.jsTestIvyDeps).distinct

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

      final def crossDeps: Seq[Project.CrossJvmJsPublish] = Seq()

      final def mDeps = ((for (dep <- outer.deps) yield dep.js) ++ jsDeps).distinct

      def sharedSources = T.sources(
        millSourcePath / up / "shared" / "src" / "main" / "scala"
      )

      override def allSourceFiles = T {
        def isHiddenFile(path: os.Path) = path.last.startsWith(".")
        for {
          root <- allSources() ++ sharedSources()
          if os.exists(root.path)
          path <- (if (os.isDir(root.path)) os.walk(root.path) else Seq(root.path))
          if os.isFile(path) && ((path.ext == "scala" || path.ext == "java") && !isHiddenFile(path))
        } yield PathRef(path)
      }

      object tests extends Tests {

        def sharedSources = T.sources(
          millSourcePath / up / up / up / "shared" / "src" / "test" / "scala"
        )

        override def allSourceFiles = T {
          def isHiddenFile(path: os.Path) = path.last.startsWith(".")
          for {
            root <- allSources() ++ sharedSources()
            if os.exists(root.path)
            path <- (if (os.isDir(root.path)) os.walk(root.path) else Seq(root.path))
            if os.isFile(path) && ((path.ext == "scala" || path.ext == "java") && !isHiddenFile(path))
          } yield PathRef(path)
        }

        final override def moduleDeps =
          (Seq(js) ++ (for (dep <- mDeps) yield Seq(dep, dep.tests) ++ dep.testDeps).flatten ++
            testDeps ++ jsTestDeps).distinct

      }
    }

  }

  trait CrossJvmJsJitPack extends CrossJvmJsPublish {

    def artifactName: String

    final override def artifactNameOpt: Option[String] = Some(artifactName)

    final override def sharedArtifactNameOpt: Option[String] = Some(s"$artifactName-shared")

    final override def jvmArtifactNameOpt: Option[String] = None

    final override def jsArtifactNameOpt: Option[String] = None
  }

  trait CrossSharedJsJitPack extends CrossJvmJsPublish {

    def artifactName: String

    final override def artifactNameOpt: Option[String] = Some(artifactName)

    final override def sharedArtifactNameOpt: Option[String] = None

    final override def jvmArtifactNameOpt: Option[String] = Some(null) // should not publish jvm

    final override def jsArtifactNameOpt: Option[String] = None
  }

  implicit class AggDistinct[T](val agg: Agg[T]) extends AnyVal {
    def distinct: Agg[T] = {
      Agg.empty ++ agg.toSeq.distinct
    }
  }

}
