::#! 2> /dev/null                                                                                           #
@ 2>/dev/null # 2>nul & echo off & goto BOF                                                                 #
export SIREUM_HOME=$(cd -P $(dirname "$0")/.. && pwd -P)                                                    #
if [ ! -z ${SIREUM_PROVIDED_SCALA++} ]; then                                                                #
  SIREUM_PROVIDED_JAVA=true                                                                                 #
fi                                                                                                          #
"${SIREUM_HOME}/bin/init.sh"                                                                                #
if [ -n "$COMSPEC" -a -x "$COMSPEC" ]; then                                                                 #
  export SIREUM_HOME=$(cygpath -C OEM -w -a ${SIREUM_HOME})                                                 #
  if [ -z ${SIREUM_PROVIDED_JAVA++} ]; then                                                                 #
    export PATH="${SIREUM_HOME}/bin/win/java":"${SIREUM_HOME}/bin/win/z3":"$PATH"                           #
    export PATH="$(cygpath -C OEM -w -a ${JAVA_HOME}/bin)":"$(cygpath -C OEM -w -a ${Z3_HOME}/bin)":"$PATH" #
  fi                                                                                                        #
elif [ "$(uname)" = "Darwin" ]; then                                                                        #
  if [ -z ${SIREUM_PROVIDED_JAVA++} ]; then                                                                 #
    export PATH="${SIREUM_HOME}/bin/mac/java/bin":"${SIREUM_HOME}/bin/mac/z3/bin":"$PATH"                   #
  fi                                                                                                        #
elif [ "$(expr substr $(uname -s) 1 5)" = "Linux" ]; then                                                   #
  if [ -z ${SIREUM_PROVIDED_JAVA++} ]; then                                                                 #
    if [ "$(uname -m)" = "aarch64" ]; then                                                                  #
      export PATH="${SIREUM_HOME}/bin/linux/arm/java/bin":"$PATH"                                           #
    else                                                                                                    #
      export PATH="${SIREUM_HOME}/bin/linux/java/bin":"${SIREUM_HOME}/bin/linux/z3/bin":"$PATH"             #
    fi                                                                                                      #
  fi                                                                                                        #
fi                                                                                                          #
if [ -f "$0.com" ] && [ "$0.com" -nt "$0" ]; then                                                           #
  exec "$0.com" "$@"                                                                                        #
else                                                                                                        #
  rm -fR "$0.com"                                                                                           #
  exec "${SIREUM_HOME}/bin/sireum" slang run -n "$0" "$@"                                                #
fi                                                                                                          #
:BOF
setlocal
set SIREUM_HOME=%~dp0../
call "%~dp0init.bat"
if defined SIREUM_PROVIDED_SCALA set SIREUM_PROVIDED_JAVA=true
if not defined SIREUM_PROVIDED_JAVA set PATH=%~dp0win\java\bin;%~dp0win\z3\bin;%PATH%
set NEWER=False
if exist %~dpnx0.com for /f %%i in ('powershell -noprofile -executionpolicy bypass -command "(Get-Item %~dpnx0.com).LastWriteTime -gt (Get-Item %~dpnx0).LastWriteTime"') do @set NEWER=%%i
if "%NEWER%" == "True" goto native
del "%~dpnx0.com" > nul 2>&1
"%~dp0sireum.bat" slang run -n "%0" %*
exit /B %errorlevel%
:native
%~dpnx0.com %*
exit /B %errorlevel%
::!#
// #Sireum
import org.sireum._


def usage(): Unit = {
  println("Mill /build")
  println("Usage: [ dev ]")
}


var isDev = F
if (Os.cliArgs.size > 1) {
  usage()
  Os.exit(0)
} else if (Os.cliArgs.size == 1) {
  if (Os.cliArgs(0) == "dev") {
    isDev = T
  } else {
    usage()
    eprintln(s"Unrecognized command: ${Os.cliArgs(0)}")
    Os.exit(-1)
  }
}

