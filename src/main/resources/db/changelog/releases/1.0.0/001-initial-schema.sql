-- liquibase formatted sql

-- changeset Emmanuel_Deviller:1717137657894-1
CREATE SEQUENCE IF NOT EXISTS global_seq START WITH 1 INCREMENT BY 50;

-- changeset Emmanuel_Deviller:1717137657894-2
CREATE SEQUENCE IF NOT EXISTS logbook_seq START WITH 1 INCREMENT BY 1;

-- changeset Emmanuel_Deviller:1717137657894-3
CREATE SEQUENCE IF NOT EXISTS message_seq START WITH 1 INCREMENT BY 50;

-- changeset Emmanuel_Deviller:1717137657894-4
CREATE SEQUENCE IF NOT EXISTS job_seq START WITH 1 INCREMENT BY 1;

-- changeset Emmanuel_Deviller:1717137657894-5
CREATE SEQUENCE IF NOT EXISTS organization_seq START WITH 1 INCREMENT BY 50;

-- changeset Emmanuel_Deviller:1717137657894-6
CREATE SEQUENCE IF NOT EXISTS referential_seq START WITH 1 INCREMENT BY 50;

-- changeset Emmanuel_Deviller:1717137657894-8
CREATE SEQUENCE IF NOT EXISTS task_lock_seq START WITH 1 INCREMENT BY 50;

-- changeset Emmanuel_Deviller:1717137657894-9
CREATE SEQUENCE IF NOT EXISTS tenant_seq START WITH 1 INCREMENT BY 1;

-- changeset Emmanuel_Deviller:1717137657894-10
CREATE SEQUENCE IF NOT EXISTS user_account_seq START WITH 1 INCREMENT BY 50;

-- changeset Emmanuel_Deviller:1717137657894-11
CREATE TABLE access_contract
(
    id                        BIGINT       NOT NULL,
    identifier                VARCHAR(64)  NOT NULL,
    tenant                    BIGINT       NOT NULL,
    name                      VARCHAR(512) NOT NULL,
    description               VARCHAR(512) NOT NULL,
    creation_date             date         NOT NULL,
    last_update               date         NOT NULL,
    activation_date           date         NOT NULL,
    deactivation_date         date         NOT NULL,
    status                    SMALLINT     NOT NULL,
    lfcs                      BYTEA,
    auto_version              INTEGER      NOT NULL,
    operation_id              BIGINT       NOT NULL,
    root_units                BYTEA        NOT NULL,
    excluded_root_units       BYTEA        NOT NULL,
    data_object_version       BYTEA        NOT NULL,
    every_data_object_version BOOLEAN      NOT NULL,
    originating_agencies      BYTEA        NOT NULL,
    every_originating_agency  BOOLEAN      NOT NULL,
    writing_permission        BOOLEAN      NOT NULL,
    writing_restricted_desc   BOOLEAN      NOT NULL,
    access_log                SMALLINT     NOT NULL,
    CONSTRAINT pk_access_contract PRIMARY KEY (id)
);

-- changeset Emmanuel_Deviller:1717137657894-12
CREATE TABLE agency
(
    id                BIGINT       NOT NULL,
    identifier        VARCHAR(64)  NOT NULL,
    tenant            BIGINT       NOT NULL,
    name              VARCHAR(512) NOT NULL,
    description       VARCHAR(512) NOT NULL,
    creation_date     date         NOT NULL,
    last_update       date         NOT NULL,
    activation_date   date         NOT NULL,
    deactivation_date date         NOT NULL,
    status            SMALLINT     NOT NULL,
    lfcs              BYTEA,
    auto_version      INTEGER      NOT NULL,
    operation_id      BIGINT       NOT NULL,
    CONSTRAINT pk_agency PRIMARY KEY (id)
);

