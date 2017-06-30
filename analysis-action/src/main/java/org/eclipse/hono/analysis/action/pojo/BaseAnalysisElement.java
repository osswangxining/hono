package org.eclipse.hono.analysis.action.pojo;

import java.io.Serializable;

public class BaseAnalysisElement implements Serializable  {

  /**
   * 
   */
  private static final long serialVersionUID = -7306495228021718284L;

  private String tenantId;
  private String id;
  public String getTenantId() {
    return tenantId;
  }
  public void setTenantId(String tenantId) {
    this.tenantId = tenantId;
  }
  public String getId() {
    return id;
  }
  public void setId(String id) {
    this.id = id;
  }
  @Override
  public String toString() {
    return "BaseAnalysisElement [tenantId=" + tenantId + ", id=" + id + "]";
  }
}
