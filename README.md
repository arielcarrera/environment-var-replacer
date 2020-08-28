# environment-var-replacer ![Maven Package](https://github.com/arielcarrera/environment-var-replacer/workflows/Maven%20Package/badge.svg)
Little program to replace environment variables in old config files (for java docker images usage).

# How to build

## Requirements

1.  Java JDK 8 (or Graalvm 20.0.0)
2.  Maven installed
3.  Docker installed

## Steps 
### Compile:

1. From project root directory: 
```
mvn clean package
```

### Native compilation with Graalvm inside Docker (optional):

2. From project root directory:
```
mvn clean package

```
3. From docker-graalvm/centos directory (first time only to create Centos-GraalVm image): *docker-build.bat [VERSION]*
```
docker-build.bat 20.0.0
```
4. From docker-graalvm/centos directory: *docker-run.bat [VERSION] [PROJECT_PATH]*
```
docker-run.bat 20.0.0 C:\Dev\Workspace\environment-var-replacer
```
5. From docker-graalvm/ubuntu directory (first time only to create Ubuntu-GraalVm image): *docker-build.bat [VERSION]*
```
docker-build.bat 20.0.0
```
6. From docker-graalvm/centos directory: *docker-run.bat [VERSION] [PROJECT_PATH]*
```
docker-run.bat 20.0.0 C:\Dev\Workspace\environment-var-replacer
```

# Usage:

# Java
Eg. with backup:
```
java -jar target\environment-var-replacer.jar target-file.xml -fb
```
Eg. With backup and custom variables:
```
java -jar target\environment-var-replacer.jar target-file.xml -fb -DMY_VAR=CUSTOM_VALUE -DMY_VAR2=
```

## GraalVm - Native image version:
```
./environment-var-replace [-s] [PATH_TO_CONFIG_FILES] [PATH_TO_TARGET_FILES] [-p [PROPERTIES_FILE]] [-b] [-fb] [-d]
```
- *-s PATH_TO_CONFIG_FILES* : flag to indicate that a source file is indicated. PATH_TO_CONFIG_FILES is a comma separated list of configuration files paths. Each line in a configuration file is a target file path to process
- *PATH_TO_TARGET_FILES* : Path to target file to be modified. Eg. testdir/testfile.xml
- *-p PROPERTIES_FILE* : flag to indicate that properties must to be read froma properties file. PROPERTIES_FILE is a path to a file that contains properties that will be used instead of environment variables
- *-b* : Option to enable backup of file. It will generate a .bak file. If .bak file exists, it will exit with error.
- *-fb* : Option to enable backup of file. It will generate a .bak file. If .bak file exists, it will override it.
- *-d* : Option to enable debug mode. It will print some traces to console.

## Examples


### replace with backup
```
./environment-var-replace testdir/testfile.xml -b
```

### replace with different output filepath
```
./environment-var-replace testdir/testfile.xml:outputdir/outputfile.xml -b
```

### multiple target files with backup (force) and debug modes:
```
./environment-var-replace testdir/testfile.xml,testdir/testfile2.xml -fb -d
```

### replace with configuration file
```
./environment-var-replace -s testdir/replacer.cfg
```

replacer.cfg content:
```
testdir/testfile.xml
testdir/testfile2.xml:output/outputfile2.xml
```

### replace with multiple configuration files and multiple target files combined
```
./environment-var-replace -s testdir/replacer.cfg testdir/testfile3.xml:outputfile3.txt,testdir/testfile4.xml
```

replacer.cfg content:
```
testdir/testfile.xml
testdir/testfile2.xml:output/outputfile2.xml
```

