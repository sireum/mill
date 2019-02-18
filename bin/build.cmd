::#! 2> /dev/null                                   #
@ 2>/dev/null # 2>nul & echo off & goto BOF         #
SCRIPT_HOME=$(cd "$(dirname "$0")" && pwd)          #
if [ ! -f ${SCRIPT_HOME}/sireum.jar ]; then         #
  ${SCRIPT_HOME}/init.sh                            #
fi                                                  #
exec ${SCRIPT_HOME}/sireum slang run -s "$0" "$@"   #
:BOF
if not exist %~dp0sireum.jar call %~dp0init.bat
%~dp0sireum.bat slang run -s "%0" %*
exit /B %errorlevel%
::!#
// #Sireum
import org.sireum._


def usage(): Unit = {
  println("Mill /build")
  println("Usage: [ dev ]")
}


var isDev = F
if (Os.cliArgs.size > 2) {
  usage()
  Os.exit(0)
} else if (Os.cliArgs.size == 2) {
  if (Os.cliArgs(1) == "dev") {
    isDev = T
  } else {
    usage()
    eprintln(s"Unrecognized command: ${Os.cliArgs(1)}")
    Os.exit(-1)
  }
}


def checkDeps(): Unit = {
  var missing = ISZ[String]()
  if (!Os.proc(ISZ("7z")).run().ok) {
    missing = missing :+ "7z"
  }
  if (isDev && !Os.proc(ISZ("git", "--version")).run().ok) {
    missing = missing :+ "git"
  }
  if (missing.nonEmpty) {
    eprintln(st"Missing requirement(s): ${(missing, ", ")}".render)
    Os.exit(-1)
  }
}
checkDeps()


val homeBin = Os.path(Os.cliArgs(0))
val home = homeBin.up
val sireumModule = home / "sireum" / "src" / "org" / "sireum" / "mill" / "SireumModule.scala"

val millVers = ops.StringOps((home / "mill-version.txt").readLineStream.take(1).toISZ(0)).split(c => c.isWhitespace)
assert(millVers.size == 1 || millVers.size == 3)
val millRel = millVers(0)
val (millN, millHash): (String, String) = if (millVers.size != 1) (millVers(1), millVers(2)) else ("", "")
val currVer = st"${(millVers, "-")}-${sireumModule.lastModified}".render
val millVersion = st"${(millVers, "-")}".render
val releasePrefix = "mill-release-"
val releaseName: String = if (Os.isWin) s"$releasePrefix$millVersion.bat" else s"$releasePrefix$millVersion"
val millRelease = home / releaseName
val ver = home / "VER"
val millStandaloneBatch = home / "mill-standalone.bat"
val millStandaloneSh = home / "mill-standalone"
val millBatch = home / "mill.bat"
val millSh = home / "mill"
val update: B = {
  if (ver.exists) {
    ops.StringOps(ver.read).trim != currVer
  } else {
    T
  }
}


if (update) {
  ver.removeAll()
  millStandaloneBatch.removeAll()
  millStandaloneSh.removeAll()
  millBatch.removeAll()
  millSh.removeAll()
} else {
  if (!isDev || millSh.exists) {
    Os.exit(0)
  }
}


def downloadMillRelease(): Unit = {
  for (p <- home.list if ops.StringOps(p.string).startsWith(releasePrefix) && p.name != releaseName) {
    p.removeAll()
  }
  if (!millRelease.exists) {
    println(s"Please wait while downloading mill $millVersion ...")
    millRelease.downloadFrom(s"https://github.com/lihaoyi/mill/releases/download/$millRel/$millVersion")
    millRelease.chmod("+x")
    println()
  }
}


import org.sireum.U8._

