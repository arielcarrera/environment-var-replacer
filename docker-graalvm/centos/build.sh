#!/bin/bash

$JAVA_HOME/bin/native-image --no-server \
         --class-path /project/target/environment-var-replacer.jar \
	     -H:Name=environment-var-replacer-centos \
	     -H:Class=com.github.arielcarrera.env.var.replacer.EnvVarReplacer \
	     -H:+ReportUnsupportedElementsAtRuntime \
	     -H:+AllowVMInspection
		 
#-H:ReflectionConfigurationFiles=build/reflect.json \
