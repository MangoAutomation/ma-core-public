if "%1" == "init" (
        rem Use DEBUG level configuration file 
        set JAVAOPTS=%JAVAOPTS% -Dlog4j.configurationFile=file:%MA_HOME%/classes/debug-log4j2.xml
)
