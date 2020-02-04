#!/bin/sh

#
# Copyright (C) 2020 Infinite Automation Systems Inc. All rights reserved.
# @author Jared Wiltshire
#

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd -P)"
SIGNAL=TERM
[ ! -z "$1" ] && SIGNAL="$1"

# Only set MA_HOME if not already set
[ -z "$MA_HOME" ] && MA_HOME="$(dirname -- "$SCRIPT_DIR")"

if [ ! -d "$MA_HOME" ]; then
    echo 'Error: MA_HOME is not set or is not a directory'
    exit 1
fi

if [ -e "$MA_HOME/bin/ma.pid" ]; then
	PID="$(cat "$MA_HOME/bin/ma.pid")"
	echo "Killing Mango PID $PID"

	while kill -"$SIGNAL" "$PID" > /dev/null 2>&1; do
		sleep 1
	done

	# Clean up PID file
	rm -f "$MA_HOME/bin/ma.pid"
else
	echo "Mango PID file $MA_HOME/bin/ma.pid does not exist, did you start Mango using start-mango.sh?"
	exit 2
fi

exit 0
