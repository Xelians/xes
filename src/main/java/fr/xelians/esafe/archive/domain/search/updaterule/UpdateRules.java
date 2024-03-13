/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.archive.domain.search.updaterule;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record UpdateRules(
    @JsonProperty("Rules") List<UpdateRule> rules,
    @JsonProperty("PreventInheritance") Boolean preventInheritance,
    @JsonProperty("FinalAction") String finalAction) {}
