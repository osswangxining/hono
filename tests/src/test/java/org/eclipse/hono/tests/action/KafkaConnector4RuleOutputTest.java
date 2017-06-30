package org.eclipse.hono.tests.action;

import java.util.Properties;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.eclipse.hono.analysis.action.Application;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class KafkaConnector4RuleOutputTest {

  KafkaProducer<String, String> producer;

  public KafkaConnector4RuleOutputTest(String brokerList) {
    Properties props = new Properties();

    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, brokerList);
    props.put(ProducerConfig.ACKS_CONFIG, "all");
    props.put(ProducerConfig.CLIENT_ID_CONFIG, "producer");
    props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");

    producer = new KafkaProducer<>(props);
  }

  /**
   * Returns the actual number of sent records
   */
  public void sendRecord(String topic, String key, String value) {

    try {
      ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, value);
      producer.send(record);
    } catch (Exception e) {
      e.printStackTrace();
    }

  }

  public void close() {
    producer.close();
  }

  private static String getRuleOutputString(int i) {
    JsonObject root = new JsonObject();
    root.addProperty("id", "ruleoutput" + i);

    JsonObject rule = new JsonObject();
    rule.addProperty("id", "ruleId");
    rule.addProperty("name", "ruleName");
    rule.addProperty("description", "ruleDescription");
    rule.addProperty("condition", "ruleCondition");
    JsonArray messageSchemas = new JsonArray();
    messageSchemas.add("phone");
    rule.add("messageSchemas", messageSchemas);
    rule.addProperty("disabled", false);
    rule.addProperty("severity", 4);
    JsonArray actions = new JsonArray();
    rule.add("actions", actions);
    for (int j = 0; j < 3; j++) {
      JsonObject action = new JsonObject();
      action.addProperty("id", "action" + j);
      action.addProperty("name", "name" + j);
      action.addProperty("description", "description" + j);
      action.addProperty("type", "webhook");

      JsonObject operation = new JsonObject();
      // operation.addProperty("url",
      // "https://yul1z0.messaging.lon02-1.test.internetofthings.ibmcloud.com/api/v0002/application/types/dt0/devices/rti-test1/events/httpevent");
      operation.addProperty("url", "http://www.baidu.com");
      operation.addProperty("method", "post");
      // operation.addProperty("username", "username");
      // operation.addProperty("password", "password");
      operation.addProperty("contentType", "application/json");
      JsonArray headers = new JsonArray();
      for (int k = 0; k < 1; k++) {
        JsonObject head = new JsonObject();
        head.addProperty("key", "authorization");
        head.addProperty("value", "Basic YS15dWwxejAtcWE1a2FpZHUzYTpWS0NEait6PyFUb09Ab1U5UXI=");
        headers.add(head);
      }
      operation.add("headers", headers);
      operation.addProperty("body",
          "{ \"s1\": " + (100 + j) + ",\"s2\": " + (30 + j) + ",\"time\":" + (System.currentTimeMillis()) + "}");
      operation.addProperty("retry", 2);
      action.add("operation", operation);
      actions.add(action);
    }
    root.add("rule", rule);

    root.addProperty("timestamp", System.currentTimeMillis());
    root.addProperty("tenantId", "tenantId");
    root.addProperty("deviceType", "deviceType");
    root.addProperty("deviceId", "deviceId");
    JsonArray event = new JsonArray();
    root.add("event", event);

    return root.toString();
  }

  private static String getRuleOutputString4Mail(int i) {
    JsonObject root = new JsonObject();
    root.addProperty("id", "ruleoutput" + i);

    JsonObject rule = new JsonObject();
    rule.addProperty("id", "ruleId");
    rule.addProperty("name", "ruleName");
    rule.addProperty("description", "ruleDescription");
    rule.addProperty("condition", "ruleCondition");
    JsonArray messageSchemas = new JsonArray();
    messageSchemas.add("phone");
    rule.add("messageSchemas", messageSchemas);
    rule.addProperty("disabled", false);
    rule.addProperty("severity", 4);
    JsonArray actions = new JsonArray();
    rule.add("actions", actions);
    for (int j = 0; j < 3; j++) {
      JsonObject action = new JsonObject();
      action.addProperty("id", "action" + j);
      action.addProperty("name", "name" + j);
      action.addProperty("description", "description" + j);
      action.addProperty("type", "mail");

      JsonArray to = new JsonArray();
      to.add("osswangxining@126.com");
      to.add("osswangxining@126.com");
      action.add("to", to);
      action.addProperty("title", "title");
      action.addProperty("includeRawData", true);
      actions.add(action);
    }
    root.add("rule", rule);

    root.addProperty("timestamp", System.currentTimeMillis());
    root.addProperty("tenantId", "tenantId");
    root.addProperty("deviceType", "deviceType");
    root.addProperty("deviceId", "deviceId");
    JsonArray event = new JsonArray();
    event.add("event11");
    root.add("event", event);

    return root.toString();
  }

  public static void main(String[] args) {

    String brokerlist = "127.0.0.1:9092";
    System.out.println("beginning..........");
    String topic = Application.TOPIC_RULE_OUTPUT;
    KafkaConnector4RuleOutputTest connector = new KafkaConnector4RuleOutputTest(brokerlist);
    System.out.println("beginning..........");

    for (int i = 0; i < 1; i++) {
      String webhookevent = getRuleOutputString4Mail(i);
      System.out.println(topic + i + "DEFAULT_TENANT_4714" + webhookevent);

      connector.sendRecord(topic, i + "DEFAULT_TENANT_4714", webhookevent);
    }
    connector.close();
  }
}
