/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.common.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TaskEvent(@JsonProperty("name") String name, @JsonProperty("watch") Long watch) {}
