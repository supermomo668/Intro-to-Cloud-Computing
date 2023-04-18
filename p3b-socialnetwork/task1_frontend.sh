cd /home/clouduser/social_network_backend
mvn clean package exec:java -Dmaven.test.skip=true

export HISTIGNORE="export*" # so that the following export commands will not be tracked into bash history
export SUBMISSION_USERNAME="your_submission_username"
export SUBMISSION_PASSWORD="your_submission_pwd"
./submitter -t task1