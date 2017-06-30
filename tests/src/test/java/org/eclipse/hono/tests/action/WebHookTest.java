package org.eclipse.hono.tests.action;

import org.eclipse.hono.analysis.action.WebHookAction;

public class WebHookTest {

  public static void main(String[] args) {
    String kafkaConnection = System.getenv("KAFKA_CONNECTION");
    if (kafkaConnection == null) {
      kafkaConnection = "127.0.0.1:9092";
    }
    String consumerGroup = System.getenv("KAFKA_CONSUMER_GROUP");
    if (consumerGroup == null) {
      consumerGroup = "group" + System.currentTimeMillis();
    }
    WebHookAction.INSTANCE.startConsumer(kafkaConnection, consumerGroup);

    // HttpPost post = new HttpPost(
    // "https://yul1z0.messaging.lon02-1.test.internetofthings.ibmcloud.com/api/v0002/application/types/dt0/devices/rti-test1/events/httpevent");
    //
    // String input = "{\"ruleCondition\":\"dt0schema.s1>100 OR
    // dt0schema.s2>25\",\"ruleName\":\"cr0\",\"message\":\"{\\\"count\\\":24,\\\"time\\\":1498307533158,\\\"s1\\\":101,\\\"s2\\\":30}\",\"ruleId\":\"dKey93Fz\",\"deviceId\":\"yul1z0:dt0:rti-test0\",\"orgId\":\"yul1z0\",\"ruleDescription\":\"\",\"timestamp\":\"1498307533334\"}";
    // StringEntity inputEntity = new StringEntity(input, "UTF-8");
    // post.setEntity(inputEntity);
    // post.addHeader("Content-Type", "application/json");
    // post.addHeader("Accept", "application/json");
    // post.setHeader("Connection", "close");
    // String authEncodedString =
    // "YS15dWwxejAtcWE1a2FpZHUzYTpWS0NEait6PyFUb09Ab1U5UXI=";
    // post.addHeader("Authorization", "Basic " + authEncodedString);

  }

}
