#!/bin/sh

#
# Copyright (C) 2021 Radix IoT LLC. All rights reserved.
# @author Jared Wiltshire
# @author Matthew Lohbihler
#

set -e

# set options from arguments
for arg in "$@"; do
  case "$arg" in
  wait) mango_wait=true ;;
  esac
done

mango_script_dir="$(cd "$(dirname "$0")" && pwd -P)"
. "$mango_script_dir"/getenv.sh

echo "Mango installation/home directory is $mango_paths_home"

pid_file="$mango_paths_home/bin/ma.pid"
if [ -e "$pid_file" ]; then
  PID="$(cat "$pid_file")"
  if ps -p "$PID" >/dev/null 2>&1; then
    err "Mango is already running with PID $PID"
  fi
  # Clean up old PID file
  rm -f "$pid_file"
fi

# Set the working directory to the Mango installation/home directory
cd "$mango_paths_home"

# Check for core upgrade
for f in "$mango_paths_home"/m2m2-core-*.zip; do
  if [ -r "$f" ]; then
    echo "Upgrading Mango installation from zip file $f"

    # Delete jars and work dir
    rm -f "$mango_paths_home"/lib/*.jar
    rm -rf "$mango_paths_home"/work

    # Delete the release properties files
    rm -f "$mango_paths_home"/release.properties
    rm -f "$mango_paths_home"/release.signed

    # Unzip core. The exact name is unknown, but there should only be one, so iterate
    mango_unzip "$f" "$mango_paths_home"
    rm -f "$f"

    chmod +x "$mango_paths_home"/bin/*.sh
  fi
done

# Construct the Java classpath
MA_CP="$mango_paths_home/lib/*"

if [ -e "$mango_paths_home/overrides/start-options.sh" ]; then
  . "$mango_paths_home/overrides/start-options.sh"
fi

if [ -n "$MA_JAVA_OPTS" ]; then
  echo "Starting Mango with options '$MA_JAVA_OPTS'"
else
  echo "Starting Mango"
fi

CLASSPATH="$MA_CP" \
  "$java_cmd" $MA_JAVA_OPTS -server \
  com.serotonin.m2m2.Main &

PID=$!
echo $PID >"$pid_file"
echo "Mango started (PID $PID)"

if [ "$mango_wait" = true ]; then
  # trap the SIGINT signal (Ctrl-C) and stop mango
  trap mango_stop INT TERM
  # needed for trap to work
  set +e
  # wait for Mango to exit
  wait $PID
fi

exit 0
