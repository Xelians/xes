# Use a base image with a larger environment like Ubuntu
FROM ubuntu:20.04

# Install dependencies and repositories in one layer, clean up APT cache to reduce image size
RUN apt-get update && \
    apt-get install -y wget curl gnupg software-properties-common apt-transport-https && \
    # Add OpenJDK 21 repository
    add-apt-repository ppa:openjdk-r/ppa -y && \
    apt-get update && apt-get install -y openjdk-21-jdk && \
    # Add PostgreSQL 16 repository and key
    echo "deb http://apt.postgresql.org/pub/repos/apt focal-pgdg main" > /etc/apt/sources.list.d/pgdg.list && \
    wget --quiet -O - https://www.postgresql.org/media/keys/ACCC4CF8.asc | apt-key add - && \
    apt-get update && apt-get install -y postgresql-16 && \
    # Add Elasticsearch repository and key
    wget -qO - https://artifacts.elastic.co/GPG-KEY-elasticsearch | apt-key add - && \
    echo "deb https://artifacts.elastic.co/packages/8.x/apt stable main" | tee /etc/apt/sources.list.d/elastic-8.x.list && \
    apt-get update && apt-get install -y elasticsearch=8.1.2 && \
    # Clean up APT cache
    apt-get clean && rm -rf /var/lib/apt/lists/*

# Copy your eSafe JAR and related files into the container
COPY target/*.jar /app/esafe.jar

# Configure PostgreSQL
RUN echo "listen_addresses='*'" >> /etc/postgresql/16/main/postgresql.conf && \
    echo "# Allow all local connections to the postgres database using the postgres user" > /etc/postgresql/16/main/pg_hba.conf && \
    echo "local   all   postgres   trust" >> /etc/postgresql/16/main/pg_hba.conf && \
    echo "host    all   postgres   0.0.0.0/0   trust" >> /etc/postgresql/16/main/pg_hba.conf && \
    echo "host    all   postgres   ::/0        trust" >> /etc/postgresql/16/main/pg_hba.conf

# Configure Elasticsearch
RUN echo "discovery.type: single-node" >> /etc/elasticsearch/elasticsearch.yml && \
    echo "bootstrap.memory_lock: true" >> /etc/elasticsearch/elasticsearch.yml && \
    echo "http.port: 9200" >> /etc/elasticsearch/elasticsearch.yml && \
    echo "-Xms2g" >> /etc/elasticsearch/jvm.options && \
    echo "-Xmx2g" >> /etc/elasticsearch/jvm.options && \
    sed -i '/cluster.initial_master_nodes/d' /etc/elasticsearch/elasticsearch.yml && \
    sed -i 's/xpack.security.enabled: true/xpack.security.enabled: false/' /etc/elasticsearch/elasticsearch.yml && \
    # Set ownership and permissions for Elasticsearch files
    chown -R elasticsearch:elasticsearch /etc/elasticsearch /usr/share/elasticsearch /var/lib/elasticsearch

# Set environment variables
ENV TZ=Europe/Paris \
    PATH=/usr/share/elasticsearch/bin:$PATH \
    SPRING_PROFILES_ACTIVE=dev \
    SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/postgres \
    SPRING_DATASOURCE_USERNAME=postgres \
    SPRING_DATASOURCE_PASSWORD=postgres \
    ELASTICSEARCH_HOST=localhost

# Expose the necessary ports
EXPOSE 5432 9200 8080
HEALTHCHECK --interval=5s --timeout=10s --start-period=30s --retries=5 CMD curl -f http://localhost:8080/actuator/health || exit 1

# Copy and set permissions for the entrypoint script
COPY ./tools/docker/all-in-one/entrypoint.sh /usr/local/bin/entrypoint.sh
RUN chmod +x /usr/local/bin/entrypoint.sh

# Set the entrypoint script
ENTRYPOINT ["/usr/local/bin/entrypoint.sh"]
