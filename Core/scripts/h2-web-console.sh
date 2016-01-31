#!/bin/bash
#
#    Copyright (C) 2015 Infinite Automation Systems Inc. All rights reserved.
#    @author Terry Packer
#

# -----------------------------------------------------------------------------
# This script will startup the H2 database to allow modifications off-line.  Mango
# should NOT be running when this script is run.
#

#Edit to desired port number
H2_PORT=8081


PRGDIR=`dirname "$0"`

# Only set MA_HOME if not already set
[ -z "$MA_HOME" ] && MA_HOME=`cd "$PRGDIR/.." >/dev/null; pwd -P`

if [ ! -r "$MA_HOME"/bin/h2-web-console.sh ]; then
    echo The MA_HOME environment variable is not defined correctly
    echo This environment variable is needed to run this program
    exit 1
fi

# Add -webAllowOthers to use the web console from an origin other than localhost
java -cp ../lib/h2*.jar org.h2.tools.Server -web -webPort "$H2_PORT" -baseDir "$MA_HOME"/databases
