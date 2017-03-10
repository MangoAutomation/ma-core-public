if "%1" == "init" (
        rem Startup with Java Memory setup for small installation
        set JAVAOPTS=%JAVAOPTS% -Xms1600m -Xmx1600m
)
