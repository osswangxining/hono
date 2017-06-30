package org.eclipse.hono.analysis.action.pojo;

import java.util.Arrays;

public class WebHook extends Action {
  /**
   * 
   */
  private static final long serialVersionUID = 4479125718761620090L;
  private Operation operation;

  public Operation getOperation() {
    return operation;
  }

  public void setOperation(Operation operation) {
    this.operation = operation;
  }

  @Override
  public String toString() {
    return "WebHook [getOperation()=" + getOperation() + ", getTenantId()=" + getTenantId() + ", getTimestamp()="
        + getTimestamp() + ", getId()=" + getId() + ", getEvent()=" + Arrays.toString(getEvent()) + ", getDeviceType()="
        + getDeviceType() + ", getDescription()=" + getDescription() + ", getName()=" + getName() + ", getType()="
        + getType() + ", getDeviceId()=" + getDeviceId() + ", getRule()=" + getRule() + ", toString()="
        + super.toString() + ", getClass()=" + getClass() + ", hashCode()=" + hashCode() + "]";
  }
}