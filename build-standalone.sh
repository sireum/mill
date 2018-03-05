#!/bin/bash
if [[ -z $1 ]]; then
  echo "Please specify mill version to use"
  exit -1
fi
export SCRIPT_DIR=$( cd "$( dirname "$0" )" &> /dev/null && pwd )
cd $SCRIPT_DIR
echo "Downloding mill $1 ..."
rm -fR $SCRIPT_DIR/mill-release
curl -sLo mill-release https://github.com/lihaoyi/mill/releases/download/$1/$1
chmod +x mill-release
echo "Building SireumModule ..."
$SCRIPT_DIR/mill-release -i sireum.jar
echo "Packaging mill-standalone ..."
mkdir temp
cd temp
unzip -q $SCRIPT_DIR/mill-release > /dev/null
unzip -qo $SCRIPT_DIR/out/sireum/jar/dest/out.jar > /dev/null
rm -fR META-INF
echo "Main-Class: mill.Main" > $SCRIPT_DIR/Manifest.txt
jar cfm $SCRIPT_DIR/mill.jar $SCRIPT_DIR/Manifest.txt *
cd $SCRIPT_DIR
rm -fR temp Manifest.txt mill-standalone
echo -en '#!/usr/bin/env sh\nexec java -DMILL_VERSION=' > header
echo -en $1 >> header
echo -e ' $JAVA_OPTS -cp "$0" mill.Main "$@"\n' >> header
cat header mill.jar > mill-standalone
rm -fR header mill.jar out
chmod +x mill-standalone
echo "... done!"
