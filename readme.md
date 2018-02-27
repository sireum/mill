# Sireum Mill Build

This repository holds a script to build a custom master branch [mill](https://github.com/lihaoyi/mill)
with [SireumModule](sireum/src/org/sireum/mill/SireumModule.scala) embedded in it to
ease building Sireum modules and with IntelliJ support for SireumModule.

## Standalone Version
  
To build:

```bash
./build.sh
```

## Local Version with IntelliJ Support for SireumModule
 
To build:

```bash
./build-patch.sh <mill-release-version>
```

For example:

```bash
./build-patch.sh 0.1.3
```
