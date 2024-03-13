/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.cluster.service;

import fr.xelians.esafe.cluster.domain.NodeFeature;
import fr.xelians.esafe.cluster.entity.ServerNodeDb;
import fr.xelians.esafe.cluster.repository.ServerNodeRepository;
import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServerNodeService {

  private final ServerNodeRepository repository;
  private Set<NodeFeature> features = new HashSet<>();

  @Getter private long identifier;

  @PostConstruct
  public void init() {
    this.identifier = repository.getNextValue();
    // TODO Shuffle and wait somme time before initiating node features
    EnumSet.allOf(NodeFeature.class).forEach(repository::upsert);
  }

  @Transactional
  public void updateFeatures() {
    Set<NodeFeature> newFeatures = new HashSet<>();
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime delay = now.plusMinutes(5);
    List<ServerNodeDb> nodes = repository.findByIdentifierOrDelay(identifier, now);
    for (ServerNodeDb node : nodes) {
      node.setIdentifier(identifier);
      node.setDelay(delay);
      newFeatures.add(node.getFeature());
    }
    repository.saveAll(nodes);

    // Assignment is atomic in Java. So synchronization is not needed
    this.features = newFeatures;
  }

  public boolean hasFeature(NodeFeature feature) {
    return features.contains(feature);
  }

  public List<NodeFeature> getFeatures() {
    return new ArrayList<>(features);
  }
}
