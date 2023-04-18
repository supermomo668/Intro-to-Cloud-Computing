az login

# Check and set the Azure subscription.
az account list --output table --refresh >> azure_accounts.txt
az account set --subscription 70e826c7-0644-40e0-baf8-6ea10a818774
export RESOURCE_GROUP=cloudcomputing-p2_task5

# Azure Container registry, the name must be in all lowercase
export ACR_NAME=p5azurecontainer

# AKS Cluster name
export CLUSTER_NAME=p5-azure-cluster

# create resouce group
az group create -n ${RESOURCE_GROUP} -l eastus
# Create a container registry.
az acr create -n ${ACR_NAME} -g ${RESOURCE_GROUP} --sku basic
# Create a New AKS Cluster with ACR Integration
az aks create -n ${CLUSTER_NAME} -g ${RESOURCE_GROUP} --attach-acr ${ACR_NAME}  --generate-ssh-keys

# Login
az acr login --name ${ACR_NAME}
# obtain credential for aks
az aks get-credentials --resource-group=${RESOURCE_GROUP} --name=${CLUSTER_NAME} &>> aks_credentials.txt

# switch between Kubernetes clusters by using the following commands
kubectl config get-contexts >> cluster_contexts.txt  # display list of contexts (i.e., clusters)

# set the default context (i.e, set the default cluster you will work on)
# (TODO)
export CONTEXT_NAME=${CLUSTER_NAME}
kubectl config use-context ${CONTEXT_NAME}