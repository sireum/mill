# Sireum Mill Build

This repository holds a script to build  [mill](https://github.com/lihaoyi/mill)
with [SireumModule](sireum/src/org/sireum/mill/SireumModule.scala) embedded in it to
ease building Sireum modules and with IntelliJ support for SireumModule.

## Standalone Version

Based on mill releases. 
Latest version is available [here](http://files.sireum.org/mill).

To build:

```bash
./build-patch.sh <mill-release-version>
```

For example:

```bash
./build-patch.sh 0.1.3
```

## Local Version with IntelliJ Support for SireumModule

Based on mill master branch.

To build:

```bash
./build.sh
```
