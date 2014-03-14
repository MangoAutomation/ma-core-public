#!/bin/bash
#
#    Copyright (C) 2014 Infinite Automation Systems Inc. All rights reserved.
#    @author Matthew Lohbihler
#

# -----------------------------------------------------------------------------
# Start/Stop Script for the Mango Automation
#

PRGDIR=`dirname "$0"`

# Only set MA_HOME if not already set
[ -z "$MA_HOME" ] && MA_HOME=`cd "$PRGDIR/.." >/dev/null; pwd -P`

if [ ! -r "$MA_HOME"/bin/ma.sh ]; then
    echo The MA_HOME environment variable is not defined correctly
    echo This environment variable is needed to run this program
    exit 1
fi
export MA_HOME

case "$1" in
    start)
        echo MA_HOME is $MA_HOME
        export JPDA EXECJAVA JAVAOPTS SYSOUT SYSERR
        source $MA_HOME/bin/ma-init.sh
        $MA_HOME/bin/ma-start.sh &
        ;;
    restart)
        source $MA_HOME/bin/ma-restart.sh
        ;;
    stop)
        source $MA_HOME/bin/ma-stop.sh
        ;;
    *)
        echo "Usage: ma.sh {start|stop|restart}"
        exit 1
        ;;
esac

exit 0
