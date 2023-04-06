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

# function for getting values from config file
# does not support multiline properties or properties containing = sign
get_prop() {
  if [ -n "$mango_config" ] && [ -f "$mango_config" ]; then
    awk -v name="$1" -v dval="$2" -F = '$1==name{print $2; f=1; exit} END{if(f!=1){print dval}}' "$mango_config"
  else
    printf %s "$2"
  fi
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
    keystoreMessage="keystore variable populated with"
    keystoreDefault="keystore.p12"
    keystoreProperty="ssl.keystore.location"
    keystore="$(get_prop $keystoreProperty $keystoreDefault)"
    print_value_source_message "$keystore" "$keystoreDefault" "$keystoreMessage" "$keystoreProperty"
    MA_KEYSTORE="$(resolve_path "$mango_paths_data" "$keystore")"
    print_ma_keystore_value_source_message "$MA_KEYSTORE" "$keystore" "MA_KEYSTORE environment variable populated with"
  fi

  if [ -z "$MA_KEYSTORE_PASSWORD" ]; then
    maKeystoreMessage="MA_KEYSTORE_PASSWORD environment variable populated with"
    maKeystorePasswordDefault="freetextpassword"
    maKeystorePasswordProperty="ssl.keystore.password"
    MA_KEYSTORE_PASSWORD="$(get_prop $maKeystorePasswordProperty maKeystorePasswordDefault)"
    print_value_source_message "$MA_KEYSTORE_PASSWORD" "$maKeystorePasswordDefault" "$maKeystoreMessage" "$maKeystorePasswordProperty"
  fi

  if [ -z "$MA_KEY_PASSWORD" ]; then
    maKeyMessage="MA_KEY_PASSWORD environment variable populated with"
    maKeyPasswordProperty="ssl.key.password"
    MA_KEY_PASSWORD="$(get_prop "$maKeyPasswordProperty" "$MA_KEYSTORE_PASSWORD")"
    print_value_source_message "$MA_KEY_PASSWORD" "$MA_KEYSTORE_PASSWORD" "$maKeyMessage" "$maKeyPasswordProperty"
  fi

  if [ -z "$MA_KEY_ALIAS" ]; then
    MA_KEY_ALIAS=mango
    echo "+++++ MA_KEY_ALIAS environment variable populated with default value mango"
  fi
}

mango_ensure_unzip() {
  if [ ! -x "$(command -v unzip)" ] && [ ! -x "$jar_cmd" ]; then
    err "Can't find command to extract zip file, please install unzip"
  fi
}

mango_unzip() {
  file="$1"
  if [ -x "$(command -v unzip)" ]; then
    unzip -q -o "$file"
  elif [ -x "$jar_cmd" ]; then
    "$jar_cmd" xf "$file"
  else
    err "Can't find command to extract zip file, please install unzip"
  fi
}

