#!/bin/bash

# Le header à ajouter
header="/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */
 "

# Rechercher tous les fichiers .java dans l'arborescence
find . -type f -name "*.java" | while read -r file; do
    # Vérifier si le fichier contient le header
    if ! head -n 10 "$file" | grep -q "Ceccil v2.1 License" ; then
        # Si le header n'est pas trouvé, ajouter le header en tête de fichier
        echo "Ajout du header dans $file"
        # Créer un fichier temporaire avec le header puis le contenu original
        echo "$header" | cat - "$file" > temp && mv temp "$file"
    fi
done
