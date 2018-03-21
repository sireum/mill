#!/bin/bash
rm -fR ~/.mill out
mill $* mill.scalalib.GenIdeaModule/idea
