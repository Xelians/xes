#!/bin/bash
#set -x

# This script creates several referentials in the the all-in-one esafe container
# As some referentials require unicity, it must be executed only once.

ALL_IN_ONE="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
SCRIPT=$ALL_IN_ONE/../../scripts
ASSETS=$ALL_IN_ONE/../../../src/test/resources/all_in_one
pushd $ASSETS/ref > /dev/null

export HOST=localhost:8080
export TENANT=1

# import des services agents
for i in $(ls *services_agents*.csv 2>/dev/null); do
	${SCRIPT}/create_agencies.sh $i
done 

# import des services règles de gestion
for i in $(ls *regles_gestion*.csv 2>/dev/null); do
	${SCRIPT}/create_rules.sh $i
done 

# import des contrats d'accès
for i in $(ls *contrat_acces*.json 2>/dev/null); do
	${SCRIPT}/create_accesscontracts.sh $i
done 

# import des profils
for i in $(ls *profil_archivage*.json 2>/dev/null); do
	${SCRIPT}/create_profiles.sh $i
done 

# import des contrats d'entrée
for i in $(ls *contrat_entree*.json 2>/dev/null); do
	${SCRIPT}/create_ingestcontracts.sh $i
done

# versement des arbres de positionnement
pushd $ASSETS/holding > /dev/null
for i in $(ls *arbre_de_positionnement*.zip 2>/dev/null); do
  # TODO Check the holding does not already exist
	${SCRIPT}/ingest_holding.sh $i
done
popd > /dev/null

sleep 1
popd > /dev/null