/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.operation.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;

@SequenceGenerator(name = "logbook_generator", sequenceName = "logbook", allocationSize = 1)
@Entity
public class DummyEntity {

  @Id
  @GeneratedValue(generator = "logbook_generator")
  private Long id;
}
