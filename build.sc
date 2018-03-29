import mill._
import mill.scalalib._

object sireum extends ScalaModule {

  final override def scalaVersion = T { "2.12.4" }

  final override def ivyDeps = T { Agg(ivy"com.lihaoyi::mill-scalajslib:0.1.7-8-b913c6") }
}
