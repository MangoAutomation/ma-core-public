#!/bin/sh

set -e
umask 077

# function for getting values from .properties file
getProp() {
  awk -F "=" "/^$2=/ {print "'$2'"; exit; }" "$1"
}

if [ -z "$MA_HOME" ]
then
    script_file=$(readlink -f "$0")
    script_dir=$(dirname "$script_file")
    possible_ma_home=$(dirname "$script_dir")
    if [ -e "$possible_ma_home/release.signed" ]
    then
        MA_HOME="$possible_ma_home"
    else
        MA_HOME=/opt/mango
    fi
fi

if [ -z "$MA_ENV_PROPERTIES" ]
then
    MA_ENV_PROPERTIES="$MA_HOME/overrides/properties/env.properties"
fi

if [ -z "$MA_KEYSTORE" ]
then
    if [ -e "$MA_ENV_PROPERTIES" ]
    then
        MA_KEYSTORE=$(getProp "$MA_ENV_PROPERTIES" "ssl.keystore.location")
    fi

    if [ -z "$MA_KEYSTORE" ]
	then
	    MA_KEYSTORE="$MA_HOME/overrides/keystore.jks"
	fi
fi

if [ -z "$MA_KEYSTORE_PASSWORD" ]
then
    if [ -e "$MA_ENV_PROPERTIES" ]
    then
        MA_KEYSTORE_PASSWORD=$(getProp "$MA_ENV_PROPERTIES" "ssl.keystore.password")
    fi

    if [ -z "$MA_KEYSTORE_PASSWORD" ]
    then
        MA_KEYSTORE_PASSWORD=freetextpassword
    fi
fi

if [ -z "$MA_KEY_PASSWORD" ]
then
    if [ -e "$MA_ENV_PROPERTIES" ]
    then
        MA_KEY_PASSWORD=$(getProp "$MA_ENV_PROPERTIES" "ssl.key.password")
    fi

    if [ -z "$MA_KEY_PASSWORD" ]
    then
        MA_KEY_PASSWORD="$MA_KEYSTORE_PASSWORD"
    fi
fi

if [ -z "$MA_KEY_ALIAS" ]
then
    MA_KEY_ALIAS=mango
fi

keytool_cmd="$JAVA_HOME/bin/keytool"
if [ -z "$JAVA_HOME" ] || [ ! -x "$keytool_cmd" ]
then
    # use keytool from PATH
    keytool_cmd=keytool
fi

if [ "$1" = "genkey" ]
then
    "$keytool_cmd" -genkey -noprompt -alias "$MA_KEY_ALIAS" -dname "CN=$(hostname)" -keystore "$MA_KEYSTORE" -storepass "$MA_KEYSTORE_PASSWORD" -keypass "$MA_KEY_PASSWORD"

    # ensure user read only permission
    chmod 400 "$MA_KEYSTORE"

    exit 0
fi

openssl pkcs12 -export \
    -in "$RENEWED_LINEAGE/fullchain.pem" -inkey "$RENEWED_LINEAGE/privkey.pem" \
    -out "$RENEWED_LINEAGE/keystore.p12" -name "$MA_KEY_ALIAS" -passout pass:"$MA_KEYSTORE_PASSWORD"

"$keytool_cmd" -importkeystore -noprompt \
    -srckeystore "$RENEWED_LINEAGE/keystore.p12" -srcstoretype PKCS12 -srcstorepass "$MA_KEYSTORE_PASSWORD" \
    -srcalias "$MA_KEY_ALIAS" \
    -destkeystore "$RENEWED_LINEAGE/keystore.jks" -deststorepass "$MA_KEYSTORE_PASSWORD" \
    -destkeypass "$MA_KEY_PASSWORD" -destalias "$MA_KEY_ALIAS" \
    2> /dev/null

# copy keystore to final destination, dont change destination ownership
cp "$RENEWED_LINEAGE/keystore.jks" "$MA_KEYSTORE"

# ensure user read only permission
chmod 400 "$MA_KEYSTORE"

# remove interim files
rm -f "$RENEWED_LINEAGE/keystore.p12" "$RENEWED_LINEAGE/keystore.jks"
