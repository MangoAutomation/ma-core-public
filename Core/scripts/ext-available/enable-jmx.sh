#!/bin/bash
case "$1" in
    init)
    # Start JMX and open port on specified host (if host -i does return something useful you must set hostname here)
        JAVAOPTS="$JAVAOPTS -Dcom.sun.management.jmxremote -Djava.rmi.server.hostname=192.168.1.16 -Dcom.sun.management.jmxremote.port=8091 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false"
        ;;
esac