val homeBin: Os.Path = Os.slashDir
val home = homeBin.up
val z7: String = {
  val (plat, exe): (String, String) = Os.kind match {
    case Os.Kind.Mac => ("mac", "7za")
    case Os.Kind.Linux => ("linux", "7za")
    case Os.Kind.LinuxArm => ("linux/arm", "7za")
    case Os.Kind.Win => ("win", "7za.exe")
    case _ => ("unsupported", "")
  }
  val f = home.up / "bin" / plat / exe
  if (f.exists) f.value else "7z"
}

def checkDeps(): Unit = {
  var missing = ISZ[String]()
  if (isDev && !Os.proc(ISZ(z7)).run().ok) {
    missing = missing :+ z7
  }
  if (!Os.proc(ISZ("git", "--version")).run().ok) {
    missing = missing :+ "git"
  }
  if (missing.nonEmpty) {
    eprintln(st"Missing requirement(s): ${(missing, ", ")}".render)
    Os.exit(-1)
  }
}
checkDeps()

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
    millRelease.downloadFrom(s"https://github.com/lihaoyi/mill/releases/download/$millRel/$millVersion-assembly")
    millRelease.chmod("+x")
    println()
  }
}


import org.sireum.U8._

def madeInteractive(millJar: Os.Path, millBat: Os.Path, mill: Os.Path): Unit = {
  var lines = 0
  def linesF(n: Z): U8 => B = {
    return (c: U8) => {
      if (c == u8"0xA") {
        lines = lines + 1
        if (lines == n) {
          lines = 0
          F
        } else {
          T
        }
      } else {
        T
      }
    }
  }
  val millJarU8s = millJar.readU8s
  val headerStream = Jen.IS(millJarU8s)
  val bashHeader1: String = conversions.String.fromU8is(headerStream.takeWhile(linesF(2)).toISZ)
  val bashHeader2 = ops.StringOps(
    st"""if [ "x$${SIREUM_PROVIDED_SCALA}" != "x" ]; then
        |  SIREUM_PROVIDED_JAVA=true
        |fi
        |if [ "x$${SIREUM_HOME}" != "x" ]; then
        |  if [ -n "$$COMSPEC" -a -x "$$COMSPEC" ]; then
        |    if [ "x$${SIREUM_PROVIDED_JAVA}" = "x" ]; then
        |      export JAVA_HOME="$${SIREUM_HOME}/bin/win/java"
        |      export PATH="$${JAVA_HOME}/bin":"$$PATH"
        |    fi
        |  elif [ "$$(uname)" = "Darwin" ]; then
        |    if [ "x$${SIREUM_PROVIDED_JAVA}" = "x" ]; then
        |      export JAVA_HOME="$${SIREUM_HOME}/bin/mac/java"
        |      export PATH="$${JAVA_HOME}/bin":"$$PATH"
        |    fi
        |  elif [ "$$(expr substr $$(uname -s) 1 5)" = "Linux" ]; then
        |    if [ "x$${SIREUM_PROVIDED_JAVA}" = "x" ]; then
        |      if [ "$$(uname -m)" = "aarch64" ]; then
        |        export JAVA_HOME="$${SIREUM_HOME}/bin/linux/arm/java"
        |      else
        |        export JAVA_HOME="$${SIREUM_HOME}/bin/linux/java"
        |      fi
        |      export PATH="$${JAVA_HOME}/bin":"$$PATH"
        |    fi
        |  fi
        |fi""".render).replaceAllLiterally("\r\n", "\n")
  val bashHeader3: String = conversions.String.fromU8is(headerStream.dropWhile(linesF(2)).takeWhile(linesF(6)).toISZ)
  val bashHeader4: String = ops.StringOps(ops.StringOps(
    conversions.String.fromU8is(headerStream.dropWhile(linesF(15)).takeWhile(linesF(2)).toISZ)).
    replaceAllLiterally("mill.MillMain \"$@\"", "mill.MillMain \"$@\"")).trim
  val bashHeader5: String = "\nexit\n"
  val batchHeader1: String = ops.StringOps(conversions.String.fromU8is(headerStream.dropWhile(linesF(25)).takeWhile(linesF(5)).toISZ)).trim
  val batchHeader2: String =
    ops.StringOps(ops.StringOps(
      st"""if not "%SIREUM_HOME%"=="" (
          |  set "JAVA_HOME=%SIREUM_HOME%\bin\win\java"
          |  set "PATH=%SIREUM_HOME%\bin\win\java\bin;%PATH%"
          |)
          |""".render).replaceAllLiterally("\r\n", "\n")).replaceAllLiterally("\n", "\r\n")
  val batchHeader3: String = ops.StringOps(ops.StringOps(
    conversions.String.fromU8is(headerStream.dropWhile(linesF(35)).takeWhile(linesF(2)).toISZ)).
    replaceAllLiterally("mill.MillMain %*", "mill.MillMain %*")).trim
  val batchHeader4: String = "\r\nendlocal\r\nexit /B %errorlevel%\r\n"
//  println("Bash header 1")
//  println(bashHeader1)
//  println("Bash header 3")
//  println(bashHeader3)
//  println("Bash header 4")
//  println(bashHeader4)
//  println("Batch header 1")
//  println(batchHeader1)
//  println("Batch header 3")
//  println(batchHeader3))
  millBat.write(bashHeader1)
  millBat.writeAppend("\n")
  millBat.writeAppend(bashHeader2)
  millBat.writeAppend(bashHeader3)
  millBat.writeAppend("\n")
  millBat.writeAppend(bashHeader4)
  millBat.writeAppend(bashHeader5)
  millBat.writeAppend(batchHeader1)
  millBat.writeAppend("\r\n")
  millBat.writeAppend(batchHeader2)
  millBat.writeAppend(batchHeader3)
  millBat.writeAppend(batchHeader4)
  def findLinesIndex(n: Z): Z = {
    lines = 0
    var i = 0
    for (e <- headerStream) {
      if (e == u8"0xA") {
        lines = lines + 1
        if (lines == n) {
          return i
        }
      }
      i = i + 1
    }
    return 0
  }
  val offset = findLinesIndex(41) + 1
  millBat.writeAppendU8Parts(millJarU8s, offset, millJarU8s.size - offset)
  millBat.chmod("+x")
  mill.write("#!/bin/sh\n")
  mill.writeAppend(bashHeader1)
  mill.writeAppend("\n")
  mill.writeAppend(bashHeader2)
  mill.writeAppend(bashHeader3)
  mill.writeAppend("\n")
  mill.writeAppend(bashHeader4)
  mill.writeAppend(bashHeader5)
  mill.writeAppend(batchHeader1)
  mill.writeAppend("\r\n")
  mill.writeAppend(batchHeader2)
  mill.writeAppend(batchHeader3)
  mill.writeAppend(batchHeader4)
  mill.writeAppendU8Parts(millJarU8s, offset, millJarU8s.size - offset)
  mill.chmod("+x")
}


