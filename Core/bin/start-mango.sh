#!/bin/sh

#
# Copyright (C) 2019 Infinite Automation Systems Inc. All rights reserved.
# @author Jared Wiltshire
# @author Matthew Lohbihler
#

set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd -P)"
OPTIONS="$1"

# Only set MA_HOME if not already set
[ -z "$MA_HOME" ] && MA_HOME="$(dirname -- "$SCRIPT_DIR")"

if [ ! -d "$MA_HOME" ]; then
    echo 'Error: MA_HOME is not set or is not a directory'
    exit 1
fi

echo "MA_HOME is $MA_HOME"

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

# Determine the Java home
if [ -d "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
    EXECJAVA="$JAVA_HOME/bin/java"
elif [ -x "$(command -v java)" ]; then
    EXECJAVA=java
else
	echo "JAVA_HOME not set and java not found on path"
	exit 3
fi

jar_cmd="$(command -v jar)" || true
[ -d "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/jar" ] && jar_cmd="$JAVA_HOME/bin/jar"
mango_unzip() {
  file="$1"
  if [ ! -x "$(command -v unzip)" ]; then
    unzip -q -o "$file"
  elif [ -x "$jar_cmd" ]; then
    "$jar_cmd" xf "$file"
  else
    echo "Can't find command to extract zip file, please install unzip"
    exit 3
  fi
}

# Check for core upgrade
for f in "$MA_HOME"/m2m2-core-*.zip; do
	if [ -r "$f" ]; then
		echo "Upgrading Mango installation from zip file $f"

		# Delete jars and work dir
		rm -f "$MA_HOME"/lib/*.jar
		rm -rf "$MA_HOME"/work

		# Delete the release properties files
		rm -f "$MA_HOME"/release.properties
		rm -f "$MA_HOME"/release.signed

		# Unzip core. The exact name is unknown, but there should only be one, so iterate
		mango_unzip "$f" "."
    rm -f "$f"

		chmod +x "$MA_HOME"/bin/*.sh
	fi
done

# Construct the Java classpath
MA_CP="$MA_HOME/lib/*"

if [ -e "$MA_HOME/overrides/start-options.sh" ]; then
	. "$MA_HOME/overrides/start-options.sh"
fi

if [ -n "$MA_JAVA_OPTS" ]; then
	echo "Starting Mango with options '$MA_JAVA_OPTS'"
else
	echo "Starting Mango"
fi

CLASSPATH="$MA_CP" \
"$EXECJAVA" $MA_JAVA_OPTS -server \
	"-Dmango.paths.home=$MA_HOME" \
	com.serotonin.m2m2.Main &

PID=$!
echo $PID > "$MA_HOME"/bin/ma.pid
echo "Mango started with process ID: $PID"

if [ "$OPTIONS" = 'wait' ]; then
  # sends SIGTERM to Mango and waits for it to exit
  stop_mango() {
    echo "Stopping Mango with process ID: $PID"
    kill $PID
    wait $PID
  }
  # trap the SIGINT signal (Ctrl-C) and stop mango
  trap stop_mango INT TERM
  # needed for trap to work
  set +e
  # wait for Mango to exit
  wait $PID
fi

exit 0
