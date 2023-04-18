export subscription_id=70e826c7-0644-40e0-baf8-6ea10a818774
az account set --subscription $subscription_id
# register the following resource provider:
az provider register --namespace Microsoft.HDInsight
#Confirm that you have registered
az hdinsight list-usage --location eastus

mkdir spark_project && cd spark_project
wget https://clouddeveloper.blob.core.windows.net/s22-cloud-developer/iterative-processing/project/terraform.tgz && tar -xvzf terraform.tgz

#Create a file named terraform.tfvars and set the values of the variables defined in variables.tf:
touch terraform.tfvars
echo -e '''gateway_password      = "cc15619CMUmo" # The password for Ambari UI gateway of the Spark cluster\n
ssh_password          = "cc15619CMUmo" # The password for SSH into the Spark cluster\n
username              = "azureusermo" # The username to SSH into the Spark cluster
'''
export ssh_password=cc15619CMUmo
export user_name=azureusermo

terraform init
terraform validate
terraform apply

