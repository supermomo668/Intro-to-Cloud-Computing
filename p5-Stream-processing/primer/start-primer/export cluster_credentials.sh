export p5_keyname=15619-CC
export p5_keyfile=15619-CC.pem
export log_uri=s3://aws-logs-intro-to-stream-6118-us-east-1
export master_public_dns=ec2-54-210-218-197.compute-1.amazonaws.com
export master_public_homedir=/home/hadoop
# transfer pem file
#scp -i $p5_keyfile $p5_keyfile "hadoop@$master_public_home:$master_public_homedir"
# ssh session
# sudo ssh -i $p5_keyfile hadoop@ec2-54-210-218-197.compute-1.amazonaws.com