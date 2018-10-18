#!/bin/bash -e
export SCRIPT_DIR=$( cd "$( dirname "$0" )" &> /dev/null && pwd )
cd ${SCRIPT_DIR}
./prelude.sh
if [ -f mill ]; then
  exit 0
fi
if [ ! -f mill-standalone ]; then
  echo "Building mill-standalone first ..."
  bash ./build-standalone.sh
  code=$?
  if [[ ${code} -ne 0 ]]; then
    exit ${code}
  fi
fi
rm -fR git out
echo "Cloning mill master branch ..."
git clone https://github.com/lihaoyi/mill.git git
IFS=' ' read -r -a ver <<< $(head -n 1 mill-version.txt)
echo "Building mill with SireumModule ..."
mkdir -p git/scalajslib/src/org/sireum/mill
cd git
git reset --hard ${ver[1]}
cp ${SCRIPT_DIR}/sireum/src/org/sireum/mill/SireumModule.scala scalajslib/src/org/sireum/mill/
${SCRIPT_DIR}/mill-standalone dev.assembly
cp out/dev/assembly/dest/* ${SCRIPT_DIR}/
cd ${SCRIPT_DIR}
rm -fR ~/.mill
if [[ -f mill ]]; then
  MILL=mill
else
  MILL=mill.bat
fi
head -n 22 ${MILL} > header
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
tail -n +22 ${MILL} > mill.jar
cat header mill.jar > ${MILL}
rm header mill.jar
chmod +x ${MILL}
./${MILL} sireum.jar
rm -fR out git/scalajslib/src/org/sireum/mill/SireumModule.scala
echo "${ver[0]}-${ver[1]}" > VER
echo "... done!"