def standalone(): Unit = {
  if (millStandaloneSh.exists) {
    return
  }

  println("Building mill-standalone ...")

  println("Building SireumModule ...")
  (home / "out").removeAll()
  millRelease.call(ISZ("-i", "sireum.jar")).at(home).runCheck()

  println("Packaging mill-standalone ...")
  val temp = home / "temp"
  temp.removeAll()
  temp.mkdirAll()
  Os.proc(ISZ(z7, "x", (home / "out" / "sireum" / "jar" / "dest" / "out.jar").string)).at(temp).runCheck()
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
  Os.proc(ISZ[String](z7, "a", millJar.string) ++ files).at(temp).runCheck()
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
  val dest = home / "git" / "out" / "dev" / "assembly" / "dest"
  val vmoptions = dest / "mill.vmoptions"
  (dest / name).copyOverTo(millJar)
  if (vmoptions.exists) {
    vmoptions.copyOverTo(home / vmoptions.name)
  }
  madeInteractive(millJar, millBatch, millSh)
  millJar.removeAll()
  println("Testing mill dev ...")
  (home / "out").removeAll()
  (Os.home / ".mill").removeAll()
  Os.proc(ISZ(if (Os.isWin) millBatch.string else millSh.string, "sireum.jar")).at(home).runCheck()
  ver.writeOver(currVer)
}

downloadMillRelease()

if (Os.cliArgs.isEmpty) {
  standalone()
} else {
  dev()
}
