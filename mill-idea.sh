#!/bin/bash
if [ -z $SIREUM_HOME ]; then
  echo "Please specify SIREUM_HOME env var"
  exit -1
fi
rm -fR out
mill $* mill.scalalib.GenIdeaModule/idea
