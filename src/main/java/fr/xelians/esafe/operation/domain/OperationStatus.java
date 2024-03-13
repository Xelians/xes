/*
 * Ce programme est un logiciel libre. Vous pouvez le modifier, l'utiliser et
 * le redistribuer en respectant les termes de la license Ceccil v2.1.
 */

package fr.xelians.esafe.operation.domain;

public enum OperationStatus {

  // Initialization => RUN, ERROR_INIT
  INIT(OperationState.RUNNING),

  // Initialization failed with technical error => Final Operation
  ERROR_INIT(OperationState.COMPLETED),

  // Check functional requirement then or commit to offers => ERROR_CHECK,  ERROR_COMMIT
  RUN(OperationState.RUNNING),

  // Check failed with functional or technical error => Final Operation
  ERROR_CHECK(OperationState.COMPLETED),

  // Commit failed with technical error => Final Operation
  ERROR_COMMIT(OperationState.COMPLETED),

  // Backup referentials on offers => ERROR_BACKUP
  BACKUP(OperationState.RUNNING),

  // Store object on offers => INDEX, ERROR_STORE, RETRY_STORE
  STORE(OperationState.RUNNING),

  // Store failed with technical error => STORED
  RETRY_STORE(OperationState.RUNNING),

  // Index object on search engine  => OK, RETRY_INDEX
  INDEX(OperationState.RUNNING),

  // Indexation failed with technical error => INDEX
  RETRY_INDEX(OperationState.RUNNING),

  // Process was successfully completed => Final Operation
  OK(OperationState.COMPLETED),

  // Process failed with unexpected technical that could not be retried => Final Operation
  FATAL(OperationState.COMPLETED);

  private final OperationState state;

  OperationStatus(OperationState state) {
    this.state = state;
  }

  public boolean isRunning() {
    return this.state == OperationState.RUNNING;
  }

  public boolean isCompleted() {
    return this.state == OperationState.COMPLETED;
  }

  public OperationState getState() {
    return state;
  }

  public boolean isOK() {
    return this == OK;
  }
}
