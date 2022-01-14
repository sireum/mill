import mill._
import mill.scalalib._

object sireum extends ScalaModule {

  final override def scalaVersion = T { "2.13.8" }

  def version = T.sources {
    os.pwd / "mill-version.txt"
  }

  final override def ivyDeps = T {
    val Seq(pathRef) = version()
    val ver = (os.read(pathRef.path)).trim
    Agg(ivy"com.lihaoyi::mill-scalajslib:${ver.split(' ').mkString("-")}")
  }
}
