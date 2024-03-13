/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.processing;

import fr.xelians.esafe.common.task.AbstractOperationTask;
import fr.xelians.esafe.operation.service.OperationService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.Future;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessingService {

  @Getter private final OperationService operationService;

  @Value("${app.processing.threads:0}")
  private int processingThreads;

  private TaskPoolExecutor executor;

  @PostConstruct
  public void init() {
    int defaultThreads = Runtime.getRuntime().availableProcessors() * 2;
    int threads = processingThreads <= 0 ? defaultThreads : processingThreads;
    log.info("Number thread available={} for task pool executor", threads);
    this.executor = new TaskPoolExecutor(operationService, threads);
    this.executor.start();
  }

  @PreDestroy
  public void destroy() {
    this.executor.stop();
  }

  public Future<Void> submit(AbstractOperationTask<Void> task) {
    return executor.submit(task);
  }
}
