#!/bin/bash
export SCRIPT_DIR=$( cd "$( dirname "$0" )" &> /dev/null && pwd )
cd $SCRIPT_DIR
IFS=' ' read -r -a ver <<< $(head -n 1 mill-version.txt)
if [[ "${ver[1]}" == "" ]]; then
  MILL_VERSION=${ver[0]}
else
  MILL_VERSION=${ver[0]}-${ver[1]}
fi
echo "Downloading mill $MILL_VERSION ..."
rm -fR $SCRIPT_DIR/mill-release
curl -sLo mill-release https://github.com/lihaoyi/mill/releases/download/${ver[0]}/$MILL_VERSION
chmod +x mill-release
echo "Building SireumModule ..."
$SCRIPT_DIR/mill-release -i sireum.jar
echo "Packaging mill-standalone ..."
rm -fR temp
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
echo -en $MILL_VERSION >> header
echo -e ' $JAVA_OPTS -cp "$0" mill.Main "$@"\n' >> header
cat header mill.jar > mill-standalone
rm -fR header mill.jar out
chmod +x mill-standalone
echo "... done!"
