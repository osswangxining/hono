package org.eclipse.hono.tests.action;

import java.util.Properties;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class KafkaConnector4WebHookTest {
  
  KafkaProducer<String, String> producer;

  public KafkaConnector4WebHookTest(String brokerList) {
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
  public void sendRecord(String topic,  String key, String value) {

      try{
          ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, value);
          producer.send(record);
      } catch (Exception e) {
          e.printStackTrace();
      }

  }


  public void close() {
      producer.close();
  }

  private static String getWebHookString(int i){
    JsonObject root = new JsonObject();
    root.addProperty("id", "webhook" + i);
    root.addProperty("name", "name" + i);
    root.addProperty("description", "description" + i);
    root.addProperty("type", "webhook");
    
    JsonObject operation = new JsonObject();
    operation.addProperty("url", "https://yul1z0.messaging.lon02-1.test.internetofthings.ibmcloud.com/api/v0002/application/types/dt0/devices/rti-test1/events/httpevent");
    operation.addProperty("method", "post");
//    operation.addProperty("username", "username");
//    operation.addProperty("password", "password");
    operation.addProperty("contentType", "application/json");
    JsonArray headers = new JsonArray();
    for (int j = 0; j < 1; j++) {
      JsonObject head = new JsonObject();
      head.addProperty("key", "authorization");
      head.addProperty("value", "Basic YS15dWwxejAtcWE1a2FpZHUzYTpWS0NEait6PyFUb09Ab1U5UXI=");
      headers.add(head);
    }
    operation.add("headers", headers);
    operation.addProperty("body", "{ \"s1\": 101,\"s2\": 30,\"time\":1498635845205}");
    operation.addProperty("retry",2);
    root.add("operation", operation);

    JsonObject rule = new JsonObject();
    rule.addProperty("id", "ruleId");
    rule.addProperty("name", "ruleName");
    rule.addProperty("description", "ruleDescription");
    rule.addProperty("condition", "ruleCondition");
    root.add("rule", rule);
    
    root.addProperty("timestamp", System.currentTimeMillis());
    root.addProperty("tenantId", "tenantId");
    root.addProperty("deviceType", "deviceType");
    root.addProperty("deviceId", "deviceId");
    JsonArray event = new JsonArray();
    root.add("event", event);
    
    return root.toString();
  }
  
  public static void main(String[] args) {

      String brokerlist = "127.0.0.1:9092";

      String topic = "action.webhook";
      KafkaConnector4WebHookTest connector = new KafkaConnector4WebHookTest(brokerlist);
      for(int i = 0; i < 100; i++) {
        String webhookevent = getWebHookString(i);
          connector.sendRecord(topic, i+"DEFAULT_TENANT_4714", webhookevent);
          System.out.println(topic + i+"DEFAULT_TENANT_4714" + webhookevent);
      }
      connector.close();
  }
}
