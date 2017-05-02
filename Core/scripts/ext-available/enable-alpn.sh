#!/bin/bash
case "$1" in
    init)
        # Enable alpn support in Jetty 
        JAVAOPTS="$JAVAOPTS -javaagent:$MA_HOME/boot/jetty-alpn-agent.jar"
        ;;
esac