-- changeset Emmanuel_Deviller:1717137657894-13
CREATE TABLE ingest_contract
(
    id                                BIGINT       NOT NULL,
    identifier                        VARCHAR(64)  NOT NULL,
    tenant                            BIGINT       NOT NULL,
    name                              VARCHAR(512) NOT NULL,
    description                       VARCHAR(512) NOT NULL,
    creation_date                     date         NOT NULL,
    last_update                       date         NOT NULL,
    activation_date                   date         NOT NULL,
    deactivation_date                 date         NOT NULL,
    status                            SMALLINT     NOT NULL,
    lfcs                              BYTEA,
    auto_version                      INTEGER      NOT NULL,
    operation_id                      BIGINT       NOT NULL,
    link_parent_id                    BIGINT,
    check_parent_ids                  BYTEA        NOT NULL,
    check_parent_link                 SMALLINT     NOT NULL,
    archive_profiles                  BYTEA        NOT NULL,
    master_mandatory                  BOOLEAN      NOT NULL,
    every_data_object_version         BOOLEAN      NOT NULL,
    data_object_version               BYTEA        NOT NULL,
    format_unidentified_authorized    BOOLEAN      NOT NULL,
    every_format_type                 BOOLEAN      NOT NULL,
    format_type                       BYTEA        NOT NULL,
    compute_inherited_rules_at_ingest BOOLEAN      NOT NULL,
    store_manifest                    BOOLEAN      NOT NULL,
    management_contract_id            VARCHAR(255),
    CONSTRAINT pk_ingest_contract PRIMARY KEY (id)
);

-- changeset Emmanuel_Deviller:1717137657894-14
CREATE TABLE message
(
    id                BIGINT       NOT NULL,
    sender_identifier BIGINT       NOT NULL,
    recipient         VARCHAR(255) NOT NULL,
    content           VARCHAR(255) NOT NULL,
    created           TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_message PRIMARY KEY (id)
);

-- changeset Emmanuel_Deviller:1717137657894-15
CREATE TABLE ontology
(
    id                BIGINT       NOT NULL,
    identifier        VARCHAR(64)  NOT NULL,
    tenant            BIGINT       NOT NULL,
    name              VARCHAR(512) NOT NULL,
    description       VARCHAR(512) NOT NULL,
    creation_date     date         NOT NULL,
    last_update       date         NOT NULL,
    activation_date   date         NOT NULL,
    deactivation_date date         NOT NULL,
    status            SMALLINT     NOT NULL,
    lfcs              BYTEA,
    auto_version      INTEGER      NOT NULL,
    operation_id      BIGINT       NOT NULL,
    CONSTRAINT pk_ontology PRIMARY KEY (id)
);

-- changeset Emmanuel_Deviller:1717137657894-16
CREATE TABLE ontology_mapping
(
    ontology_id   BIGINT       NOT NULL,
    mapping_value VARCHAR(255),
    mapping_key   VARCHAR(255) NOT NULL,
    CONSTRAINT pk_ontology_mapping PRIMARY KEY (ontology_id, mapping_key)
);

-- changeset Emmanuel_Deviller:1717137657894-17
CREATE TABLE operation
(
    id                  BIGINT       NOT NULL,
    lbk_id              BIGINT,
    tenant              BIGINT       NOT NULL,
    user_identifier     VARCHAR(255) NOT NULL,
    application_id      VARCHAR(255),
    contract_identifier VARCHAR(255),
    type                VARCHAR(255) NOT NULL,
    to_register         BOOLEAN      NOT NULL,
    to_secure           BOOLEAN      NOT NULL,
    status              VARCHAR(255) NOT NULL,
    message             TEXT         NOT NULL,
    created             TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    modified            TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    type_info           VARCHAR(255),
    object_identifier   VARCHAR(255),
    object_info         VARCHAR(255),
    object_data         TEXT,
    outcome             VARCHAR(255),
    property01          TEXT,
    property02          TEXT,
    events              TEXT,
    actions             BYTEA        NOT NULL,
    CONSTRAINT pk_operation PRIMARY KEY (id)
);

-- changeset Emmanuel_Deviller:1717137657894-18
CREATE TABLE organization
(
    id                BIGINT       NOT NULL,
    name              VARCHAR(512) NOT NULL,
    description       VARCHAR(512) NOT NULL,
    creation_date     date         NOT NULL,
    last_update       date         NOT NULL,
    activation_date   date         NOT NULL,
    deactivation_date date         NOT NULL,
    status            SMALLINT     NOT NULL,
    lfcs              BYTEA,
    auto_version      INTEGER      NOT NULL,
    operation_id      BIGINT       NOT NULL,
    identifier        VARCHAR(64)  NOT NULL,
    tenant            BIGINT,
    CONSTRAINT pk_organization PRIMARY KEY (id)
);

