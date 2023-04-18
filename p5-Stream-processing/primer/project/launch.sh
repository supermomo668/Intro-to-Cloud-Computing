#! /bin/bash
##############################################
# Runner Script for deploying samza job    ###
##############################################

# Preparing folder for deployment
mkdir -p deploy/samza

# Compile and build the jar
mvn clean package
rm -rf deploy/samza/*

# Extract tar.gz file to deployment folder
tar -xvf target/newsfeed-0.0.1-dist.tar.gz -C deploy/samza/

# Copy the tar.gz file to hadoop file system
hadoop fs -copyFromLocal -f target/newsfeed-0.0.1-dist.tar.gz /

deploy/samza/bin/run-app.sh --config-factory=org.apache.samza.config.factories.PropertiesConfigFactory --config-path="file://$PWD/deploy/samza/config/newsfeed-fan-out.properties"
deploy/samza/bin/run-app.sh --config-factory=org.apache.samza.config.factories.PropertiesConfigFactory --config-path="file://$PWD/deploy/samza/config/newsfeed-generate-follows.properties"
deploy/samza/bin/run-app.sh --config-factory=org.apache.samza.config.factories.PropertiesConfigFactory --config-path="file://$PWD/deploy/samza/config/newsfeed-generate-messages.properties"
