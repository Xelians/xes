/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.common.json;

public class JsonConfigBuilder {

  private Boolean format;

  private JsonConfigBuilder() {
    format = false;
  }

  public static JsonConfigBuilder builder() {
    return new JsonConfigBuilder();
  }

  public JsonConfigBuilder format(boolean format) {
    this.format = format;
    return this;
  }

  public JsonConfig build() {
    return new JsonConfig(format);
  }
}
