import mill._
import mill.scalalib._
import ammonite.ops._

object sireum extends ScalaModule {

  final override def scalaVersion = T { "2.13.5" }

  def version = T.sources {
    pwd / "mill-version.txt"
  }

  final override def ivyDeps = T {
    val Seq(pathRef) = version()
    val ver = (read ! pathRef.path).trim
    Agg(ivy"com.lihaoyi::mill-scalajslib:${ver.split(' ').mkString("-")}")
  }
}
