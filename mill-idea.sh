#!/bin/bash
export SCRIPT_DIR=$( cd "$( dirname "$0" )" &> /dev/null && pwd )
export SIREUM_SOURCE_BUILD=true
if [ -f ${SCRIPT_DIR}/mill.bat ]; then
  MILL=mill.bat
else
  MILL=mill
fi
${MILL} $* mill.scalalib.GenIdea/idea
