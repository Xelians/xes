#!/bin/bash
#set -x

# Versement d'un arbre de positionnement
FILEPATH=$1

echo Versement d\'un arbre: $FILEPATH
id=$(curl -s -D - -X 'POST' \
  http://${HOST}/ingest-external/v1/ingests \
  --header "X-Tenant-Id: ${TENANT}" \
  --header 'Accept: */*' \
  --header 'Content-Type: application/octet-stream' \
  --header 'X-ACTION: RESUME' \
  --header 'X-Context-Id: HOLDING_SCHEME' \
  --data-binary "@${FILEPATH}" \
  | grep 'X-Request-Id' | sed -e "s/X-Request-Id: //" | tr -d "\r")

echo -n OperationId: ${id}

while true
do
  sleep 0.1
  status=$(curl -s -X 'GET' http://${HOST}/admin-external/v1/operations/$id \
  --header "X-Tenant-Id: ${TENANT}" \
  --header 'Accept: application/json' | jq -r  '."$results"[] | .globalStatus')
  if [[ "$status" == "OK" || "$status" == "KO" || "$status" = "Fatal" ]]; then
    echo " - status:" $status
    break
  fi
done
