#   1. In the cluster
# (HDInsight name), which you can get from the Azure Portal under the resource group named spark-resources once it has been provisioned:
export subscription_id=70e826c7-0644-40e0-baf8-6ea10a818774
export cluster_name=cloud-spark-clusterdf10b29942120b7b   # TODO
# 
export https_endpoint="cloud-spark-clusterdf10b29942120b7b.azurehdinsight.net"
export ssh_endpoint="cloud-spark-clusterdf10b29942120b7b-ssh.azurehdinsight.net"
export ssh_password=cc15619CMUmo
export user_name=azureusermo

#cluster public DNS for ssh in the output of terraform apply or via the below command.
az hdinsight show --resource-group spark-resources --name $cluster_name --query properties.connectivityEndpoints

#	1. ssh into the master node
# ssh azureusermo@cloud-spark-cluster3551e536115e00af-ssh.azurehdinsight.net

ssh "$user_name"@"$cluster_name"-ssh.azurehdinsight.net -t
"
sudo apt update && sudo apt install -y maven;   #	2. Install maven.
#	3. Download the student template for this project
wget https://sail.blob.core.windows.net/instance-launcher/p41-template.tgz && tar -xvzf p41-template.tgz  
#   Download the submitter on the master vertex of your Spark cluster.
wget https://clouddeveloper.blob.core.windows.net/s22-cloud-developer/iterative-processing/project/task2-submitter.tgz && tar -xvzf task2-submitter.tgz
"
    
# Zeppelin notebook for Apache Spark 
# Visit the endpoint (i.e. https://cloud-spark-cluster<random-id>.azurehdinsight.net/zeppelin/) 
echo https://"$cluster_name".azurehdinsight.net/zeppelin/