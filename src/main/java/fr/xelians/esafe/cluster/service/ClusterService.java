/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.cluster.service;

import fr.xelians.esafe.cluster.domain.JobType;
import fr.xelians.esafe.cluster.repository.ClusterRepository;
import jakarta.annotation.PostConstruct;
import java.util.EnumSet;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/*
 * @author Emmanuel Deviller
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClusterService {

  private final ClusterRepository repository;

  @Getter private long identifier;

  @Value("${app.batch.cluster.fixedDelay:120000}")
  private long fixedDelay;

  @PostConstruct
  public void init() {
    identifier = repository.getNextValue();
    // Create all job types in database
    EnumSet.allOf(JobType.class).forEach(repository::initJobs);
  }

  @Transactional
  public void refreshJobs() {
    repository.refreshJobs(identifier, fixedDelay * 0.005d);
  }

  public boolean isActive(JobType jobType) {
    return !repository.findActiveJobs(jobType, identifier, fixedDelay * 0.002d).isEmpty();
  }
}
