# Ubuntu Linux as the base image
FROM ubuntu:18.04

# Install the packages by using the default package manager in Ubuntu
RUN apt-get update && \
    apt-get -y install default-jre

# Open port 80
EXPOSE 80

# Copy the files from the host file system and ADD them to the desired directory inside the container image
# Note that this will copy the JAR to `/demo-1.0-SNAPSHOT-jar-with-dependencies.jar`
ADD ./target/demo-1.0-SNAPSHOT-jar-with-dependencies.jar /

# Define the command which runs when the container starts
# Note that the filepath of the JAR is `/demo-1.0-SNAPSHOT-jar-with-dependencies.jar`
# as in the filesystem of the image,
# NOT `./target/demo-1.0-SNAPSHOT-jar-with-dependencies.jar` as in the host filesystem
CMD ["java -cp demo-1.0-SNAPSHOT-jar-with-dependencies.jar HelloWorld"]

# Use Bash as the container's entry point.
ENTRYPOINT ["/bin/bash", "-c"]