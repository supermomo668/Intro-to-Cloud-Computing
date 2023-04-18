## EMR commands

aws emr list-clusters

#export CLUSTER_ID=j-3OVR6WO06WXQS
aws emr describe-cluster --cluster-id $CLUSTER_ID
aws emr list-instances --cluster-id $CLUSTER_ID --instance-group-type MASTER >> 
aws emr list-instances --cluster-id $CLUSTER_ID --instance-group-type NODE >> emr_cluster_node.txt
