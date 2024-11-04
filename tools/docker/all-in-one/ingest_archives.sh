#!/bin/bash
#set -x

# This script upload several archives in the the all-in-one container
# You can replay this script multiple times.

ALL_IN_ONE="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
SCRIPT=$ALL_IN_ONE/../../scripts
ASSETS=$ALL_IN_ONE/../../../src/test/resources/all_in_one
pushd $ASSETS/sip > /dev/null

export HOST=localhost:8080
export TENANT=1

# versement des archives séquentiellement
for i in $(ls sip_*.zip 2>/dev/null); do
	${SCRIPT}/ingest_sip.sh $i
done
popd > /dev/null


