#!/bin/bash

# Start PostgreSQL
echo "Starting PostgreSQL..."
service postgresql start

# Start Elasticsearch
echo "Starting Elasticsearch..."
su elasticsearch -s /bin/bash -c "export PATH=/usr/share/elasticsearch/bin:$PATH; elasticsearch" &

# Wait for PostgreSQL to be ready
until pg_isready; do
  echo "Waiting for PostgreSQL..."
  sleep 1
done

# Wait for Elasticsearch to be ready
until curl -u elastic:elastic --fail --silent --output /dev/null http://localhost:9200/_cat/health; do
  echo "Waiting for Elasticsearch..."
  sleep 1
done

# Determine the Spring profiles to use
SPRING_PROFILE="dev"  # Default profile

# Check for STUB_AUTH environment variable
if [ "$STUB_AUTH" == "true" ]; then
    SPRING_PROFILE="$SPRING_PROFILE,stub-auth"  # Append stub-auth if true
fi

# Start eSafe application
echo "Starting eSafe with profiles: $SPRING_PROFILE..."
java -jar /app/esafe.jar --spring.profiles.active="$SPRING_PROFILE"


