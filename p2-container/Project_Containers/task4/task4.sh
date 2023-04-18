# Install ingress
helm install my-nginx stable/nginx-ingress
kubectl create -f Ingress/ingress.yaml
kubectl get ingress >>  kube_ingress.txt
# Install SQL and services
    # Configure everything in yaml first
    # and ConfigMap(yaml)
# sql-profile (only if not installed)
# helm install mysql-profile --set mysqlRootPassword=${mysqlRootPassword},mysqlUser=${mysqlUser},mysqlPassword=${mysqlPassword},mysqlDatabase=test stable/mysql
helm install profile profile-service/src/main/helm/profile/
# Chat
export SERVICE=chat
export SERVICE_TAG="$TAG-$SERVICE"
export DOCKERFILE_PATH="group-$SERVICE-service/src/main/docker/Dockerfile"
export PROJECT_PATH="./group-$SERVICE-service"
docker build -f $DOCKERFILE_PATH -t $SERVICE_TAG $PROJECT_PATH
docker tag $SERVICE_TAG:latest gcr.io/$GCP_PROJECT_ID/$SERVICE_TAG
docker push gcr.io/$GCP_PROJECT_ID/$SERVICE_TAG:latest
    #helm 
helm install mysql-chat --set mysqlRootPassword=${mysqlRootPassword},mysqlUser=${mysqlUser},mysqlPassword=${mysqlPassword},mysqlDatabase=test stable/mysql
helm install chat group-chat-service/src/main/helm/chat/
# Login
export SERVICE=login
export SERVICE_TAG="$TAG-$SERVICE"
export DOCKERFILE_PATH="$SERVICE-service/src/main/docker/Dockerfile"
export PROJECT_PATH="./$SERVICE-service"
docker build -f $DOCKERFILE_PATH -t $SERVICE_TAG $PROJECT_PATH
docker tag $SERVICE_TAG:latest gcr.io/$GCP_PROJECT_ID/$SERVICE_TAG:latest
docker push gcr.io/$GCP_PROJECT_ID/$SERVICE_TAG
    #helm 
helm install mysql-login --set mysqlRootPassword=${mysqlRootPassword},mysqlUser=${mysqlUser},mysqlPassword=${mysqlPassword},mysqlDatabase=test stable/mysql
helm install login login-service/src/main/helm/login/

# Check
helm list >> helm_list.txt

# Submit
# ./submitter