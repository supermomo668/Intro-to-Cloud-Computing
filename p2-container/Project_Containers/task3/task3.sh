#
helm version >> helm_version.txt
#Get the latest Chart information from chart repositories, similar to the apt-get update command in Linux.
helm repo add stable https://charts.helm.sh/stable
helm repo update

# Before you run the command, set the values of the environment variables $mysqlRootPassword, $mysqlUser and $mysqlPassword using `export`.
# Please avoid using digits only in these variables or the value will be recognized as integer instead of string which may cause error.

export mysqlRootPassword=pagemasterRr
export mysqlUser=eric
export mysqlPassword=pagemaster

##
helm install mysql-profile --set mysqlRootPassword=${mysqlRootPassword},mysqlUser=${mysqlUser},mysqlPassword=${mysqlPassword},mysqlDatabase=test stable/mysql

helm list >> helmlist.txt

##
cd profile-service
export TAG=containers
export PROJECT_PATH=$PWD
export DOCKERFILE_PATH=$PWD/src/main/docker/Dockerfile
#
### CHECK depencies are uploaded
docker build -f $DOCKERFILE_PATH -t $TAG $PROJECT_PATH
docker tag $TAG:latest gcr.io/$GCP_PROJECT_ID/$TAG:latest
docker push gcr.io/$GCP_PROJECT_ID/$TAG

### Check service, deployment, config yaml. Match docker name
helm install profile profile-service/src/main/helm/profile/
kubectl get pods >> kube_pods.txt
#

sleep 5s
## test [ TODO]
#export LOAD_BALANCER_EXTERNAL_IP=35.229.104.18
#curl http://$LOAD_BALANCER_EXTERNAL_IP/profile?username=$USERNAME

# Unisntall profile
#helm uninstall profile