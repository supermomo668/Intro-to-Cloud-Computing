<< com
2. Before you use MySQL client to connect to the Azure database for MySQL instance, you must whitelist the IP address of the client machine.
    ○ Go to Azure Portal -> Azure Database for MySQL server -> Select the database resource
    ○ Under Settings -> Connection Security
    ○ Set Allow access to Azure services to Yes
    ○ Add the Public IP of your student VM instance as the Start IP and the End IP
    ○ Click Save
3. SSH into the workspace VM as clouduser with the password you set when you provision the VM.
ssh clouduser@"$INSTANCE_PUBLIC_IP"
Validate the connection by logging on to the MySQL instance. You need to specify the host when connecting to a remote server. You can visit Azure Portal -> Azure Database for MySQL server -> Select the database you created to view the Server Name and Server Admin Login Name fields. For your convenience, you might want to set the server name, admin name, and the password you set above as environment variables.

com
# set variables
# export variable
export MYSQL_HOST=mangyinmuvyj7qyyp26gy.mysql.database.azure.com # from azure
export MYSQL_NAME=clouduser@mangyinmuvyj7qyyp26gy  # from azure
export MYSQL_PWD=pagemasterRr5!  #