if "%1" == "init" (
        rem Enable alpn support in Jetty
        set JAVAOPTS=%JAVAOPTS% -javaagent:%MA_HOME%/boot/jetty-alpn-agent.jar
)
