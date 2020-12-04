#!/bin/sh

#
# Copyright (C) 2019 Infinite Automation Systems Inc. All rights reserved.
# @author Jared Wiltshire
# @author Matthew Lohbihler
#

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd -P)"

# Only set MA_HOME if not already set
[ -z "$MA_HOME" ] && MA_HOME="$(dirname -- "$SCRIPT_DIR")"

if [ ! -d "$MA_HOME" ]; then
    echo 'Error: MA_HOME is not set or is not a directory'
    exit 1
fi

echo MA_HOME is "$MA_HOME"

if [ -e "$MA_HOME"/bin/ma.pid ]; then
	PID="$(cat "$MA_HOME"/bin/ma.pid)"
	if ps -p "$PID" > /dev/null 2>&1; then
		echo "Mango is already running at PID $PID"
		exit 2
	fi
	# Clean up old PID file
	rm -f "$MA_HOME"/bin/ma.pid
fi

# This will ensure that the logs are written to the correct directories.
cd "$MA_HOME"

#Create a logs directory if it doesn't exist
if [ ! -d "$MA_HOME"/logs ]; then
	mkdir "$MA_HOME"/logs
fi

# Determine the Java home
if [ -d "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
    EXECJAVA="$JAVA_HOME/bin/java"
elif [ -x "$(command -v java)" ]; then
    EXECJAVA=java
else
	echo "JAVA_HOME not set and java not found on path"
	exit 3
fi

# Check for core upgrade
for f in "$MA_HOME"/m2m2-core-*.zip; do
	if [ -r "$f" ]; then
		echo 'Upgrading core...'

		# Delete jars and work dir
		rm -f "$MA_HOME"/lib/*.jar
		rm -rf "$MA_HOME"/work

		# Delete the release properties files
		rm -f "$MA_HOME"/release.properties
		rm -f "$MA_HOME"/release.signed

		# Unzip core. The exact name is unknown, but there should only be one, so iterate
		unzip -o "$f"
	    rm "$f"

		chmod +x "$MA_HOME"/bin/*.sh
		chmod +x "$MA_HOME"/bin/ext-available/*.sh
	fi
done

# Delete Range.class if it exists
if [ -e "$MA_HOME"/classes/org/jfree/data/Range.class ]; then
	rm -f "$MA_HOME"/classes/org/jfree/data/Range.class
fi

# Construct the Java classpath
MA_CP="$MA_HOME/lib/*"

# Only log error messages to stdout and dont include the date
# Avoids duplicating too much information in the systemd log and syslog
MA_LOG4J_STDOUT_LEVEL=error
MA_LOG4J_STDOUT_PATTERN='%-5p (%C.%M:%L) - %m %n'

if [ -e "$MA_HOME/overrides/start-options.sh" ]; then
	. "$MA_HOME/overrides/start-options.sh"
fi

if [ -n "$MA_JAVA_OPTS" ]; then
	echo "Starting Mango Automation with options '$MA_JAVA_OPTS'"
else
	echo "Starting Mango Automation"
fi

export MA_LOG4J_STDOUT_LEVEL
export MA_LOG4J_STDOUT_PATTERN

CLASSPATH="$MA_CP" \
"$EXECJAVA" $MA_JAVA_OPTS -server \
	"-Dma.home=$MA_HOME" \
	com.serotonin.m2m2.Main &

PID=$!
echo $PID > "$MA_HOME"/bin/ma.pid
echo "Mango Automation started with process ID: " $PID

exit 0
