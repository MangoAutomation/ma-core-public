#!/bin/bash

set -e

# Prompts the user for input
prompt() {
	read -r -p "$1 [$2]: " result
	[ -z "$result" ] && result="$2"
	echo "$result"
}

# Set default environment variables
[ -z "$MA_DB_NAME" ] && MA_DB_NAME=mango
[ -z "$MA_DB_USER" ] && MA_DB_USER=mango
[ -z "$MA_DB_PASSWORD" ] && MA_DB_PASSWORD="$(openssl rand -base64 24)"

if [ -z "$MA_HOME" ]; then
	MA_HOME=$(prompt 'Where should we install Mango?' '/opt/mango')
fi

# Create the MA_HOME directory if it does not exist
if [ ! -d "$MA_HOME" ]; then
	mkdir "$MA_HOME"
	echo "Created MA_HOME directory '$MA_HOME'."
fi

# Create the Mango user if it does not exist
[ -z "$MA_USER" ] && MA_USER=mango
if [ ! "$(id -u "$MA_USER")" ]; then
	NO_LOGIN_SHELL="$(command -v nologin)"
	[ ! -x "$NO_LOGIN_SHELL" ] NO_LOGIN_SHELL=/bin/false
	
	USER_ADD_CMD="$(command -v useradd)"
	if [ ! -x "$USER_ADD_CMD" ]; then
		echo "Can't create user '$MA_USER' as useradd command does not exist."
		exit 1;
	fi

	"$USER_ADD_CMD" --system --no-create-home --home-dir "$MA_HOME" --shell "$NO_LOGIN_SHELL" --comment 'Mango Automation' "$MA_USER"
	echo "Created user '$MA_USER'."
fi
[ -z "$MA_GROUP" ] && MA_GROUP="$(id -gn "$MA_USER")"

if [ ! -f "$MA_CORE_ZIP" ] && [ -z "$MA_VERSION" ]; then
	MA_VERSION=$(prompt 'What version of Mango should we install?' '3.6.5')
fi

while [[ "$MA_DB_TYPE" != 'mysql' ]] && [[ "$MA_DB_TYPE" != 'h2' ]]; do
	MA_DB_TYPE=$(prompt 'What type of SQL database?' 'mysql')
done

while [[ "$MA_CONFIRM" != 'yes' ]]; do
	MA_CONFIRM=$(prompt "Entire contents of '$MA_HOME' will be deleted and SQL database '$MA_DB_NAME' will be dropped. Proceed?" 'no')
done

# Stop and remove any existing mango service
if [ -x "$(command -v systemctl)" ]; then
	systemctl stop mango || true
	systemctl disable mango || true
fi

if [[ "$MA_DB_TYPE" = 'mysql' ]]; then
	# Drop database tables and user, create new user and table
	echo "DROP DATABASE $MA_DB_NAME;
	DROP USER '$MA_DB_USER'@'localhost';
	CREATE DATABASE $MA_DB_NAME;
	CREATE USER '$MA_DB_USER'@'localhost' IDENTIFIED BY '$MA_DB_PASSWORD';
	GRANT ALL ON $MA_DB_NAME.* TO '$MA_DB_USER'@'localhost';" | mysql -u root
fi

# Remove any old files in MA_HOME
rm -rf "${MA_HOME:?}"/*
rm -f "${MA_HOME:?}"/.ma

# Download and extract the Mango enterprise archive
if [ ! -f "$MA_CORE_ZIP" ]; then
	MA_CORE_ZIP=$(mktemp)
	curl https://store.infiniteautomation.com/downloads/fullCores/enterprise-m2m2-core-"$MA_VERSION".zip > "$MA_CORE_ZIP"
	MA_DELETE_ZIP=1
fi
unzip "$MA_CORE_ZIP" -d "$MA_HOME"
[ $MA_DELETE_ZIP ] && rm -f "$MA_CORE_ZIP"

# Create an overrides env.properties file
MA_ENV_FILE="$MA_HOME"/overrides/properties/env.properties
if [[ "$MA_DB_TYPE" = 'mysql' ]]; then
	echo "db.url=jdbc:mysql://localhost/$MA_DB_NAME?useSSL=false" > "MA_ENV_FILE"
elif [[ "$MA_DB_TYPE" = 'h2' ]]; then
	echo 'db.url=jdbc:h2:${ma.home}/databases/'"$MA_DB_NAME" > "MA_ENV_FILE"
else
	echo "Unknown database type $MA_DB_TYPE"
	exit 2;
fi

echo "db.type=$MA_DB_TYPE
db.username=$MA_DB_USER
db.password=$MA_DB_PASSWORD
web.openBrowserOnStartup=false
ssl.on=true
ssl.keystore.location=$MA_HOME/overrides/keystore.jks
ssl.keystore.password=$(openssl rand -base64 24)" >> "$MA_ENV_FILE"

# Used to download updated scripts from git main branch
get-script() {
	curl -s "https://raw.githubusercontent.com/infiniteautomation/ma-core-public/main/Core/scripts/$1" > "$MA_HOME/bin/$1"
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

# create a new systemd service file in overrides
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

# Stop and remove any existing mango service
if [ -x "$(command -v systemctl)" ] && [ -d /etc/systemd/system ]; then
	# link the new service file into /etc
	ln -sf "$MA_HOME"/overrides/mango.service /etc/systemd/system/mango.service

	# enable the sytemd service (but dont start Mango)
	systemctl enable mango
	echo "Mango was installed successfully. Type 'systemctl start mango' to start Mango."
else
	echo "Mango was installed successfully. Type 'sudo -u $MA_USER $MA_HOME/bin/ma-start-systemd.sh' to start Mango. (systemd is not available and Mango will not start on boot)"
fi
