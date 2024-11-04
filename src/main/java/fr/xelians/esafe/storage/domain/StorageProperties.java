/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.storage.domain;

import fr.xelians.esafe.storage.domain.offer.fs.FileSystemStorage;
import fr.xelians.esafe.storage.domain.offer.s3.S3Storage;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/*
 * @author Emmanuel Deviller
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.storage.offer")
@Validated
public class StorageProperties {

  private List<FileSystemStorage> fs = new ArrayList<>();

  private List<S3Storage> s3 = new ArrayList<>();
}
