

# helm uninstall all
#helm ls --all --short | xargs -L1 helm delete

gcloud projects delete $PROJECT_NAME
az group delete --name $RESOURCE_GROUP
