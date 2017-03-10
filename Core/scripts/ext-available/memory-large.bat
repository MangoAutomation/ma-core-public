if "%1" == "init" (
        rem Startup with Java Memory setup for Large installation
        set JAVAOPTS=%JAVAOPTS% -Xms10g -Xmx10g
)
