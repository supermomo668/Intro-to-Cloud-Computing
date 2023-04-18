cd profile-service-embedded-db
source ./paths.sh
#
export TAG=containers
export PROJECT_PATH=$PWD
export DOCKERFILE_PATH=$PWD/src/main/docker/Dockerfile
#
docker build -f $DOCKERFILE_PATH -t $TAG $PROJECT_PATH
#
docker run -p 8000:8080 containers:latest 
#
./task1/.submitter