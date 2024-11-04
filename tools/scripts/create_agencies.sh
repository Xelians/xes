#!/bin/bash
#set -x

# Import des services agents
FILEPATH=$1

curl -X 'POST' \
  http://${HOST}/admin-external/v1/agencies \
  --header "X-Tenant-Id: ${TENANT}" \
  --header 'Accept: */*' \
  --header 'Content-Type: application/octet-stream' \
  --data-binary "@${FILEPATH}"
