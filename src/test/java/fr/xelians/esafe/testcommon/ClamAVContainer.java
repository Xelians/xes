/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.testcommon;

import fr.xelians.esafe.common.constant.Env;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.Base58;

public class ClamAVContainer extends GenericContainer<ClamAVContainer> {

  private static final int DEFAULT_PORT = 3310;
  private static final String DEFAULT_IMAGE = "mkodockx/docker-clamav";
  private static final String DEFAULT_TAG = "latest";

  public ClamAVContainer() {
    this(DEFAULT_IMAGE + ":" + DEFAULT_TAG);
  }

  public ClamAVContainer(String image) {
    super(image == null ? DEFAULT_IMAGE + ":" + DEFAULT_TAG : image);
    withNetworkAliases("clamav-" + Base58.randomString(6));
    addExposedPort(DEFAULT_PORT);

    // APP_PATH directory must be created before the binding
    String appDir = Env.APP_PATH.toString();
    addFileSystemBind(appDir, appDir, BindMode.READ_ONLY);

    //    setWaitStrategy(
    //        new HttpWaitStrategy()
    //            .forPort(DEFAULT_PORT)
    //            .forPath(HEALTH_ENDPOINT)
    //            .withStartupTimeout(Duration.ofMinutes(2)));
  }

  public String getHostAddress() {
    return getHost() + ":" + getMappedPort(DEFAULT_PORT);
  }
}
