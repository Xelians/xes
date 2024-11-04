#!/bin/bash
#set -x

# Import des règle de cycle de vie
FILEPATH=$1

echo Import rules: $FILEPATH
curl -s -w "HttpCode: %{http_code}\n" -X 'POST' \
  http://${HOST}/admin-external/v1/rules \
  --header "X-Tenant-Id: ${TENANT}" \
  --header 'Accept: */*' \
  --header 'Content-Type: text/plain' \
  --data-binary "@${FILEPATH}"
