@echo Off
if "%~2"=="" (
    echo Se debe indicar el tag a utilizar como parametro. Ej. docker-run.bat 1.0.0-SNAPSHOT [PROJECT-PATH]
) else (
	docker run --volume %~2:/project tools/graalvm-jdk8-centos:%~1
)