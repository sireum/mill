#!/bin/bash
rm -fR mill git out
wget -q http://files.sireum.org/mill
chmod +x mill
git clone https://github.com/lihaoyi/mill.git git
mkdir -p git/scalajslib/src/org/sireum/mill
cp sireum/src/org/sireum/mill/SireumModule.scala git/scalajslib/src/org/sireum/mill/
cd git
TERM=xterm-color ../mill dev.assembly
cp out/dev/assembly/dest/mill ../mill
cd ..
chmod +x mill
TERM=xterm-color ./mill -i sireum.jar
rm -fR out

