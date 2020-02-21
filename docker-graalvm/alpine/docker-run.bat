@echo Off
if "%~2"=="" (
    echo Se debe indicar el tag a utilizar como parametro. Ej. docker-run.bat 1.0.0-SNAPSHOT [PROJECT-PATH]
) else (
Rem docker run -it --volume %~2:/project --entrypoint=/bin/sh tools/graalvm-jdk8-alpine:%~1
	docker run --volume %~2:/project tools/graalvm-jdk8-alpine:%~1
)