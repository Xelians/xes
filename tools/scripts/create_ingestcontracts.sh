#!/bin/bash

# Import de contrat d'entree
FILEPATH=$1

echo Import ingest contract: $FILEPATH
curl -s -w "HttpCode: %{http_code}\n" -X 'POST' \
  http://${HOST}/admin-external/v1/ingestcontracts \
  --header "X-Tenant-Id: ${TENANT}" \
  --header 'Accept: application/json' \
  --header 'Content-Type: application/json' \
  --data-binary "@${FILEPATH}"
