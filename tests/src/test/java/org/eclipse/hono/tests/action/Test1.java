package org.eclipse.hono.tests.action;

import org.eclipse.hono.analysis.action.pojo.Action;
import org.eclipse.hono.analysis.action.pojo.WebHook;

public class Test1 {

  public static void main(String[] args) {
    // TODO Auto-generated method stub
    Action webhook = new WebHook();
    System.out.println(webhook.getClass());
  }

}
