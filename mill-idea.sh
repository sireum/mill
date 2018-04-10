#!/bin/bash
rm -fR ~/.mill out
mill $* mill.scalalib.GenIdea/idea
