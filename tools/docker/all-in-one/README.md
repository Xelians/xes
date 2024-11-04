# All-in-One eSafe Docker Container

This docker setup provides a All-in-One container that includes eSafe, PostgreSQL and Elasticsearch.

This All-in-One container allows you to approach eSafe in a simple way. Examples of 'referentials'
and archives (SIP) are provided in order to populate the solution and be able to quickly test and
work with it.

**The All-in-One docker image is intended for testing purposes only and is not suitable for
production use.**

## Prerequisites

Minimum requirements to build and create the docker image:

* 2 Cpus / 4GB RAM
* Linux operating system with the following packages:
    * Docker
    * Java 21
    * Maven

## Build the All-in-One docker image

Go to the "all-in-one" directory and ensure that all scripts are executable. if not, execute the
following command:

```bash
chmod +x *.sh
```

Build eSafe and the docker image by launching the build.sh script:

```bash
./build.sh
```

The docker build creates the image based on the provided dockerfile. The first build downloads
all dependencies and can take several minutes to complete.

The `entrypoint.sh` script is used in the dockerfile to start PostgreSQL, Elasticsearch and
eSafe.

## Create and start the All-in-One docker container

Then, create and start the container with the start.sh script:

```bash
./start.sh 
```

The eSafe application will be started and ready after about 30 seconds (depending on the hardware
performance). The container keeps its data after a restart. But all data will be wipe out after a
build.

The Tenant 1 is automatically created and available for all tenant operations.

## Init and populate eSafe

The following scripts require the curl and jq packages to be previously installed on the system.

Optionally, eSafe may be initialized with sample "referentials" and holding.
Warning. The import_referentials.sh script must only be executed once.

```bash
./import_refrentials.sh 
```

Several sips can then be ingested with the following command:

```bash
./ingest_archives.sh 
```

The container persists the data. Rebuild the image to restart from a clean state.

Note. these scripts are provided for demonstration purpose and are not intended for use in
production environnement.

## Documentation

All eSafe APIs are documented according to the OpenAPI v3 specifications.
After starting the docker container, consult the Swagger documentation
at: [http://localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html)
to learn and test the APIs.

## Configuration

### PostgreSQL

- Listens on port 5432.
- Allows connections from any IP address with the `trust` method.
- Default postgres username: `postgres`
- No password is required.

### Elasticsearch

- Listens on port 9200.
- Configured to run as a single-node cluster.
- Memory settings are set to 2 GB for both heap sizes.
- Security is disabled.

You can access to ElasticSearch in this way:

```bash
curl http://localhost:9200
```

### eSafe

- Listens on port 8080.
- Configured to connect to PostgreSQL and Elasticsearch using environment variables.
- Authentification : eSafe provides a special stub authentication profile for
  testing purposes. When this profile is enabled (by setting the environnement variable
  STUB_AUTH=true), a predefined stub user with administrative roles is injected into the security
  context, bypassing the usual authentication flow. This stub user has access to already created
  tenant 1 within the application. This profile is enabled in the start.sh script.

Warning: the stub profile is only intended for local testing or development purposes and **must not
be used** in production environments.