# environment-var-replacer
Little program to replace environment variables in old config files (for java docker images usage).

# How to build

## Requirements

1.  Java JDK 8 (or Graalvm 20.0.0)
2.  Maven installed
3.  Docker installed

## Steps 
### Compile:

1. (From project root directory): mvn clean package

### (optional) Native compilation with Graalvm inside Docker:

2. (From project root directory): mvn clean package
3. (From docker-graalvm/centos directory): docker-build.bat [VERSION]
  Eg. docker-build.bat 1.0.0
4. (From docker-graalvm/centos directory): docker-run.bat [VERSION] [PROJECT_PATH]
  Eg. docker-run.bat 1.0.0 C:\Dev\Workspace\environment-var-replacer


# Usage:

# Java
Eg. with backup:

java -jar target\environment-var-replacer.jar target-file.xml -fb

Eg. With backup and custom variables:

java -jar target\environment-var-replacer.jar target-file.xml -fb -DMY_VAR=CUSTOM_VALUE -DMY_VAR2=

## GraalVm - Native image version:

./environment-var-replace [path-to-target-file] [-b] [-fb] [-d]

[path-to-target-file] : Path to target file to be modified. Eg. testdir/testfile.xml
[-b] : Option to enable backup of file. It will generate a .bak file. If .bak file exists, it will exit with error.
[-fb] : Option to enable backup of file. It will generate a .bak file. If .bak file exists, it will override it.
[-d] : Option to enable debug mode. It will print some traces to console.

Eg. ./environment-var-replace testdir/testfile.xml -b

### multiple target files:
./environment-var-replace [path-to-target-file,path-to-target-file2,path-to-target-file3...] [-b] [-fb] [-d]

[path-to-target-file,path-to-target-file2,path-to-target-file3...] : Comma separated list of paths to target files to be modified. Eg. testdir/testfile.xml,testdir/testfile2.xml
[-b] : Option to enable backup of file. It will generate a .bak file. If .bak file exists, it will exit with error.
[-fb] : Option to enable backup of file. It will generate a .bak file. If .bak file exists, it will override it.
[-d] : Option to enable debug mode. It will print some traces to console.

Eg. ./environment-var-replace testdir/testfile.xml,testdir/testfile2.xml -fb -d

