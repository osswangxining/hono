package org.eclipse.hono.analysis.action.pojo;

import java.util.Arrays;

public class Rule extends BaseAnalysisElement {
  /**
   * 
   */
  private static final long serialVersionUID = -4534207484373875670L;
  private String name;
  private String description;
  private String[] messageSchemas;

  private String condition;
  private Action[] actions;

  private int severity;
  private boolean disabled;

  public String getCondition() {
    return condition;
  }

  public void setCondition(String condition) {
    this.condition = condition;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String[] getMessageSchemas() {
    return messageSchemas;
  }

  public void setMessageSchemas(String[] messageSchemas) {
    this.messageSchemas = messageSchemas;
  }

  public Action[] getActions() {
    return actions;
  }

  public void setActions(Action[] actions) {
    this.actions = actions;
  }

  public int getSeverity() {
    return severity;
  }

  public void setSeverity(int severity) {
    this.severity = severity;
  }

  public boolean isDisabled() {
    return disabled;
  }

  public void setDisabled(boolean disabled) {
    this.disabled = disabled;
  }

  @Override
  public String toString() {
    return "Rule [name=" + name + ", description=" + description + ", messageSchemas=" + Arrays.toString(messageSchemas)
        + ", condition=" + condition + ", actions=" + Arrays.toString(actions) + ", severity=" + severity
        + ", disabled=" + disabled + ", getTenantId()=" + getTenantId() + ", getId()=" + getId() + "]";
  }

}
