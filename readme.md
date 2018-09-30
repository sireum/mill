# Sireum Mill Build

This repository holds a script to build [mill](https://github.com/lihaoyi/mill)
with [SireumModule](sireum/src/org/sireum/mill/SireumModule.scala) embedded in it to
ease building Sireum modules and with IntelliJ support for SireumModule.

## Standalone Version

Based on mill releases. 
Latest version is available [here](http://files.sireum.org/mill-standalone).

To build:

```bash
./build-standalone.sh
```

This produces `mill-standalone`, and the mill release version is stored as 
`mill-release`.

## Local Version with IntelliJ Support for SireumModule

Based on mill master branch.

To build:

```bash
./build.sh
```

It calls `build-standalone.sh` first if `mill-standalone` is not available.

This produces `mill` (and `mill-standalone`).


## Notes

* The `NODEJS_MAX_HEAP` environment variable can be set to specify a custom max heap size for Node.js (in MB).
  For example, `NODEJS_MAX_HEAP=4096` sets the max heap size to 4096MB.