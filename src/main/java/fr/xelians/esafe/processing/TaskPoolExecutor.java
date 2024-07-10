/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Ceccil v2.1 License as published by
 * the CEA, CNRS and INRIA.
 */

package fr.xelians.esafe.processing;

import static fr.xelians.esafe.operation.domain.OperationStatus.*;

import fr.xelians.esafe.common.exception.EsafeException;
import fr.xelians.esafe.common.exception.technical.InternalException;
import fr.xelians.esafe.common.task.FutureOperationTask;
import fr.xelians.esafe.common.task.OperationTask;
import fr.xelians.esafe.common.utils.ExceptionsUtils;
import fr.xelians.esafe.common.utils.Utils;
import fr.xelians.esafe.operation.domain.OperationStatus;
import fr.xelians.esafe.operation.entity.OperationDb;
import fr.xelians.esafe.operation.service.OperationService;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TaskPoolExecutor extends ThreadPoolExecutor {
  private final Object lock = new Object();
  private final OperationService operationService;
  private final Map<Long, List<OperationTask>> stashedTasks = new HashMap<>();
  private final UnStasher unstasher = new UnStasher();
  private boolean isStarted;

  public TaskPoolExecutor(
      OperationService operationService, int nThreads, MeterRegistry meterRegistry) {
    super(nThreads, nThreads, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
    this.operationService = operationService;
    registerQueueSizeGauge(meterRegistry);
  }

  private void registerQueueSizeGauge(MeterRegistry meterRegistry) {
    Gauge.builder("taskpool.queue.size", getQueue(), Collection::size)
        .description("The size of the task pool queue")
        .register(meterRegistry);
  }

  public void start() {
    isStarted = true;
    unstasher.start();
  }

  public void stop() {
    isStarted = false;
  }

  @Override
  protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {
    return runnable instanceof OperationTask operationTask
        ? new FutureOperationTask<>(operationTask)
        : new FutureTask<>(runnable, null);
  }

  @Override
  protected void beforeExecute(Thread t, Runnable r) {
    super.beforeExecute(t, r);
    if (r instanceof FutureOperationTask<?> task) {
      OperationTask operationTask = task.getOperationTask();
      checkBeforeStatus(operationTask);
      processTask(operationTask);
      checkAfterStatus(operationTask);
    }
    // All tasks are executed after before execute
  }

  private void processTask(OperationTask operationTask) {
    OperationDb operation = operationTask.getOperation();
    if (operationService.tryLock(operation)) {
      // Already locked, stash and deactivate operation task
      synchronized (lock) {
        stash(operationTask);
      }
    } else if (!operationTask.isExclusive()) {
      // Unstash all operation tasks from tenant
      synchronized (lock) {
        unStashNonExclusive(operation.getTenant());
      }
    }
  }

  @Override
  protected void afterExecute(Runnable r, Throwable t) {
    super.afterExecute(r, t);
    if (r instanceof FutureOperationTask<?> task) {
      OperationTask operationTask = task.getOperationTask();
      // Avoid tasks loop
      if (operationTask.isActive()) {
        synchronized (lock) {
          unStash(operationTask.getOperation().getTenant());
        }
      }
    }
  }

  private void stash(OperationTask operationTask) {
    operationTask.setActive(false);
    Long tenant = operationTask.getOperation().getTenant();
    stashedTasks.computeIfAbsent(tenant, k -> new ArrayList<>()).add(operationTask);
  }

  // All unstashed tasks will be reprocessed in processTask method
  private void unStashNonExclusive(Long tenant) {
    List<OperationTask> tasks = stashedTasks.get(tenant);
    if (tasks != null) {
      var it = tasks.iterator();
      while (it.hasNext()) {
        OperationTask task = it.next();
        if (!task.isExclusive()) {
          it.remove();
          task.setActive(true);
          submit(task);
        }
      }
      if (tasks.isEmpty()) {
        stashedTasks.remove(tenant);
      }
    }
  }

  private void unStash(Long tenant) {
    List<OperationTask> tasks = stashedTasks.get(tenant);
    if (tasks != null) {
      boolean isFirst = true;
      var it = tasks.iterator();
      while (it.hasNext()) {
        OperationTask task = it.next();
        if (task.isExclusive()) {
          if (isFirst) {
            it.remove();
            task.setActive(true);
            submit(task);
          }
          break;
        } else {
          isFirst = false;
          it.remove();
          task.setActive(true);
          submit(task);
        }
      }
      if (tasks.isEmpty()) {
        stashedTasks.remove(tenant);
      }
    }
  }

  // This check is only done for asserting that this works as expected
  private void checkBeforeStatus(OperationTask operationTask) {
    OperationDb operation = operationTask.getOperation();
    OperationStatus status = operation.getStatus();
    if (!operationTask.isActive() || status != INIT) {
      // This should never happen
      InternalException ex =
          new InternalException(
              String.format("Before operation task - active: '%s'", operationTask.isActive()));
      log.error(ExceptionsUtils.format(ex, operation));
      operationTask.setActive(false);
      save(operation, ERROR_INIT, String.format("Error in init phase - Code: %s", ex.getCode()));
      // Don't throw in Production environment ?
      throw ex;
    }
  }

  // This check is only done for asserting that this works as expected
  private void checkAfterStatus(OperationTask operationTask) {
    OperationDb operation = operationTask.getOperation();
    OperationStatus status = operation.getStatus();
    if (!((operationTask.isActive() && status == RUN)
        || (!operationTask.isActive() && status == INIT))) {
      // This should never happen
      InternalException ex =
          new InternalException(
              String.format("After operation task - active: '%s'", operationTask.isActive()));
      log.error(ExceptionsUtils.format(ex, operation));
      operationTask.setActive(false);
      save(operation, ERROR_INIT, String.format("Error in init phase - Code: %s", ex.getCode()));
      // Don't throw in Production environment ?
      throw ex;
    }
  }

  private void save(OperationDb operation, OperationStatus status, String statusDetail) {
    operation.setStatus(status);
    operation.setMessage(statusDetail);
    operation.setModified(LocalDateTime.now());
    operationService.save(operation);
  }

  private class UnStasher extends Thread {
    @Override
    public void run() {
      while (isStarted) {
        try {
          // We could optimize the synchronized
          // by locking by tenant and not for all tenants.
          synchronized (lock) {
            var iterator = stashedTasks.entrySet().iterator();
            while (iterator.hasNext()) {
              var entry = iterator.next();
              if (!operationService.isLocked(entry.getKey())) {
                List<OperationTask> tasks = entry.getValue();
                unStashExclusiveTasksFirst(tasks);
                if (tasks.isEmpty()) {
                  iterator.remove();
                }
              }
            }
          }
          Utils.sleep(2000);
        } catch (Exception ex) {
          EsafeException e = new InternalException(ex);
          log.error("UnStasher failed", e);
        }
      }
    }

    private void unStashExclusiveTasksFirst(List<OperationTask> tasks) {
      var it = tasks.iterator();
      while (it.hasNext()) {
        OperationTask task = it.next();
        if (task.isExclusive()) {
          it.remove();
          task.setActive(true);
          submit(task);
          return;
        }
      }

      tasks.forEach(
          task -> {
            task.setActive(true);
            submit(task);
          });
      tasks.clear();
    }
  }
}
