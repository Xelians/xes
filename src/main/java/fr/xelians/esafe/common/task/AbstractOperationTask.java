/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.common.task;

import fr.xelians.esafe.common.domain.TaskEvent;
import fr.xelians.esafe.common.exception.EsafeException;
import fr.xelians.esafe.common.json.JsonService;
import fr.xelians.esafe.common.utils.ExceptionsUtils;
import fr.xelians.esafe.common.utils.NioUtils;
import fr.xelians.esafe.operation.domain.Workspace;
import fr.xelians.esafe.operation.entity.OperationDb;
import fr.xelians.esafe.operation.service.OperationService;
import fr.xelians.esafe.organization.service.TenantService;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractOperationTask<T> implements OperationTask<T> {

  @Getter protected final OperationDb operation;
  @Getter protected final List<TaskEvent> events = new ArrayList<>();
  @Getter @Setter protected boolean isActive;

  protected final OperationService operationService;
  protected final TenantService tenantService;

  protected AbstractOperationTask(
      OperationDb operation, OperationService operationService, TenantService tenantService) {
    this.operation = operation;
    this.isActive = true;
    this.operationService = operationService;
    this.tenantService = tenantService;
  }

  public boolean isExclusive() {
    return operation.isExclusive();
  }

  @Override
  public void clean() {
    Path ws = Workspace.getPath(operation);
    NioUtils.deleteDirQuietly(ws);
  }

  protected String format(String title, EsafeException ex) {
    return ExceptionsUtils.format(title, ex, operation);
  }

  protected void logEvent(String event) {
    // FIXME Change this temporary implementation (lack of time with performance tests & deadlines)
    this.events.add(new TaskEvent(event, System.nanoTime()));
    this.operation.setEvents(JsonService.toString(this.events));
  }
}