-- changeset Emmanuel_Deviller:1717137657894-19
CREATE TABLE profile
(
    id                BIGINT       NOT NULL,
    identifier        VARCHAR(64)  NOT NULL,
    tenant            BIGINT       NOT NULL,
    name              VARCHAR(512) NOT NULL,
    description       VARCHAR(512) NOT NULL,
    creation_date     date         NOT NULL,
    last_update       date         NOT NULL,
    activation_date   date         NOT NULL,
    deactivation_date date         NOT NULL,
    status            SMALLINT     NOT NULL,
    lfcs              BYTEA,
    auto_version      INTEGER      NOT NULL,
    operation_id      BIGINT       NOT NULL,
    format            SMALLINT,
    data              BYTEA,
    CONSTRAINT pk_profile PRIMARY KEY (id)
);

-- changeset Emmanuel_Deviller:1717137657894-21
CREATE TABLE rule
(
    id                BIGINT       NOT NULL,
    identifier        VARCHAR(64)  NOT NULL,
    tenant            BIGINT       NOT NULL,
    name              VARCHAR(512) NOT NULL,
    description       VARCHAR(512) NOT NULL,
    creation_date     date         NOT NULL,
    last_update       date         NOT NULL,
    activation_date   date         NOT NULL,
    deactivation_date date         NOT NULL,
    status            SMALLINT     NOT NULL,
    lfcs              BYTEA,
    auto_version      INTEGER      NOT NULL,
    operation_id      BIGINT       NOT NULL,
    type              SMALLINT     NOT NULL,
    duration          VARCHAR(10)  NOT NULL,
    measurement       SMALLINT     NOT NULL,
    CONSTRAINT pk_rule PRIMARY KEY (id)
);

-- changeset Emmanuel_Deviller:1717137657894-22
CREATE TABLE secret_key
(
    tenant BIGINT NOT NULL,
    secret BYTEA  NOT NULL,
    CONSTRAINT pk_secret_key PRIMARY KEY (tenant)
);

-- changeset Emmanuel_Deviller:1717137657894-23
CREATE TABLE job
(
    job_type   VARCHAR(255) NOT NULL,
    identifier BIGINT,
    expire     TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT pk_job PRIMARY KEY (job_type)
);

-- changeset Emmanuel_Deviller:1717137657894-24
CREATE TABLE storage
(
    id            BIGINT NOT NULL,
    secure_number BIGINT NOT NULL,
    CONSTRAINT pk_storage PRIMARY KEY (id)
);

-- changeset Emmanuel_Deviller:1717137657894-25
CREATE TABLE task_lock
(
    id             BIGINT  NOT NULL,
    exclusive_lock BOOLEAN NOT NULL,
    CONSTRAINT pk_task_lock PRIMARY KEY (id)
);

-- changeset Emmanuel_Deviller:1717137657894-26
CREATE TABLE tenant
(
    id                BIGINT       NOT NULL,
    name              VARCHAR(512) NOT NULL,
    description       VARCHAR(512) NOT NULL,
    creation_date     date         NOT NULL,
    last_update       date         NOT NULL,
    activation_date   date         NOT NULL,
    deactivation_date date         NOT NULL,
    status            SMALLINT     NOT NULL,
    lfcs              BYTEA,
    auto_version      INTEGER      NOT NULL,
    operation_id      BIGINT       NOT NULL,
    storage_offers    BYTEA        NOT NULL,
    encrypted         BOOLEAN,
    organization_id   BIGINT       NOT NULL,
    CONSTRAINT pk_tenant PRIMARY KEY (id)
);

