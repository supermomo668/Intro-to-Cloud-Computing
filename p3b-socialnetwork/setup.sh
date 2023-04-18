export TF_VAR_password=DEFAULT_PASSWORD

terraform init
terraform apply
az deployment group create --name "sql-instance" --resource-group "social-network-rg" --template-file "template.json" --parameters "parameters.json"

#ssh clouduser@"$INSTANCE_PUBLIC_IP"