def madeInteractive(millJar: Os.Path, millBat: Os.Path, mill: Os.Path): Unit = {
  var lines = 0
  def linesN(n: Z, c: C): B = {
    if (c == '\n') {
      lines = lines + 1
      if (lines == n) {
        lines = 0
        return F
      }
    }
    return T
  }
  def lines12(c: C): B = {
    return linesN(12, c)
  }
  def lines11(c: C): B = {
    return linesN(11, c)
  }
  def lines22(b: U8): B = {
    if (b == u8"10") {
      lines = lines + 1
      if (lines == 22) {
        lines = 0
        return F
      }
    }
    return T
  }
  val headerStream = millJar.readCStream
  val bashHeader: ISZ[C] = headerStream.takeWhile(lines12 _).toISZ
  val batchHeader: ISZ[C] = headerStream.dropWhile(lines12 _).takeWhile(lines11 _).toISZ
  millBat.write(ops.StringOps.replaceAllLiterally(bashHeader, "mill.main.client.MillClientMain", "mill.MillMain"))
  millBat.writeAppend(ops.StringOps.replaceAllLiterally(batchHeader, "mill.main.client.MillClientMain", "mill.MillMain"))
  millBat.writeAppend("\r\n")
  val content = millJar.readU8s
  val size = content.size
  lines = 0
  def findLinesIndex(n: Z): Z = {
    var i = 0
    while (i < size) {
      if (content(i) == u8"10") {
        lines = lines + 1
        if (lines == n) {
          return i
        }
      }
      i = i + 1
    }
    return 0
  }
  millBat.writeAppendU8s(ops.ISZOps(content).slice(findLinesIndex(22) + 1, size))
  millBat.chmod("+x")
  mill.write("#!/bin/bash\n")
  mill.writeAppendU8s(millBat.readU8s)
  mill.chmod("+x")
}


def standalone(): Unit = {
  if (millStandaloneSh.exists) {
    return
  }

  println("Building mill-standalone ...")

  println("Building SireumModule ...")
  (home / "out").removeAll()
  Os.proc(ISZ(millRelease.string, "-i", "sireum.jar")).at(home).runCheck()

  println("Packaging mill-standalone ...")
  val temp = home / "temp"
  temp.removeAll()
  temp.mkdirAll()
  Os.proc(ISZ("7z", "x", (home / "out" / "sireum" / "jar" / "dest" / "out.jar").string)).at(temp).runCheck()
  (temp / "META-INF").removeAll()
  val manifest = temp / "META-INF" / "MANIFEST.MF"
  manifest.removeAll()
  manifest.writeOver(
    st"""Manifest-Version: 1.0
        |Created-By: Sireum mill-build
        |Main-Class: mill.MillMain
        |""".render)
  val millJar = home / "mill.jar"
  millJar.removeAll()
  millRelease.copyTo(millJar)
  val files: ISZ[String] = for (p <- temp.list) yield p.name
  Os.proc(ISZ[String]("7z", "a", millJar.string) ++ files).at(temp).runCheck()
  temp.removeAll()
  madeInteractive(millJar, millStandaloneBatch, millStandaloneSh)
  millJar.removeAll()
  ver.write(currVer)
}


def dev(): Unit = {
  standalone()
  if (millSh.exists) {
    return
  }
  println("Building mill dev ...")
  (home / "git").removeAll()
  if (millHash == "") {
    println(s"Cloning mill $millRel ...")
    Os.proc(ISZ("git", "clone", "--branch", millRel, "https://github.com/lihaoyi/mill", "git")).at(home).runCheck()
  } else {
    println(s"Cloning mill $millVersion ...")
    Os.proc(ISZ("git", "clone", "--branch", millRel, "https://github.com/lihaoyi/mill", "git")).at(home).runCheck()
    Os.proc(ISZ("git", "reset", "--hard", millHash)).at(home / "git").runCheck()
  }
  Os.proc(ISZ("git", "config", "core.filemode", "false")).at(home / "git").runCheck()
  assert(sireumModule.exists)
  sireumModule.copyOverTo(home / "git" / "scalajslib" / "src" / "org" / "sireum" / "mill" / sireumModule.name)
  println("Packaging mill dev with SireumModule ...")
  Os.proc(ISZ(if (Os.isWin) millStandaloneBatch.string else millStandaloneSh.string, "dev.assembly")).at(home / "git").runCheck()
  val name: String = if (Os.isWin) "mill.bat" else "mill"
  val millJar = home / "mill.jar"
  (home / "git" / "out" / "dev" / "assembly" / "dest" / name).copyOverTo(millJar)
  madeInteractive(millJar, millBatch, millSh)
  millJar.removeAll()
  println("Testing mill dev ...")
  (home / "out").removeAll()
  (Os.home / ".mill").removeAll()
  Os.proc(ISZ(if (Os.isWin) millBatch.string else millSh.string, "sireum.jar")).at(home).runCheck()
  ver.writeOver(currVer)
}

downloadMillRelease()

if (Os.cliArgs.size == 1) {
  standalone()
} else {
  dev()
}