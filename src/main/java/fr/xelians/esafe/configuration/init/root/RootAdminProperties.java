/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.configuration.init.root;

import fr.xelians.esafe.common.constant.DefaultValue;
import java.util.ArrayList;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/*
 * @author Julien Cornille
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.root-admin")
public class RootAdminProperties {

  private String username;

  private String password;

  private String email;

  private boolean initAccessKey = false;

  private Tenant tenant;

  @Data
  public static class Tenant {

    private ArrayList<String> storageOffers;

    private Boolean encrypted = DefaultValue.ENCRYPTED;
  }
}