-- changeset Emmanuel_Deviller:1717137657894-27
CREATE TABLE user_account
(
    id                BIGINT       NOT NULL,
    name              VARCHAR(512) NOT NULL,
    description       VARCHAR(512) NOT NULL,
    creation_date     date         NOT NULL,
    last_update       date         NOT NULL,
    activation_date   date         NOT NULL,
    deactivation_date date         NOT NULL,
    status            SMALLINT     NOT NULL,
    lfcs              BYTEA,
    auto_version      INTEGER      NOT NULL,
    operation_id      BIGINT       NOT NULL,
    identifier        VARCHAR(64)  NOT NULL,
    username          VARCHAR(256) NOT NULL,
    first_name        VARCHAR(256),
    last_name         VARCHAR(256),
    email             VARCHAR(256) NOT NULL,
    password          VARCHAR(256),
    access_key        VARCHAR(256),
    global_roles      BYTEA        NOT NULL,
    tenant_roles      BYTEA        NOT NULL,
    access_contracts  BYTEA        NOT NULL,
    ingest_contracts  BYTEA        NOT NULL,
    organization_id   BIGINT       NOT NULL,
    CONSTRAINT pk_user_account PRIMARY KEY (id)
);

-- changeset Emmanuel_Deviller:1717137657894-28
ALTER TABLE access_contract
    ADD CONSTRAINT access_contract_access_log_check CHECK ((access_log >= 0) AND (access_log <= 1));

-- changeset Emmanuel_Deviller:1717137657894-29
ALTER TABLE access_contract
    ADD CONSTRAINT access_contract_status_check CHECK ((status >= 0) AND (status <= 1));

-- changeset Emmanuel_Deviller:1717137657894-31
ALTER TABLE access_contract
    ADD CONSTRAINT access_contract_tenant_check CHECK (tenant >= 0);

-- changeset Emmanuel_Deviller:1717137657894-32
ALTER TABLE access_contract
    ADD CONSTRAINT unique_accesscontract_tenant_identifier UNIQUE (tenant, identifier);

-- changeset Emmanuel_Deviller:1717137657894-33
ALTER TABLE access_contract
    ADD CONSTRAINT unique_accesscontract_tenant_name UNIQUE (tenant, name);

-- changeset Emmanuel_Deviller:1717137657894-34
ALTER TABLE agency
    ADD CONSTRAINT agency_status_check CHECK ((status >= 0) AND (status <= 1));

-- changeset Emmanuel_Deviller:1717137657894-35
ALTER TABLE agency
    ADD CONSTRAINT agency_tenant_check CHECK (tenant >= 0);

-- changeset Emmanuel_Deviller:1717137657894-36
ALTER TABLE agency
    ADD CONSTRAINT unique_agency_tenant_identifier UNIQUE (tenant, identifier);

-- changeset Emmanuel_Deviller:1717137657894-37
ALTER TABLE ingest_contract
    ADD CONSTRAINT ingest_contract_check_parent_link_check CHECK ((check_parent_link >= 0) AND (check_parent_link <= 2));

-- changeset Emmanuel_Deviller:1717137657894-38
ALTER TABLE ingest_contract
    ADD CONSTRAINT ingest_contract_status_check CHECK ((status >= 0) AND (status <= 1));

-- changeset Emmanuel_Deviller:1717137657894-39
ALTER TABLE ingest_contract
    ADD CONSTRAINT ingest_contract_link_parent_id_check CHECK (link_parent_id >= 0);

-- changeset Emmanuel_Deviller:1717137657894-40
ALTER TABLE ingest_contract
    ADD CONSTRAINT ingest_contract_tenant_check CHECK (tenant >= 0);

-- changeset Emmanuel_Deviller:1717137657894-41
ALTER TABLE ingest_contract
    ADD CONSTRAINT unique_ingestcontract_tenant_identifier UNIQUE (tenant, identifier);

-- changeset Emmanuel_Deviller:1717137657894-42
ALTER TABLE ingest_contract
    ADD CONSTRAINT unique_ingestcontract_tenant_name UNIQUE (tenant, name);

-- changeset Emmanuel_Deviller:1717137657894-43
ALTER TABLE message
    ADD CONSTRAINT message_content_check CHECK ((content)::text = ANY
    ((ARRAY ['SECURE_START':: character varying, 'SECURE_STOP':: character varying, 'SECURE_STARTED':: character varying, 'SECURE_STOPPED':: character varying])::text[]));

-- changeset Emmanuel_Deviller:1717137657894-44
ALTER TABLE ontology
    ADD CONSTRAINT ontology_status_check CHECK ((status >= 0) AND (status <= 1));

