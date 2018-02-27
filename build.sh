#!/bin/bash
if [ -z $1 ]; then
  echo "Please specify mill version to use to build"
  exit -1
fi
rm -fR mill git out
curl -Lo mill https://github.com/lihaoyi/mill/releases/download/$1/$1
chmod +x mill
git clone https://github.com/lihaoyi/mill.git git
mkdir -p git/scalajslib/src/org/sireum/mill
cp sireum/src/org/sireum/mill/SireumModule.scala git/scalajslib/src/org/sireum/mill/
cd git
TERM=xterm-color ../mill dev.assembly
cp out/dev/assembly/dest/mill ../mill
cd ..
