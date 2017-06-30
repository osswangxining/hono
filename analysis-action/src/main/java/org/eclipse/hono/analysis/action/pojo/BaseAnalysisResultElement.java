package org.eclipse.hono.analysis.action.pojo;

import java.util.Arrays;

public class BaseAnalysisResultElement extends BaseAnalysisElement  {
  /**
   * 
   */
  private static final long serialVersionUID = -4460723913829594905L;
  private String timestamp;
  private String[] event;
  private String deviceType;
  private String deviceId;
  public String getTimestamp() {
    return timestamp;
  }
  public void setTimestamp(String timestamp) {
    this.timestamp = timestamp;
  }
  public String[] getEvent() {
    return event;
  }
  public void setEvent(String[] event) {
    this.event = event;
  }
  public String getDeviceType() {
    return deviceType;
  }
  public void setDeviceType(String deviceType) {
    this.deviceType = deviceType;
  }
  public String getDeviceId() {
    return deviceId;
  }
  public void setDeviceId(String deviceId) {
    this.deviceId = deviceId;
  }
  @Override
  public String toString() {
    return "BaseAnalysisResultElement [timestamp=" + timestamp + ", event=" + Arrays.toString(event) + ", deviceType="
        + deviceType + ", deviceId=" + deviceId + ", getTenantId()=" + getTenantId() + ", getId()=" + getId() + "]";
  }
}