-- changeset Emmanuel_Deviller:1717137657894-45
ALTER TABLE ontology
    ADD CONSTRAINT ontology_tenant_check CHECK (tenant >= 0);

-- changeset Emmanuel_Deviller:1717137657894-46
ALTER TABLE ontology
    ADD CONSTRAINT unique_ontology_tenant_identifier UNIQUE (tenant, identifier);

-- changeset Emmanuel_Deviller:1717137657894-47
ALTER TABLE ontology
    ADD CONSTRAINT unique_ontology_tenant_name UNIQUE (tenant, name);

-- changeset Emmanuel_Deviller:1717137657894-48
ALTER TABLE operation
    ADD CONSTRAINT operation_status_check CHECK ((status)::text = ANY
    ((ARRAY ['INIT':: character varying, 'ERROR_INIT':: character varying, 'RUN':: character varying, 'ERROR_CHECK':: character varying, 'ERROR_COMMIT':: character varying, 'BACKUP':: character varying, 'STORE':: character varying, 'RETRY_STORE':: character varying, 'INDEX':: character varying, 'RETRY_INDEX':: character varying, 'OK':: character varying, 'FATAL':: character varying])::text[]));

-- changeset Emmanuel_Deviller:1717137657894-49
ALTER TABLE operation
    ADD CONSTRAINT operation_type_check CHECK ((type)::text = ANY
    ((ARRAY ['INGEST_ARCHIVE':: character varying, 'INGEST_FILING':: character varying, 'INGEST_HOLDING':: character varying, 'UPDATE_ARCHIVE':: character varying, 'UPDATE_ARCHIVE_RULES':: character varying, 'RECLASSIFY_ARCHIVE':: character varying, 'ELIMINATE_ARCHIVE':: character varying, 'PROBATIVE_VALUE':: character varying, 'EXPORT_ARCHIVE':: character varying, 'TRANSFER_ARCHIVE':: character varying, 'TRACEABILITY':: character varying, 'REBUILD_SEARCH_ENGINE':: character varying, 'RESET_INDEX':: character varying, 'RESET_INDEX_CHUNK':: character varying, 'CREATE_AGENCY':: character varying, 'UPDATE_AGENCY':: character varying, 'CREATE_ONTOLOGY':: character varying, 'UPDATE_ONTOLOGY':: character varying, 'CREATE_ACCESSCONTRACT':: character varying, 'UPDATE_ACCESSCONTRACT':: character varying, 'CREATE_INGESTCONTRACT':: character varying, 'UPDATE_INGESTCONTRACT':: character varying, 'CREATE_PROFILE':: character varying, 'UPDATE_PROFILE':: character varying, 'CREATE_RULE':: character varying, 'UPDATE_RULE':: character varying, 'DELETE_RULE':: character varying, 'CREATE_ROLE':: character varying, 'UPDATE_ROLE':: character varying, 'CREATE_USER':: character varying, 'UPDATE_USER':: character varying, 'CREATE_ORGANIZATION':: character varying, 'UPDATE_ORGANIZATION':: character varying, 'CREATE_TENANT':: character varying, 'UPDATE_TENANT':: character varying, 'EXTERNAL':: character varying, 'ADD_OFFER':: character varying, 'ADD_OFFER_CHUNK':: character varying, 'DELETE_OFFER':: character varying, 'CHECK_COHERENCY':: character varying, 'CHECK_TENANT_COHERENCY':: character varying, 'REPAIR_COHERENCY':: character varying, 'AUDIT':: character varying, 'SYNC':: character varying])::text[]));

-- changeset Emmanuel_Deviller:1717137657894-50
ALTER TABLE organization
    ADD CONSTRAINT organization_status_check CHECK ((status >= 0) AND (status <= 1));

-- changeset Emmanuel_Deviller:1717137657894-51
ALTER TABLE organization
    ADD CONSTRAINT organization_tenant_check CHECK (tenant >= 0);

-- changeset Emmanuel_Deviller:1717137657894-52
ALTER TABLE organization
    ADD CONSTRAINT unique_organization_identifier UNIQUE (identifier);

-- changeset Emmanuel_Deviller:1717137657894-53
ALTER TABLE profile
    ADD CONSTRAINT profile_format_check CHECK ((format >= 0) AND (format <= 1));

