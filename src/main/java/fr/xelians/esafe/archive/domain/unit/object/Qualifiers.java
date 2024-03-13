/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.archive.domain.unit.object;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@EqualsAndHashCode
@ToString
public class Qualifiers {

  @NotNull
  @JsonProperty("qualifier")
  private String qualifier;

  @JsonProperty("_nbc")
  private int nbc = 0;

  @JsonProperty("versions")
  private List<ObjectVersion> versions = new ArrayList<>();

  @JsonIgnore
  public void incNbc() {
    nbc++;
  }

  @JsonIgnore
  public boolean isBinaryQualifier() {
    return BinaryQualifier.isValid(qualifier);
  }

  @JsonIgnore
  public static ObjectVersion getGreatestObjectVersion(
      List<Qualifiers> qualifiers, String qualifier) {

    for (Qualifiers q : qualifiers) {
      if (q.getQualifier().equals(qualifier)) {
        return ObjectVersion.getGreatestVersion(q.getVersions());
      }
    }
    return null;
  }

  // Fetch the binary object version
  @JsonIgnore
  public static ObjectVersion getObjectVersion(
      List<Qualifiers> qualifiers, BinaryVersion binaryVersion) {

    String qualifier = binaryVersion.qualifier().toString();
    if (binaryVersion.version() == null) {
      return getGreatestObjectVersion(qualifiers, qualifier);
    }

    for (Qualifiers q : qualifiers) {
      if (q.getQualifier().equals(qualifier)) {
        return ObjectVersion.getVersion(q.versions, binaryVersion);
      }
    }

    // Fetch the binary object
    return null;
  }
}
