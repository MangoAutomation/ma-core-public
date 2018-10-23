#!/bin/sh

set -e
umask 077

# function for getting values from .properties file
getProp() {
  awk -F "=" "/^$2=/ {print "'$2'"; exit; }" "$1"
}

if [ -z "$MA_HOME" ]
then
    MA_HOME=/opt/mango
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
    else
        MA_KEYSTORE="$MA_HOME/overrides/keystore.jks"
    fi
fi

if [ -z "$MA_KEYSTORE_PASSWORD" ]
then
    if [ -e "$MA_ENV_PROPERTIES" ]
    then
        MA_KEYSTORE_PASSWORD=$(getProp "$MA_ENV_PROPERTIES" "ssl.keystore.password")
    else
        MA_KEYSTORE_PASSWORD=password
    fi
fi

keytool_cmd="$JAVA_HOME/bin/keytool"
if [ -z "$JAVA_HOME" ] || [ ! -x "$keytool_cmd" ]
then
    # use keytool from PATH
    keytool_cmd=keytool
fi

if [ -z "$MA_USER" ]
then
    MA_USER=mango
fi

if [ -z "$MA_GROUP" ]
then
    MA_GROUP=mango
fi

openssl pkcs12 -export \
    -out "$RENEWED_LINEAGE/keystore.p12" -passout pass:"$MA_KEYSTORE_PASSWORD" \
    -in "$RENEWED_LINEAGE/fullchain.pem" -inkey "$RENEWED_LINEAGE/privkey.pem"

"$keytool_cmd" -importkeystore -noprompt \
    -srckeystore "$RENEWED_LINEAGE/keystore.p12" -srcstoretype PKCS12 -srcstorepass "$MA_KEYSTORE_PASSWORD" \
    -destkeystore "$RENEWED_LINEAGE/keystore.jks" -deststorepass "$MA_KEYSTORE_PASSWORD" \
    2> /dev/null

# remove interim file
rm -f "$RENEWED_LINEAGE/keystore.p12"

# set user read only permission
chmod 400 "$RENEWED_LINEAGE/keystore.jks"
# change owner so mango can access it
chown "$MA_USER":"$MA_GROUP" "$RENEWED_LINEAGE/keystore.jks"

mv -f "$RENEWED_LINEAGE/keystore.jks" "$MA_KEYSTORE"
