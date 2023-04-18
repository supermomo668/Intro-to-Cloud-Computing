aws emr list-clusters --active >> EMR_info.txt
## Get ID then input
export emr_cluster_id=j-3JMQMLFNNKCX7
aws emr list-instances --region us-east-1 --instance-group-types MASTER --cluster-id j-3JMQMLFNNKCX7 >> EMR_master_info.txt

##
aws ec2 describe-instances --instance-types "t3.micro" >> student_vm_info.txt