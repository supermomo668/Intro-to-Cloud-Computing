
mkdir azure-sql && cd azure-sql
wget https://clouddeveloper.blob.core.windows.net/assets/social-network/project/azure-sql.tgz
tar -xvzf azure-sql.tgz

# in parameters.json, set the value of the following parameter: 
# `serverName.value` 
# This will be used to identify your Azure Server

# note that the value should be the andrew_id username 
#(for e.g. xyz@andrew.cmu.edu; username is xyz)
#az deployment group create --name "sql-instance" --resource-group "social-network-rg" --template-file "template.json" --parameters "parameters.json"
