gcloud projects create --name zeppelin-primer


gcloud projects list
# Set the default region, zone, and project:
# [EDIT]
gcloud config set project zeppelin-primer-xxxxxx
gcloud config set compute/region us-east1
gcloud config set compute/zone us-east1-b