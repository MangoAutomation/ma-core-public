#!/bin/bash

set -e

# Prompts the user for input
prompt() {
	read -p "$1 [$2]: " result
	[ -z "$result" ] && result="$2"
	echo "$result"
}

# Set default environment variables
[ -z "$MA_DB_TABLE" ] && MA_DB_TABLE=mango
[ -z "$MA_DB_USER" ] && MA_DB_USER=mango
[ -z "$MA_DB_PASSWORD" ] && MA_DB_PASSWORD=$(openssl rand -base64 24)
[ -z "$MA_USER" ] && MA_USER=mango
[ -z "$MA_GROUP" ] && MA_GROUP=$(id -gn "$MA_USER")

while [ -z "$MA_HOME" ]; do
	MA_HOME=$(prompt 'Where should we install Mango?' '/opt/mango')
	if [ ! -d "$MA_HOME" ]; then
		echo 'Invalid directory specified for MA_HOME'
		MA_HOME=''
	fi
done

while [ -z "$MA_VERSION" ]; do
	MA_VERSION=$(prompt 'What version of Mango should we install?' '3.6.5')
done

while [[ "$MA_CONFIRM" != 'y' ]]; do
	MA_CONFIRM=$(prompt "Entire contents of '$MA_HOME' will be deleted and MySQL table '$MA_DB_TABLE' will be dropped. Proceed?" 'n')
done

# Stop and remove any existing mango service
systemctl stop mango || true
systemctl disable mango || true

# Drop database tables and user, create new user and table
echo "DROP DATABASE $MA_DB_TABLE;
DROP USER '$MA_DB_USER'@'localhost';
CREATE DATABASE $MA_DB_TABLE;
CREATE USER '$MA_DB_USER'@'localhost' IDENTIFIED BY '$MA_DB_PASSWORD';
GRANT ALL ON $MA_DB_TABLE.* TO '$MA_DB_USER'@'localhost';" | mysql -u root

# Remove any old files in MA_HOME
rm -rf "$MA_HOME"/*
rm -f "$MA_HOME"/.ma

# Download and extract the Mango enterprise archive
MA_TMP_ZIP=$(mktemp)
wget -O "$MA_TMP_ZIP" https://store.infiniteautomation.com/downloads/fullCores/enterprise-m2m2-core-"$MA_VERSION".zip
unzip "$MA_TMP_ZIP" -d "$MA_HOME"
rm -f "$MA_TMP_ZIP"

# Create an overrides env.properties file
echo "web.openBrowserOnStartup=false
db.type=mysql
db.url=jdbc:mysql://localhost/$MA_DB_TABLE?useSSL=false
db.username=$MA_DB_USER
db.password=$MA_DB_PASSWORD
ssl.on=true
ssl.keystore.location=$MA_HOME/overrides/keystore.jks
ssl.keystore.password=$(openssl rand -base64 24)" > "$MA_HOME"/overrides/properties/env.properties

# Used to download updated scripts from git main branch
get-script() {
	wget -q -O "$MA_HOME/bin/$1" "https://raw.githubusercontent.com/infiniteautomation/ma-core-public/main/Core/scripts/$1"
}

get-script ma-start-systemd.sh
get-script mango.service
get-script getenv.sh
get-script genkey.sh
get-script certbot-deploy.sh

chmod +x "$MA_HOME"/bin/*.sh

# generate a default self signed SSL/TLS certificate
"$MA_HOME"/bin/genkey.sh

chown -R "$MA_USER":"$MA_GROUP" "$MA_HOME"

# create a new service file in overrides
echo "[Unit]
Description=Mango Automation
After=mysqld.service
StartLimitIntervalSec=0

[Service]
EnvironmentFile=/etc/environment
EnvironmentFile=-$MA_HOME/overrides/environment
Type=forking
WorkingDirectory=$MA_HOME
PIDFile=$MA_HOME/bin/ma.pid
ExecStart=$MA_HOME/bin/ma-start-systemd.sh
SuccessExitStatus=0 SIGINT SIGTERM 130 143
Restart=always
RestartSec=5s
User=mango
NoNewPrivileges=true

[Install]
WantedBy=multi-user.target" > "$MA_HOME"/overrides/mango.service

# link the new service file into /etc
ln -sf "$MA_HOME"/overrides/mango.service /etc/systemd/system/mango.service

# enable the sytemd service (but dont start Mango)
systemctl enable mango
