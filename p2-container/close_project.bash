helm uninstall $(helm ls -aq)
gcloud container clusters resize $CLUSTER_NAME --num-nodes=0 --zone $ZONE -Y

# Azure
kubectl config unset users.[USER]
kubectl config unset contexts.[CONTEXT]
kubectl config unset clusters.[CLUSTER]