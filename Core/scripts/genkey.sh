#!/bin/sh

set -e
umask 077

if [ -x "$(command -v greadlink)" ]
then
	alias readlink='greadlink'
fi

script_file=$(readlink -f "$0")
script_dir=$(dirname "$script_file")

source "$script_dir"/getenv.sh

"$keytool_cmd" -genkey -noprompt -alias "$MA_KEY_ALIAS" -dname "CN=$(hostname)" -keystore "$MA_KEYSTORE" -storepass "$MA_KEYSTORE_PASSWORD" -keypass "$MA_KEY_PASSWORD"

# ensure user read only permission
chmod 400 "$MA_KEYSTORE"
