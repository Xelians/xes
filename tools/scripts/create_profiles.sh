#!/bin/bash
#set -x

# Import du profile
FILEPATH=$1
BNAME=$(basename "$FILEPATH" .json)
DNAME=$(dirname "$FILEPATH")/rng
RNGPATH=${DNAME}/${BNAME}.rng

echo Import profiles: $FILEPATH
curl -s -X 'POST' \
  http://${HOST}/admin-external/v1/profiles \
  --header "X-Tenant-Id: ${TENANT}" \
  --header 'Accept: application/json' \
  --header 'Content-Type: application/json' \
  --data-binary "@${FILEPATH}" | jq -r '.[] | .Identifier' \
| while read identifier
do
# Import RNG profile
curl -s -w "HttpCode: %{http_code}\n" -X 'PUT' \
  http://${HOST}/admin-external/v1/profiles/${identifier}/data \
  --header "X-Tenant-Id: ${TENANT}" \
  --header 'Accept: application/json' \
  --header 'Content-Type: application/octet-stream' \
  --data-binary "@${RNGPATH}"
done




