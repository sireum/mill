#!/bin/bash
export SCRIPT_DIR=$( cd "$( dirname "$0" )" &> /dev/null && pwd )
if [ -f ${SCRIPT_DIR}/mill.bat ]; then
  MILL=mill.bat
else
  MILL=mill
fi
${mill} mill.scalalib.Dependency/updates
