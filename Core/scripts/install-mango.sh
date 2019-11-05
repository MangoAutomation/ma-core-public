#!/bin/sh

#
# Copyright (C) 2019 Infinite Automation Systems Inc. All rights reserved.
# @author Jared Wiltshire
#

set -e

SCRIPT="$0"
[ -x "$(command -v greadlink)" ] && alias readlink=greadlink
[ -x "$(command -v readlink)" ] && SCRIPT="$(readlink -f "$SCRIPT")"
SCRIPT_DIR="$(dirname "$SCRIPT")"

# Prompts the user for input, uses the second argument as the default
prompt() {
	printf "%s [%s]: " "$1" "$2" > /dev/tty
	read -r result
	[ -z "$result" ] && result="$2"
	printf "%s" "$result"
}

# Used to download updated scripts from git main branch
get_script() {
	curl -s "https://raw.githubusercontent.com/infiniteautomation/ma-core-public/main/Core/scripts/$1" > "$MA_HOME/bin/$1"
}

# Set default environment variables
[ -z "$MA_DB_NAME" ] && MA_DB_NAME=mango
[ -z "$MA_DB_USER" ] && MA_DB_USER=mango
[ -z "$MA_DB_PASSWORD" ] && MA_DB_PASSWORD="$(openssl rand -base64 24)"
[ -z "$MA_SERVICE_NAME" ] && MA_SERVICE_NAME=mango

if [ -z "$MA_HOME" ]; then
	DEFAULT_HOME=/opt/mango
    CHECK_DIR="$(dirname "$SCRIPT_DIR")"
    if [ -e "$CHECK_DIR/release.signed" ] || [ -e "$CHECK_DIR/release.properties" ]; then
        DEFAULT_HOME="$CHECK_DIR"
    fi
    MA_HOME="$(prompt 'Where should we install Mango?' "$DEFAULT_HOME")"
fi

# Create the MA_HOME directory if it does not exist
if [ ! -d "$MA_HOME" ]; then
	mkdir "$MA_HOME"
	echo "Created MA_HOME directory '$MA_HOME'."
fi

# Create the Mango user if it does not exist
[ -z "$MA_USER" ] && MA_USER=mango
if [ ! "$(id -u "$MA_USER" 2> /dev/null)" ]; then
	NO_LOGIN_SHELL="$(command -v nologin)"
	[ ! -x "$NO_LOGIN_SHELL" ] && NO_LOGIN_SHELL=/bin/false
	
	USER_ADD_CMD="$(command -v useradd)"
	if [ ! -x "$USER_ADD_CMD" ]; then
		echo "Can't create user '$MA_USER' as useradd command does not exist."
		exit 1;
	fi

	"$USER_ADD_CMD" --system --no-create-home --home-dir "$MA_HOME" --shell "$NO_LOGIN_SHELL" --comment 'Mango Automation' "$MA_USER"
	echo "Created user '$MA_USER'."
fi
[ -z "$MA_GROUP" ] && MA_GROUP="$(id -gn "$MA_USER")"

while [ "$MA_DB_TYPE" != 'mysql' ] && [ "$MA_DB_TYPE" != 'h2' ]; do
	MA_DB_TYPE="$(prompt 'What type of SQL database?' 'h2')"
done

# Stop and remove any existing mango service
if [ -x "$(command -v systemctl)" ]; then
	systemctl stop "$MA_SERVICE_NAME" 2> /dev/null || true
	systemctl disable "$MA_SERVICE_NAME" 2> /dev/null || true
fi

while [ "$MA_CONFIRM_DROP" != 'yes' ] && [ "$MA_CONFIRM_DROP" != 'no' ]; do
	prompt_text="Drop SQL database '$MA_DB_NAME' if it exists?"
	MA_CONFIRM_DROP="$(prompt "$prompt_text" 'no')"
done
if [ "$MA_CONFIRM_DROP" = 'yes' ]; then
	if [ "$MA_DB_TYPE" = 'mysql' ]; then
		# Drop database tables and user, create new user and table
		echo "DROP DATABASE IF EXISTS $MA_DB_NAME;
		DROP USER IF EXISTS '$MA_DB_USER'@'localhost';" | mysql -u root
	elif [ "$MA_DB_TYPE" = 'h2' ]; then
		for db in "$MA_HOME/databases/$MA_DB_NAME".*.db; do
			rm -f "$db"
    	done
	fi
fi

