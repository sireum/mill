#!/bin/bash -e
export SCRIPT_DIR=$( cd "$( dirname "$0" )" &> /dev/null && pwd )
cd $SCRIPT_DIR
rm -fR mill git out
if [[ ! -x $SCRIPT_DIR/mill-standalone ]]; then
  echo "Building mill-standalone first ..."
  bash $SCRIPT_DIR/build-standalone.sh $*
  code=$?
  if [[ $code -ne 0 ]]; then
    exit $code
  fi
fi
echo "Shallow cloning mill master branch ..."
git clone --depth=1 https://github.com/lihaoyi/mill.git git
echo "Building mill with SireumModule ..."
mkdir -p git/scalajslib/src/org/sireum/mill
cp sireum/src/org/sireum/mill/SireumModule.scala git/scalajslib/src/org/sireum/mill/
cd git
$SCRIPT_DIR/mill-standalone dev.assembly
cp out/dev/assembly/dest/mill $SCRIPT_DIR/mill
cd $SCRIPT_DIR
chmod +x mill
rm -fR ~/.mill
$SCRIPT_DIR/mill -i sireum.jar
rm -fR out
echo "... done!"
