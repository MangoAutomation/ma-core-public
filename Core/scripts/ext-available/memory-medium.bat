if "%1" == "init" (
        rem Startup with Java Memory setup for medium installation
        set JAVAOPTS=%JAVAOPTS% -Xms5g -Xmx5g
)
