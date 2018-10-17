#!/bin/bash
export SIREUM_SOURCE_BUILD=true
mill $* mill.scalalib.GenIdea/idea
