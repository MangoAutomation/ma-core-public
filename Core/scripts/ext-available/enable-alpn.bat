if "%1" == "init" (
        rem Enable alpn support in Jetty (Only required if using Java 8, will fail on Java 9+)
        set JAVAOPTS=%JAVAOPTS% -javaagent:%MA_HOME%/boot/jetty-alpn-agent.jar
)
