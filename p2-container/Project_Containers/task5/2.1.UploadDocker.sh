# Build and tag the container image for uploading.


export SERVICE=profile
export SERVICE_TAG="$TAG-$SERVICE"
export DOCKERFILE_PATH="$SERVICE-service/src/main/docker/Dockerfile"
export PROJECT_PATH="./$SERVICE-service"
export AZURECR_ID=p5azurecontainer
# profile
docker tag $SERVICE_TAG:latest ${AZURECR_ID}.azurecr.io/$SERVICE_TAG
docker push ${AZURECR_ID}.azurecr.io/$SERVICE_TAG
helm install mysql-chat --set mysqlRootPassword=${mysqlRootPassword},mysqlUser=${mysqlUser},mysqlPassword=${mysqlPassword},mysqlDatabase=test stable/mysql
helm install profile profile-service/src/main/helm-multi-cloud/profile/

export SERVICE=login
export SERVICE_TAG="$TAG-$SERVICE"
export DOCKERFILE_PATH="$SERVICE-service/src/main/docker/Dockerfile"
export PROJECT_PATH="./$SERVICE-service"
export AZURECR_ID=p5azurecontainer
# LOGIN
docker tag $SERVICE_TAG:latest ${AZURECR_ID}.azurecr.io/$SERVICE_TAG
docker push ${AZURECR_ID}.azurecr.io/$SERVICE_TAG
helm install chat login-service/src/main/helm-multi-cloud/login/

#You can now verify the fully qualified name of your ACR login server with the following command.
#az acr list --resource-group ${RESOURCE_GROUP_NAME} --query "[].{acrLoginServer:loginServer}" --output table &>> acr_resource_group.txt

#Push the image to the Azure Container Registry.