if [ "$MA_DB_TYPE" = 'mysql' ]; then
	echo "CREATE DATABASE IF NOT EXISTS $MA_DB_NAME;
	CREATE USER IF NOT EXISTS '$MA_DB_USER'@'localhost' IDENTIFIED BY '$MA_DB_PASSWORD';
	GRANT ALL ON $MA_DB_NAME.* TO '$MA_DB_USER'@'localhost';" | mysql -u root
fi

# Remove any old files in MA_HOME
if [ "$(find "$MA_HOME" -mindepth 1 -maxdepth 1 ! -name 'databases')" ]; then
	while [ "$MA_CONFIRM_DELETE" != 'yes' ] && [ "$MA_CONFIRM_DELETE" != 'no' ]; do
		MA_CONFIRM_DELETE="$(prompt "Installion directory is not empty, delete all files in '$MA_HOME'?" 'no')"
	done
	
	if [ "$MA_CONFIRM_DELETE" = 'no' ]; then
		exit 1
	fi
	
	find "$MA_HOME" -mindepth 1 -maxdepth 1 ! -name 'databases' -exec rm -r '{}' \;
fi

if [ ! -f "$MA_CORE_ZIP" ] && [ -z "$MA_VERSION" ]; then
	MA_VERSION="$(prompt 'What version of Mango do you want to install?' '3.6.5')"
fi

# Download and extract the Mango enterprise archive
if [ ! -f "$MA_CORE_ZIP" ]; then
	MA_CORE_ZIP=$(mktemp)
	curl https://store.infiniteautomation.com/downloads/fullCores/enterprise-m2m2-core-"$MA_VERSION".zip > "$MA_CORE_ZIP"
	MA_DELETE_ZIP=true
fi
unzip "$MA_CORE_ZIP" -d "$MA_HOME"
[ "$MA_DELETE_ZIP" = true ] && rm -f "$MA_CORE_ZIP"

# Create an overrides env.properties file
MA_ENV_FILE="$MA_HOME"/overrides/properties/env.properties
if [ "$MA_DB_TYPE" = 'mysql' ]; then
	echo "db.url=jdbc:mysql://localhost/$MA_DB_NAME?useSSL=false" > "$MA_ENV_FILE"
elif [ "$MA_DB_TYPE" = 'h2' ]; then
	echo "db.url=jdbc:h2:$MA_HOME/databases/$MA_DB_NAME" > "$MA_ENV_FILE"
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

chmod 600 "$MA_ENV_FILE"

# Used to download updated scripts from git main branch
get_script() {
	curl -s "https://raw.githubusercontent.com/infiniteautomation/ma-core-public/main/Core/scripts/$1" > "$MA_HOME/bin/$1"
}

get_script ma-start-systemd.sh
get_script mango.service
get_script getenv.sh
get_script genkey.sh
get_script certbot-deploy.sh
get_script ma-start-options.sh

chmod +x "$MA_HOME"/bin/*.sh
cp "$MA_HOME/bin/ma-start-options.sh" "$MA_HOME/overrides/"

# generate a default self signed SSL/TLS certificate
"$MA_HOME"/bin/genkey.sh

# create a new systemd service file in overrides
echo "[Unit]
Description=Mango Automation
After=mysqld.service
StartLimitIntervalSec=0

[Service]
EnvironmentFile=/etc/environment
Type=forking
WorkingDirectory=$MA_HOME
PIDFile=$MA_HOME/bin/ma.pid
ExecStart=$MA_HOME/bin/ma-start-systemd.sh
SuccessExitStatus=0 SIGINT SIGTERM 130 143
Restart=always
RestartSec=5s
User=$MA_USER
NoNewPrivileges=true

[Install]
WantedBy=multi-user.target" > "$MA_HOME"/overrides/mango.service

chown -R "$MA_USER":"$MA_GROUP" "$MA_HOME"

# Stop and remove any existing mango service
if [ -x "$(command -v systemctl)" ] && [ -d /etc/systemd/system ]; then
	# link the new service file into /etc
	ln -sf "$MA_HOME"/overrides/mango.service /etc/systemd/system/"$MA_SERVICE_NAME".service

	# enable the sytemd service (but dont start Mango)
	systemctl enable "$MA_SERVICE_NAME"
	echo "Mango was installed successfully. Type 'systemctl start $MA_SERVICE_NAME' to start Mango."
else
	echo "Mango was installed successfully. Type 'sudo -u $MA_USER $MA_HOME/bin/ma-start-systemd.sh' to start Mango. (systemd is not available and Mango will not start on boot)"
fi
