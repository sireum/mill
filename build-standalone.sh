#!/bin/bash -e
#
# Copyright (c) 2019, Robby, Kansas State University
# All rights reserved.
#
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions are met:
#
# 1. Redistributions of source code must retain the above copyright notice, this
#    list of conditions and the following disclaimer.
# 2. Redistributions in binary form must reproduce the above copyright notice,
#    this list of conditions and the following disclaimer in the documentation
#    and/or other materials provided with the distribution.
#
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
# ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
# WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
# DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
# ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
# (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
# LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
# ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
# (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
# SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
#
export SCRIPT_DIR=$( cd "$( dirname "$0" )" &> /dev/null && pwd )
cd ${SCRIPT_DIR}
./prelude.sh
if [[ -f mill-standalone.bat ]]; then
  exit 0
fi
IFS=$' \r' read -r -a ver <<< $(head -n 1 mill-version.txt)
if [[ "$(uname)" == "Darwin" ]]; then
  TS=$(stat -f "%m" ${SCRIPT_DIR}/sireum/src/org/sireum/mill/SireumModule.scala)
else
  TS=$(date +%s -r ${SCRIPT_DIR}/sireum/src/org/sireum/mill/SireumModule.scala)
fi
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
7z x ${SCRIPT_DIR}/out/sireum/jar/dest/out.jar > /dev/null
rm -fR META-INF
echo "Main-Class: mill.MillMain" > ${SCRIPT_DIR}/Manifest.txt
cp ${SCRIPT_DIR}/mill-release ${SCRIPT_DIR}/mill.jar
7z a ${SCRIPT_DIR}/mill.jar ${SCRIPT_DIR}/Manifest.txt * > /dev/null
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
cat header mill.jar > mill-standalone.bat
rm -fR header mill.jar out
chmod +x mill-standalone.bat
ln -s mill-standalone.bat mill-standalone
echo "${ver[0]}-${ver[1]}-${ver[2]}-${TS}" > VER
echo "... done!"
