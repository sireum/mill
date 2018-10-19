#!/bin/bash -e
export SCRIPT_DIR=$( cd "$( dirname "$0" )" &> /dev/null && pwd )
cd ${SCRIPT_DIR}
IFS=' ' read -r -a ver <<< $(head -n 1 mill-version.txt)
grep -q "${ver[0]}-${ver[1]}-${ver[2]}" ${SCRIPT_DIR}/VER &> /dev/null && MILL_UPDATE=false || MILL_UPDATE=true
if [ "${MILL_UPDATE}" = "true" ]; then
  rm -fR mill mill-release mill-standalone
fi
