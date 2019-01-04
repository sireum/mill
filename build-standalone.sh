#!/bin/bash -e
export SCRIPT_DIR=$( cd "$( dirname "$0" )" &> /dev/null && pwd )
cd ${SCRIPT_DIR}
IFS=' ' read -r -a ver <<< $(head -n 1 mill-version.txt)
if [[ "${ver[1]}" == "" ]]; then
  MILL_VERSION=${ver[0]}
else
  MILL_VERSION=${ver[0]}-${ver[1]}-${ver[2]}
fi
echo "Downloading mill $MILL_VERSION ..."
rm -fR mill-release
curl -sLo mill-release https://github.com/lihaoyi/mill/releases/download/${ver[0]}/${MILL_VERSION}
chmod +x mill-release
echo "Building SireumModule ..."
./mill-release -i sireum.jar
echo "Packaging mill-standalone ..."
rm -fR temp
mkdir temp
cd temp
unzip -qo ${SCRIPT_DIR}/mill-release > /dev/null || true
unzip -qo ${SCRIPT_DIR}/out/sireum/jar/dest/out.jar > /dev/null || true
rm -fR META-INF
echo "Main-Class: mill.MillMain" > ${SCRIPT_DIR}/Manifest.txt
zip -r ${SCRIPT_DIR}/mill.jar ${SCRIPT_DIR}/Manifest.txt * > /dev/null
cd ${SCRIPT_DIR}
rm -fR temp Manifest.txt mill-standalone
head -n 22 mill-release > header
sed -i.bak 's/%1/-i/' header
sed -i.bak 's/\$1/-i/' header
sed -i.bak 's/mill.MillMain "/-DMILL_PATH="\$0" mill.MillMain "/' header
sed -i.bak 's/mill.MillMain %/-DMILL_PATH="%~dpnx0" mill.MillMain %/' header
sed -i.bak 's/mill.main.client.MillClientMain "/-DMILL_PATH="\$0" mill.main.client.MillClientMain "/' header
sed -i.bak 's/mill.main.client.MillClientMain %/-DMILL_PATH="%~dpnx0" mill.main.client.MillClientMain %/' header
head -n 2 header > header.pre
rm header.bak
tail -n 20 header > header.post
cat header.pre abspath.sh header.post > header
sed -i.bak 's/\$0/\$( abspath \$0 )/g' header
rm header.bak header.pre header.post
cat header mill.jar > mill-standalone
rm -fR header mill.jar out
chmod +x mill-standalone
echo "... done!"
