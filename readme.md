# Sireum Mill Build

This repository holds a script to build [mill](https://github.com/lihaoyi/mill)
with [SireumModule](sireum/src/org/sireum/mill/SireumModule.scala) embedded in it to
ease building Sireum modules and with IntelliJ support for SireumModule.


#### Requirements: `7z` (and `git` for dev version)

## Standalone Version

Based on mill releases. 
Latest version is available [here](https://github.com/sireum/releases/releases/download/mill/mill).

To build:

* **macOS/Linux**:

  ```bash
  bin/build.cmd
  ```
  
* Windows

  ```bash
  bin\build.cmd
  ```
  
This produces `mill-standalone` and `mill-standalone.bat`, and the mill release version is stored as 
`mill-release-*`.

## Local Version with IntelliJ Support for SireumModule (dev)

Based on mill master branch.

To build:

* **macOS/Linux**:

  ```bash
  bin/build.cmd dev
  ```
  
* Windows

  ```bash
  bin\build.cmd dev
  ```

This produces `mill` and `mill.bat`).


## Notes

* When using mill, the `NODEJS_MAX_HEAP` environment variable can be set to specify a custom max heap size for Node.js (in MB).
  For example, `NODEJS_MAX_HEAP=4096` sets the max heap size to 4096MB.