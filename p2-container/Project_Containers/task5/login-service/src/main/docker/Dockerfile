# Ubuntu Linux as the base image
FROM maven:3.8.3-jdk-8-slim AS build

COPY . /login-service
WORKDIR /login-service
# Install the packages by using the default package manager in Ubuntu
#ADD  ./src /profile-service-embedded-db/
#ADD  ./pom.xml /profile-service-embedded-db/
#ADD  ./start.sh /profile-service-embedded-db/

RUN apt-get update && \
    #apt-get install openjdk-8-jdk && \
    #apt-get -y install maven && \
    mvn clean package spring-boot:repackage && \
    cp ./target/login-0.1.0.jar ./login-0.1.0.jar
 
# Open port 80
EXPOSE 8080

# Copy the files from the host file system and ADD them to the desired directory inside the container imag
#COPY --from=build /profile-service-embedded-db/target/finalproject.jar app.jar
# Run command
CMD ["java", "-jar", "./login-0.1.0.jar"]
