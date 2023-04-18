gcloud config set project gcp-docker-kubernetes-341822
gcloud config set compute/region us-east1
gcloud config set compute/zone us-east1-b

gcloud auth application-default login
export project="gcp-docker-kubernetes-341822" # your gcp-docker-kubernetes project ID

terraform init 
terraform apply
