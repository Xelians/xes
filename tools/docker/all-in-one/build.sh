#!/bin/bash

# Navigate to the root directory
pushd ../../..

# Build eSafe and create the jar
mvn -U clean package -DskipTests

# Build the Docker image
docker build -f tools/docker/all-in-one/Dockerfile -t esafe-all-in-one .

popd