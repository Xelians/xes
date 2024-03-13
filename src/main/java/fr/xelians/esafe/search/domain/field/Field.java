/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.search.domain.field;

import com.fasterxml.jackson.databind.JsonNode;
import fr.xelians.esafe.common.exception.technical.InternalException;
import fr.xelians.esafe.common.utils.Utils;
import java.util.Objects;
import lombok.Getter;

/** The type Field. */
@Getter
public abstract class Field {

  public static final String EXT = "_ext";

  private final String name;
  private final boolean isStandard;

  /**
   * Instantiates a new Field.
   *
   * @param name the name
   */
  protected Field(String name, boolean isStandard) {
    this.name = name;
    this.isStandard = isStandard;
  }

  /**
   * Create field.
   *
   * @param name the name
   * @param type the type
   * @return the field
   */
  public static Field create(String name, String type, boolean isStandard) {
    return switch (type) {
      case KeywordField.TYPE -> new KeywordField(name, isStandard);
      case TextField.TYPE -> new TextField(name, isStandard);
      case DateField.TYPE -> new DateField(name, isStandard);
      case LongField.TYPE -> new LongField(name, isStandard);
      case IntegerField.TYPE -> new IntegerField(name, isStandard);
      case DoubleField.TYPE -> new DoubleField(name, isStandard);
      case BooleanField.TYPE -> new BooleanField(name, isStandard);
      default -> throw new InternalException(
          "Field creation failed", String.format("Unknown type '%s' for '%s'", type, name));
    };
  }

  /**
   * Gets field name.
   *
   * @param key the key
   * @param value the value
   * @return the field name
   */
  public static String getFieldName(String key, int value) {
    return key + Utils.padKey(value);
  }

  /**
   * Gets type.
   *
   * @return the type
   */
  public abstract String getType();

  /**
   * Check.
   *
   * @param value the value
   */
  public abstract void check(String value);

  /**
   * Is valid boolean.
   *
   * @param value the value
   * @return the boolean
   */
  public abstract boolean isValid(String value);

  /**
   * As value t.
   *
   * @param node the node
   * @return the t
   */
  public abstract Object asValue(JsonNode node);

  /**
   * Gets name.
   *
   * @return the name
   */
  public String getName() {
    return name;
  }

  public String getFullName() {
    return isStandard ? name : EXT + "." + name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Field field = (Field) o;
    return getName().equals(field.getName()) && getType().equals(field.getType());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getName(), getType());
  }

  @Override
  public String toString() {
    return "Field{" + "name='" + getName() + '\'' + ", type='" + getType() + '\'' + '}';
  }
}
