package org.eclipse.hono.analysis.action.pojo;

public class Headers {
  private String value;

  private String key;

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  @Override
  public String toString() {
    return "Headers [value=" + value + ", key=" + key + "]";
  }

}
