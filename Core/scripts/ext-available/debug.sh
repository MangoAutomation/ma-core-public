#!/bin/bash
case "$1" in
    init)
        # Starts the Java debugger as a socket at port 8090 without suspending.
        # Note for JDK 9+ this will only bind to 127.0.0.1, to bind to all interfaces use: address=*:8090
        JPDA=-agentlib:jdwp=transport=dt_socket,address=8090,server=y,suspend=n
        ;;
esac