mango_upgrade() {
  # Unzip core. The exact name is unknown, but there should only be one, so iterate
  for f in "$mango_paths_home"/m2m2-core-*.zip; do
    if [ -r "$f" ]; then
      echo "Upgrading Mango installation from zip file $f"
      # ensure unzip is available before deleting anything
      mango_ensure_unzip

      # Delete jars and work dir, some old jars may not be included in a new zip and we dont want them on the classpath
      rm -f "$mango_paths_home"/lib/*.jar

      # Delete the release files in case we move from unsigned to signed or vice versa
      rm -f "$mango_paths_home"/release.properties
      rm -f "$mango_paths_home"/release.signed

      mango_unzip "$f" "$mango_paths_home"
      rm -f "$f"

      chmod +x "$mango_paths_home"/bin/*.sh
    fi
  done
}

mango_start() {
  echo "Mango installation/home directory is $mango_paths_home"
  echo "Mango data path is $mango_paths_data"

  if [ -f "$mango_paths_pid_file" ]; then
    mango_pid="$(cat "$mango_paths_pid_file")"
    if ps -p "$mango_pid" >/dev/null 2>&1; then
      err "Mango is already running with PID $mango_pid"
    fi
    # Clean up old PID file
    rm -f "$mango_paths_pid_file"
  fi

  # Set the working directory to the Mango installation/home directory
  cd "$mango_paths_home"

  # Check for core upgrade
  mango_upgrade

  # Construct the Java classpath
  MA_CP="$mango_paths_home/lib/*"

  if [ -f "$mango_paths_start_options" ]; then
    # shellcheck source=start_options.sh
    . "$mango_paths_start_options"
  fi

  if [ -n "$MA_JAVA_OPTS" ]; then
    echo "Starting Mango with options '$MA_JAVA_OPTS'"
  else
    echo "Starting Mango"
  fi

  CLASSPATH="$MA_CP" \
    "$java_cmd" $MA_JAVA_OPTS -server \
    com.serotonin.m2m2.Main &

  mango_pid=$!
  echo $mango_pid >"$mango_paths_pid_file"
  echo "Mango started (PID $mango_pid)"
}

mango_stop() {
  SIGNAL="$1"
  [ -z "$SIGNAL" ] && SIGNAL=TERM

  if [ -z "$mango_pid" ]; then
    if [ -f "$mango_paths_pid_file" ]; then
      mango_pid="$(cat "$mango_paths_pid_file")"
    else
      err "Mango PID file $mango_paths_pid_file does not exist, did you start Mango using start-mango.sh?"
    fi
  fi

  echo "Stopping Mango (PID $mango_pid), sending signal $SIGNAL"
  while kill -"$SIGNAL" "$mango_pid" >/dev/null 2>&1; do
    sleep 1
  done

  # Clean up PID file
  rm -f "$mango_paths_pid_file"
}

print_value_source_message() {
  if [ "$1" = "$2" ]; then
    echo "++ $3 mango.properties file property $4"
  else
    echo "++++++ $3 default value $2"
  fi
}

print_ma_keystore_value_source_message() {
  if [ "$1" = "$2" ]; then
    echo "++ MA_KEYSTORE used $1"
  else
    echo "++++++ $3 default value $2 as file $1 was not found."
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
  # shellcheck disable=SC2034
  keytool_cmd=keytool
fi

if [ -d "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/jar" ]; then
  jar_cmd="$JAVA_HOME/bin/jar"
elif [ -x "$(command -v jar)" ]; then
  jar_cmd=jar
fi

# Mango home search path:
# $mango_paths_home
# $MA_HOME (legacy environment variable)
# Directory up from where this script is located (fallback)
[ -z "$mango_paths_home" ] && mango_paths_home="$MA_HOME"
[ -z "$mango_paths_home" ] && mango_paths_home="$(dirname -- "$mango_script_dir")"
if [ ! -f "$mango_paths_home/release.signed" ] && [ ! -f "$mango_paths_home/release.properties" ]; then
  err "Can't find Mango installation/home directory, please set mango_paths_home or MA_HOME"
fi

# Config file (mango.properties) search path:
# $mango_config
# $MA_ENV_PROPERTIES (legacy environment variable)
# $mango_paths_data/mango.properties
# $mango_paths_data/env.properties
# ~/mango.properties
# $mango_paths_home/env.properties (legacy location)
# $mango_paths_home/overrides/properties/env.properties (legacy location)
if [ -z "$mango_config" ]; then
  if [ -n "$MA_ENV_PROPERTIES" ]; then
    mango_config="$MA_ENV_PROPERTIES"
  elif [ -n "$mango_paths_data" ] && [ -f "$mango_paths_data/mango.properties" ]; then
    mango_config="$mango_paths_data/mango.properties"
  elif [ -n "$mango_paths_data" ] && [ -f "$mango_paths_data/env.properties" ]; then
    mango_config="$mango_paths_data/env.properties"
  elif [ -n "$HOME" ] && [ -f "$HOME/mango.properties" ]; then
    mango_config="$HOME/mango.properties"
  elif [ -f "$mango_paths_home/env.properties" ]; then
    mango_config="$mango_paths_home/env.properties"
  elif [ -f "$mango_paths_home/overrides/properties/env.properties" ]; then
    mango_config="$mango_paths_home/overrides/properties/env.properties"
  fi
fi

[ -z "$mango_paths_data" ] && mango_paths_data="$(get_prop "paths.data" "$mango_paths_home")"
mango_paths_data="$(resolve_path "$mango_paths_home" "$mango_paths_data")"
[ -z "$mango_paths_pid_file" ] && mango_paths_pid_file="$(get_prop "paths.pid.file" "$mango_paths_data/ma.pid")"
mango_paths_pid_file="$(resolve_path "$mango_paths_data" "$mango_paths_pid_file")"
[ -z "$mango_paths_start_options" ] && mango_paths_start_options="$(get_prop "paths.start.options" "$mango_paths_data/start-options.sh")"
mango_paths_start_options="$(resolve_path "$mango_paths_data" "$mango_paths_start_options")"

echo "++ HOME environment variable is $HOME"
echo "++ mango_paths_home is $mango_paths_home"
if [ "$HOME" != "$mango_paths_home" ]; then
  echo "+++++ HOME and mango_paths_home are not the same so script may not work properly!! ++++"
fi
echo "+++++ Mango configuration found on $mango_config"
echo "++ mango_script_dir is $mango_script_dir"
echo "++ mango_paths_data is $mango_paths_data"
