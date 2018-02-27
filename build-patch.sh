#!/bin/bash
if [ -z $1 ]; then
  echo "Please specify mill version to download"
  exit -1
fi
curl -Lo mill https://github.com/lihaoyi/mill/releases/download/$1/$1
chmod +x mill
TERM=xterm-color ./mill -i sireum.jar
mkdir temp
cd temp
unzip -q ../mill
unzip -qo ../out/sireum/jar/dest/out.jar
rm -fR META-INF
echo "Main-Class: mill.Main" > ../Manifest.txt
jar cfm ../mill.jar ../Manifest.txt *
cd ..
rm -fR temp Manifest.txt mill
echo -en '#!/usr/bin/env sh\nexec java -DMILL_VERSION=' > header
echo -en $1 >> header
echo -e '$JAVA_OPTS -cp "$0" mill.Main "$@"\n' >> header
cat header mill.jar > mill
rm -fR header mill.jar out
chmod +x mill