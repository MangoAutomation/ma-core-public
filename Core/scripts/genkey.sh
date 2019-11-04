#!/bin/bash

set -e
umask 077

[ -x "$(command -v greadlink)" ] && alias readlink=greadlink
script_dir="$(dirname "$(readlink -f "$0")")"

source "$script_dir"/getenv.sh

"$keytool_cmd" -genkey -noprompt -keyalg RSA -keysize 2048 -alias "$MA_KEY_ALIAS" -dname "CN=$(hostname)" -keystore "$MA_KEYSTORE" -storepass "$MA_KEYSTORE_PASSWORD" -keypass "$MA_KEY_PASSWORD"

# ensure user read only permission
chmod 400 "$MA_KEYSTORE"
