
export TAG=containers
export PROJECT_PATH=$PWD
export DOCKERFILE_PATH=$PWD/src/main/docker/Dockerfile
export GCP_PROJECT_ID=gcp-docker-kubernetes-341822
#
docker tag $TAG:latest gcr.io/$GCP_PROJECT_ID/$TAG:latest
#
gcloud auth login
gcloud auth configure-docker
docker push gcr.io/$GCP_PROJECT_ID/$TAG
#
gcloud auth application-default login
gcloud config set project $GCP_PROJECT_ID

export ZONE=us-east1-d
export CLUSTER_NAME=wecloudchatcluster
# Show the available Kubernetes versions
gcloud container get-server-config --zone=$ZONE >> gcloud_kubernetes_version.txt

# You can modify the cluster name, machine type and the zone
export CLUSTER_NAME="wecloudchatcluster"
gcloud container clusters create $CLUSTER_NAME --zone=$ZONE --num-nodes=1 --machine-type=custom-4-12288 
gcloud container clusters get-credentials $CLUSTER_NAME --zone=$ZONE &>> glcoud_credential.txt
#
kubectl apply -f ./profile-service-embedded-db/src/main/k8s/.
# testing
kubectl get services >> kube_services.txt
# List all pods in the default namespace
kubectl get pods >> kube_pods.txt
# Retrieve the logs for a specific pod
kubectl logs $POD_NAME
# The exec command will give you the terminal access inside of the pod
#kubectl exec -it $POD_NAME -- /bin/sh

export LOAD_BALANCER_EXTERNAL_IP=35.229.104.186
export USERNAME=eric
curl http://$LOAD_BALANCER_EXTERNAL_IP/profile?username=$USERNAME
# submit

#./profile-service-embedded-db/.submitter
# kubectl delete -f ./profile-service-embedded-db/src/main/k8s/.