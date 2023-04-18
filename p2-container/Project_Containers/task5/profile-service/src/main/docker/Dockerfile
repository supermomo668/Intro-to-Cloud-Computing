# Ubuntu Linux as the base image
FROM maven:3.8.3-jdk-8-slim AS build

COPY . /code
WORKDIR /code
# Install the packages by using the default package manager in Ubuntu

RUN mvn clean package spring-boot:repackage && \
    cp ./target/profile-embedded-0.1.0.jar ./profile-embedded-0.1.0.jar
 
# Open port 80
EXPOSE 8080

# Copy the files from the host file system and ADD them to the desired directory inside the container imag
# Run command
CMD ["java", "-jar", "./profile-embedded-0.1.0.jar"]
