# Esafe Sample

This repository provides a `docker-compose-sample.yml` file for easy and streamlined deployment of the eSafe
application. This setup is designed to quickly spin up the necessary services (PostgreSQL, Elasticsearch, and the eSafe
application) for testing, development, or demonstration purposes.

## Purpose

The purpose of the `docker-compose-sample.yml` file is to:

- Provide a quick and easy way to start the eSafe application with minimal configuration.
- Include essential services such as PostgreSQL, Elasticsearch, and the eSafe application for a streamlined environment.
- Allow users to run the eSafe application using a predefined profile (`dev`) with sensible defaults.

This file is particularly useful for users who want to evaluate or test the application without diving into detailed
configuration.

## Services

### 1. **PostgreSQL**

- **Image**: `postgres:16.1-alpine`
- **Purpose**: Serves as the primary database for the eSafe application.
- **Configuration**:
    - The `POSTGRES_PASSWORD` is set to `postgres`.
    - Exposes port `5432`.
    - The `log_statement=all` command is added for detailed SQL logging.
    - Includes a healthcheck using `pg_isready` to ensure that PostgreSQL is ready before the application starts.

### 2. **Elasticsearch**

- **Image**: `docker.elastic.co/elasticsearch/elasticsearch:8.1.2`
- **Purpose**: Handles search indexing and querying functionality.
- **Configuration**:
    - Single-node mode.
    - Java memory settings (`ES_JAVA_OPTS`) are configured with `-Xms2g -Xmx2g`.
    - Security is enabled with the password set to `elastic`.
    - Exposes port `9200`.
    - A healthcheck ensures that Elasticsearch is available by checking the cluster health status.

### 3. **eSafe Application**

- **Image**: `esafe:latest`
- **Purpose**: The core eSafe application.
- **Configuration**:
    - Exposes port `8080`.
    - Uses the `dev` profile (`SPRING_PROFILES_ACTIVE=dev`).
    - A healthcheck is configured to verify that the application is running via the `/actuator/health` endpoint.
    - Environment variables configure the connection to PostgreSQL and Elasticsearch.

## How to Use

### Prerequisites

Make sure Docker, Docker Compose, and Maven are installed on your machine:

- [Install Docker](https://docs.docker.com/get-docker/)
- [Install Docker Compose](https://docs.docker.com/compose/install/)
- [Install Maven](https://maven.apache.org/install.html)

### Building the eSafe Application

Before running the `docker-compose-sample.yml`, you need to package the eSafe application and build the Docker image.

1. Run the following Maven command at the root of the repository to package the application (skipping the tests):

    ```bash
    mvn package -DskipTests -f ../../pom.xml
    ```

2. Once the package is built, copy the JAR file from `../../target/*.jar` to the current directory and rename it
   to `esafe.jar`:

    ```bash
    cp ../../target/*.jar ./esafe.jar
    ```

3. Build the Docker image for the eSafe application:

    ```bash
    cd ../..
    docker build -t esafe:latest .
    ```

This builds the `esafe:latest` image, which will be used by the `docker-compose-sample.yml` file.

### Running the Sample Setup

To start the eSafe application using the `docker-compose-sample.yml`:

1. Navigate to the directory containing the Docker Compose file:

    ```bash
    cd docker/sample
    ```

2. Start the services with Docker Compose:

    ```bash
    docker-compose -f docker-compose-sample.yml up -d
    ```

   This will start PostgreSQL, Elasticsearch, and the eSafe application. The healthchecks will ensure that all services
   are up and running in the correct order.

3. Access the eSafe application at:

    ```
    http://localhost:8080
    ```

4. Access the secured Elasticsearch instance at:

    ```
    http://localhost:9200
    ```

### Stopping the Services

To stop and remove the running containers:

```bash
docker-compose -f docker-compose-sample.yml down --remove-orphans
```

This will stop the application and clean up all running services.

## Troubleshooting

- **Database Connection Issues**: Ensure that PostgreSQL is running and healthy by checking the container logs using:

    ```bash
    docker logs <postgres-container-id>
    ```

- **Elasticsearch Connectivity**: If Elasticsearch isn't responding, check its logs for potential issues:

    ```bash
    docker logs <elastic-container-id>
    ```

- **Application Health**: The eSafe application exposes a health endpoint at `http://localhost:8080/actuator/health`.
  You can monitor this to ensure the app is running correctly.

## Notes

- The `docker-compose-sample.yml` file is optimized for development and testing purposes. It should not be used in
  production. For production environments, it's recommended to use a more customized setup with appropriate security
  configurations.
- The `dev` profile is used for development environments, which simplifies configuration and allows for quick testing.
