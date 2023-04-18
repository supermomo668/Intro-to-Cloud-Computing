# create an ingress controller
helm install my-nginx stable/nginx-ingress
kubectl create -f Ingress_Azure/ingress.yaml

helm install mysql-profile --set mysqlRootPassword=${mysqlRootPassword},mysqlUser=${mysqlUser},mysqlPassword=${mysqlPassword},mysqlDatabase=test stable/mysql