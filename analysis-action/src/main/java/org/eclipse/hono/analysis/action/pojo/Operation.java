package org.eclipse.hono.analysis.action.pojo;

import java.util.Arrays;

public class Operation {
  private Headers[] headers;

  private String body;

  private String username;

  private String method;

  private String contentType;

  private String retry;

  private String password;

  private String url;

  public Headers[] getHeaders() {
    return headers;
  }

  public void setHeaders(Headers[] headers) {
    this.headers = headers;
  }

  public String getBody() {
    return body;
  }

  public void setBody(String body) {
    this.body = body;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getMethod() {
    return method;
  }

  public void setMethod(String method) {
    this.method = method;
  }

  public String getContentType() {
    return contentType;
  }

  public void setContentType(String contentType) {
    this.contentType = contentType;
  }

  public String getRetry() {
    return retry;
  }

  public void setRetry(String retry) {
    this.retry = retry;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  @Override
  public String toString() {
    return "Operation [headers=" + Arrays.toString(headers) + ", body=" + body + ", username=" + username + ", method="
        + method + ", contentType=" + contentType + ", retry=" + retry + ", password=" + password + ", url=" + url
        + "]";
  }
}