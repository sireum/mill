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
if [[ -f mill ]] || [[ -f mill.bat ]]; then
  exit 0
fi
if [[ ! -f mill-standalone ]]; then
  echo "Building mill-standalone first ..."
  bash ./build-standalone.sh
  code=$?
  if [[ ${code} -ne 0 ]]; then
    exit ${code}
  fi
fi
rm -fR git out
IFS=$' \r' read -r -a ver <<< $(head -n 1 mill-version.txt)
if [[ ${ver[2]} == "" ]]; then
  echo "Cloning mill ${ver[0]} ..."
  git clone --branch ${ver[0]} https://github.com/lihaoyi/mill.git git
else
  echo "Cloning mill ${ver[0]}-${ver[1]}-${ver[2]} ..."
  git clone https://github.com/lihaoyi/mill.git git
  cd git
  git reset --hard ${ver[2]}
  cd ..
fi
echo "Building mill with SireumModule ..."
mkdir -p git/scalajslib/src/org/sireum/mill
cd git
git config core.filemode false
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
rm -fR out
if [[ -f mill.bat ]]; then
  ln -s mill.bat mill
fi
echo "${ver[0]}-${ver[1]}-${ver[2]}" > VER
echo "... done!"
