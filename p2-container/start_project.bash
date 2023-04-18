export SUBMISSION_USERNAME=mangyinm@andrew.cmu.edu
export SUBMISSION_PASSWORD=AnjJJXblinZm7DUhnBBRnr
export ZONE=us-east1-d
export CLUSTER_NAME=wecloudchatcluster
export DOCKERFILE_PATH=/home/clouduser/Project_Containers/task4/profile-service/src/main/docker/Dockerfile
export mysqlRootPassword=pagemasterRr
export TAG=containers
export mysqlUser=eric
export PWD=/home/clouduser/Project_Containers
# [TODO]
# export GCP_PROJECT_ID=gcp-docker-kubernetes-341308

gcloud auth login
gcloud auth configure-docker
gcloud auth application-default login
gcloud config set project $GCP_PROJECT_ID

# Show the available Kubernetes versions
gcloud container get-server-config --zone=us-east1-d >> gcloud_avail_kubernetes.txt

# You can modify the cluster name, machine type and the zone
gcloud container clusters resize $CLUSTER_NAME --num-nodes=1 --zone $ZONE