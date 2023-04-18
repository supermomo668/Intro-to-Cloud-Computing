
# get current context 
# kubectl config current-context 
# helm switch
kubectl config get-contexts >> kube_contexts.txt
kubectl config use-context gke_gcp-docker-kubernetes-341822_us-east1-d_wecloudchatcluster

# Update all Azure side (profile) with correct yaml image
helm uninstall chat
# [TODO] Ensure the chat endpoint (profile) is modified to ingress
helm install chat group-chat-service/src/main/helm/chat/

# kubectl config use-context p5-azure-cluster