package org.eclipse.hono.analysis.action.pojo;

import java.util.Arrays;

public class RuleOutput extends BaseAnalysisResultElement {
  /**
   * 
   */
  private static final long serialVersionUID = -4534207484373875670L;
  private Rule rule;
  public Rule getRule() {
    return rule;
  }
  public void setRule(Rule rule) {
    this.rule = rule;
  }
  @Override
  public String toString() {
    return "RuleOutput [rule=" + rule + ", getTimestamp()=" + getTimestamp() + ", getEvent()="
        + Arrays.toString(getEvent()) + ", getDeviceType()=" + getDeviceType() + ", getDeviceId()=" + getDeviceId()
        + ", toString()=" + super.toString() + ", getTenantId()=" + getTenantId() + ", getId()=" + getId() + "]";
  }

}
