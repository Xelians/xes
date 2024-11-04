#!/bin/bash

# Import des contrats d'accès
FILEPATH=$1

echo Import access contract: $FILEPATH
curl -s -w "HttpCode: %{http_code}\n" -X 'POST' \
  http://${HOST}/admin-external/v1/accesscontracts \
  --header "X-Tenant-Id: ${TENANT}" \
  --header 'Accept: application/json' \
  --header 'Content-Type: application/json' \
  --data-binary "@${FILEPATH}"
