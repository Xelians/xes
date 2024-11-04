#!/bin/bash
# set -x

curl -s -X 'GET' \
  'http://localhost:8080/v1/users/me' \
  -H 'accept: */*' | jq   2>/dev/null