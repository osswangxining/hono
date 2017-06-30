package org.eclipse.hono.analysis.action.pojo;

public class Action extends BaseAnalysisResultElement {

  /**
   * 
   */
  private static final long serialVersionUID = 5802559775521017189L;
  private String description;
  private String name;
  private String type;
  private Rule rule;

  public Action() {
    super();
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

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public Rule getRule() {
    return rule;
  }

  public void setRule(Rule rule) {
    this.rule = rule;
  }

}