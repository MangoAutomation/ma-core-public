#!/bin/sh

#
# Copyright (C) 2021 Radix IoT LLC. All rights reserved.
# @author Jared Wiltshire
#

set -e

err() {
  echo "ERROR: $*" >&2
  exit 1
}

# function for getting values from env.properties file
# does not support multiline properties or properties containing = sign
get_prop() {
  awk -v name="$1" -v dval="$2" -F = '$1==name{print $2; f=1; exit} END{if(f!=1){print dval}}' "$mango_config"
}

is_relative() {
  case "$1" in /*) return 1 ;; esac
}

resolve_path() {
  if is_relative "$2"; then
    printf %s/ "$1"
  fi
  printf %s "$2"
}

mango_keystore_properties() {
  if [ -z "$MA_KEYSTORE" ]; then
    keystore="$(get_prop "ssl.keystore.location" "$mango_paths_home/overrides/keystore.p12")"
    MA_KEYSTORE="$(resolve_path "$mango_paths_home" "$keystore")"
  fi

  if [ -z "$MA_KEYSTORE_PASSWORD" ]; then
    MA_KEYSTORE_PASSWORD="$(get_prop 'ssl.keystore.password' 'freetextpassword')"
  fi

  if [ -z "$MA_KEY_PASSWORD" ]; then
    MA_KEY_PASSWORD="$(get_prop 'ssl.key.password' "$MA_KEYSTORE_PASSWORD")"
  fi

  if [ -z "$MA_KEY_ALIAS" ]; then
    MA_KEY_ALIAS=mango
  fi
}

mango_unzip() {
  file="$1"
  if [ ! -x "$(command -v unzip)" ]; then
    unzip -q -o "$file"
  elif [ -x "$jar_cmd" ]; then
    "$jar_cmd" xf "$file"
  else
    err "Can't find command to extract zip file, please install unzip"
  fi
}

mango_stop() {
  SIGNAL="$1"
  [ -z "$SIGNAL" ] && SIGNAL=TERM

  if [ -e "$mango_pid_file" ]; then
    PID="$(cat "$mango_pid_file")"
    echo "Stopping Mango (PID $PID), sending signal $SIGNAL"

    while kill -"$SIGNAL" "$PID" >/dev/null 2>&1; do
      sleep 1
    done

    # Clean up PID file
    rm -f "$mango_pid_file"
  else
    err "Mango PID file $mango_pid_file does not exist, did you start Mango using start-mango.sh?"
  fi
}

if [ -z "$mango_script_dir" ]; then
  err "This script is not intended to be executed directly"
fi

# Locate JAVA_HOME and the java tools
if [ -d "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
  java_cmd="$JAVA_HOME/bin/java"
elif [ -x "$(command -v java)" ]; then
  java_cmd=java
else
  err "java not found on path, please set JAVA_HOME"
fi

if [ -d "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/keytool" ]; then
  keytool_cmd="$JAVA_HOME/bin/keytool"
elif [ -x "$(command -v keytool)" ]; then
  keytool_cmd=keytool
fi

if [ -d "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/jar" ]; then
  jar_cmd="$JAVA_HOME/bin/jar"
elif [ -x "$(command -v jar)" ]; then
  jar_cmd=jar
fi

# Mango home search path:
# $mango_paths_home
# $MA_HOME
# Directory up from where this script is located
[ -z "$mango_paths_home" ] && mango_paths_home="$MA_HOME"
[ -z "$mango_paths_home" ] && mango_paths_home="$(dirname -- "$mango_script_dir")"
if [ ! -e "$mango_paths_home/release.signed" ] && [ ! -e "$mango_paths_home/release.properties" ]; then
  err "Can't find Mango installation/home directory, please set mango_paths_home or MA_HOME"
fi

# Config file (env.properties) search path:
# $mango_config
# $MA_ENV_PROPERTIES
# $MA_HOME/overrides/properties/env.properties
# $MA_HOME/env.properties
[ -z "$mango_config" ] && mango_config="$MA_ENV_PROPERTIES"
[ -z "$mango_config" ] && mango_config="$mango_paths_home/overrides/properties/env.properties"
[ -z "$mango_config" ] && mango_config="$mango_paths_home/env.properties"
if [ ! -e "$mango_config" ]; then
  err "Can't find Mango config (env.properties), please set mango_config or MA_ENV_PROPERTIES"
fi

mango_pid_file="$mango_paths_home/bin/ma.pid"
# TODO locate data path
# TODO pid file location
# TODO start options location
# TODO default keystore location
# TODO Modify get_prop to accept an env variable argument and check the env arg first