-- changeset Emmanuel_Deviller:1717137657894-54
ALTER TABLE profile
    ADD CONSTRAINT profile_status_check CHECK ((status >= 0) AND (status <= 1));

-- changeset Emmanuel_Deviller:1717137657894-55
ALTER TABLE profile
    ADD CONSTRAINT profile_tenant_check CHECK (tenant >= 0);

-- changeset Emmanuel_Deviller:1717137657894-56
ALTER TABLE profile
    ADD CONSTRAINT unique_profile_tenant_identifier UNIQUE (tenant, identifier);

-- changeset Emmanuel_Deviller:1717137657894-57
ALTER TABLE profile
    ADD CONSTRAINT unique_profile_tenant_name UNIQUE (tenant, name);

-- changeset Emmanuel_Deviller:1717137657894-58
ALTER TABLE rule
    ADD CONSTRAINT rule_measurement_check CHECK ((measurement >= 0) AND (measurement <= 2));

-- changeset Emmanuel_Deviller:1717137657894-59
ALTER TABLE rule
    ADD CONSTRAINT rule_status_check CHECK ((status >= 0) AND (status <= 1));

-- changeset Emmanuel_Deviller:1717137657894-60
ALTER TABLE rule
    ADD CONSTRAINT rule_type_check CHECK ((type >= 0) AND (type <= 6));

-- changeset Emmanuel_Deviller:1717137657894-61
ALTER TABLE rule
    ADD CONSTRAINT rule_tenant_check CHECK (tenant >= 0);

-- changeset Emmanuel_Deviller:1717137657894-62
ALTER TABLE rule
    ADD CONSTRAINT unique_rule_tenant_identifier UNIQUE (tenant, identifier);

-- changeset Emmanuel_Deviller:1717137657894-63
ALTER TABLE user_account
    ADD CONSTRAINT user_account_status_check CHECK ((status >= 0) AND (status <= 1));

-- changeset Emmanuel_Deviller:1717137657894-64
ALTER TABLE user_account
    ADD CONSTRAINT unique_user_email UNIQUE (email);

-- changeset Emmanuel_Deviller:1717137657894-65
ALTER TABLE user_account
    ADD CONSTRAINT unique_user_identifier_organization_id UNIQUE (identifier, organization_id);

-- changeset Emmanuel_Deviller:1717137657894-66
ALTER TABLE user_account
    ADD CONSTRAINT unique_user_username UNIQUE (username);

-- changeset Emmanuel_Deviller:1717137657894-69
ALTER TABLE job
    ADD CONSTRAINT job_type_check CHECK ((job_type)::text = ANY
    ((ARRAY ['ACCESSION':: character varying, 'AUDIT':: character varying, 'BACKUP':: character varying, 'CLEAN':: character varying, 'RETRY':: character varying, 'TRACEABILITY':: character varying, 'STORE':: character varying])::text[]));

-- changeset Emmanuel_Deviller:1717137657894-70
ALTER TABLE storage
    ADD CONSTRAINT storage_secure_number_check CHECK (secure_number >= '-1':: integer);

-- changeset Emmanuel_Deviller:1717137657894-71
ALTER TABLE tenant
    ADD CONSTRAINT tenant_status_check CHECK ((status >= 0) AND (status <= 1));

-- changeset Emmanuel_Deviller:1717137657894-72
ALTER TABLE tenant
    ADD CONSTRAINT FK_TENANT_ON_ORGANIZATION FOREIGN KEY (organization_id) REFERENCES organization (id);

-- changeset Emmanuel_Deviller:1717137657894-73
ALTER TABLE user_account
    ADD CONSTRAINT FK_USER_ACCOUNT_ON_ORGANIZATION FOREIGN KEY (organization_id) REFERENCES organization (id);

-- changeset Emmanuel_Deviller:1717137657894-74
ALTER TABLE ontology_mapping
    ADD CONSTRAINT fk_ontology_mapping_on_ontology_db FOREIGN KEY (ontology_id) REFERENCES ontology (id)

-- changeset Emmanuel_Deviller:1717137657894-75
CREATE INDEX idx_operation_tenant_id ON operation (tenant, id);

