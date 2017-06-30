package org.eclipse.hono.analysis.action.pojo;

import java.util.Arrays;

public class Mail extends Action {

  /**
   * 
   */
  private static final long serialVersionUID = -1112470330297312709L;

  private String[] to;
  private String[] cc;
  private String title;
  private String body;
  private boolean includeRawData;
  
  public String[] getTo() {
    return to;
  }
  public void setTo(String[] to) {
    this.to = to;
  }
  public String[] getCc() {
    return cc;
  }
  public void setCc(String[] cc) {
    this.cc = cc;
  }
  public String getTitle() {
    return title;
  }
  public void setTitle(String title) {
    this.title = title;
  }
  public String getBody() {
    return body;
  }
  public void setBody(String body) {
    this.body = body;
  }
  public boolean isIncludeRawData() {
    return includeRawData;
  }
  public void setIncludeRawData(boolean includeRawData) {
    this.includeRawData = includeRawData;
  }
  @Override
  public String toString() {
    return "Mail [to=" + Arrays.toString(to) + ", cc=" + Arrays.toString(cc) + ", title=" + title + ", body=" + body
        + ", includeRawData=" + includeRawData + ", getDescription()=" + getDescription() + ", getName()=" + getName()
        + ", getType()=" + getType() + ", getRule()=" + getRule() + ", getTimestamp()=" + getTimestamp()
        + ", getEvent()=" + Arrays.toString(getEvent()) + ", getDeviceType()=" + getDeviceType() + ", getDeviceId()="
        + getDeviceId() + ", toString()=" + super.toString() + ", getTenantId()=" + getTenantId() + ", getId()="
        + getId() + "]";
  }
}
