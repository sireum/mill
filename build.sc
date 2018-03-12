import mill._
import mill.scalalib._

object sireum extends ScalaModule {

  def scalaVersion = T { "2.12.4" }

  def ivyDeps = T { Agg(ivy"com.lihaoyi::mill-scalajslib:0.1.4-23-55ee6e") }
}
