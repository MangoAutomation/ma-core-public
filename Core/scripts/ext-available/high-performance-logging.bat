if "%1" == "init" (
        rem Enable high performance logging configuration
		set JAVAOPTS=%JAVAOPTS% -Dlog4j2.enableThreadlocals=true -Dlog4j2.contextSelector=org.apache.logging.log4j.core.async.AsyncLoggerContextSelector -Dlog4j.configurationFile=file:/%MA_HOME%/classes/high-performance-log4j2.xml
)
