@echo Off
if "%~1"=="" (
    echo Se debe indicar el tag a utilizar como parametro. Ej. docker-build.bat 1.0.0-SNAPSHOT
) else (
	docker build . -f Dockerfile -t tools/graalvm-jdk8-centos:%~1
)