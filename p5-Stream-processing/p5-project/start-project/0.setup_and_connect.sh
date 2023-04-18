chmod 400 .ssh/15619-CC.pem
export workspace_public_dns="ec2-18-208-160-118.compute-1.amazonaws.com"
export master_public_dns="ec2-3-89-139-119.compute-1.amazonaws.com"

## copy keep into EMR instance
sudo scp -i .ssh/15619-CC.pem .ssh/15619-CC.pem "hadoop@${master_public_dns}:/home/hadoop"

## connect to EMR instance
sudo ssh -i .ssh/15619-CC.pem "hadoop@${master_public_dns}"
source ./0.t1-st-setup_EMR.sh
exit
## connect to student instance
sudo ssh -i .ssh/15619-CC.pem "clouduser@${workspace_public_dns}"
