#!/bin/bash
case "$1" in
    init)
        # Starts the Java debugger as a socket at port 8090 without suspending.
        JPDA=-agentlib:jdwp=transport=dt_socket,address=8090,server=y,suspend=n
        ;;
esac
