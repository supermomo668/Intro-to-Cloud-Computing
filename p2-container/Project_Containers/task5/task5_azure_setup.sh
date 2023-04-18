az login

# Check and set the Azure subscription.
az account list --output table --refresh >> 
az account set --subscription <name or id>

# Initialize these variables

# Container registry resource group
export RESOURCE_GROUP=... 

# Azure Container registry, the name must be in all lowercase
export ACR_NAME=...

# AKS Cluster name
export CLUSTER_NAME=...

az group create -n ${RESOURCE_GROUP} -l eastus

az acr create -n ${ACR_NAME} -g ${RESOURCE_GROUP} --sku basic

az aks create -n ${CLUSTER_NAME} -g ${RESOURCE_GROUP} --attach-acr ${ACR_NAME}  --generate-ssh-keys