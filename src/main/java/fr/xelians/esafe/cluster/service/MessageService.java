/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.cluster.service;

import fr.xelians.esafe.cluster.domain.MessageContent;
import fr.xelians.esafe.cluster.entity.MessageDb;
import fr.xelians.esafe.cluster.repository.MessageRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

  private final MessageRepository repository;
  private final ServerNodeService serverNodeService;

  private final ConcurrentHashMap<MessageContent, Consumer<MessageDb>> register =
      new ConcurrentHashMap<>();

  public MessageDb publish(String recipient, MessageContent content) {
    MessageDb message = new MessageDb();
    message.setSenderIdentifier(serverNodeService.getIdentifier());
    message.setCreated(LocalDateTime.now());
    message.setRecipient(recipient);
    message.setContent(content);
    return repository.save(message);
  }

  public void register(MessageContent content, Consumer<MessageDb> consumer) {
    register.put(content, consumer);
  }

  @Transactional
  public void process() {
    String identifier = String.valueOf(serverNodeService.getIdentifier());
    List<String> features = serverNodeService.getFeatures().stream().map(Enum::name).toList();
    List<MessageDb> messages = repository.findByIdentifierOrFeatures(identifier, features);
    repository.deleteAll(messages);
    for (MessageDb message : messages) {
      Consumer<MessageDb> consumer = register.get(message.getContent());
      if (consumer != null) {
        try {
          consumer.accept(message);
        } catch (Exception ex) {
          log.error(String.format("Failed to process %s", message.getContent()), ex);
        }
      } else {
        log.error("Failed to find consumer for {}", message.getContent());
      }
    }
  }
}
