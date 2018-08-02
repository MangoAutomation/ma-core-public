#!/bin/bash
case "$1" in
    init)
        # Enable alpn support in Jetty (Only required if using Java 8, will fail on Java 9+)
        JAVAOPTS="$JAVAOPTS -javaagent:$MA_HOME/boot/jetty-alpn-agent.jar"
        ;;
esac
