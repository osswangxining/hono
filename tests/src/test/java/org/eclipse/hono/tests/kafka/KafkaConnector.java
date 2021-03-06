package org.eclipse.hono.tests.kafka;

import java.util.Properties;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

public class KafkaConnector {
  KafkaProducer<String, String> producer;

  public KafkaConnector(String brokerList) {
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
          RecordMetadata recordMetadata = producer.send(record).get();
          System.out.println(recordMetadata);
      } catch (Exception e) {
          e.printStackTrace();
      }

  }


  public void close() {
      producer.close();
  }

  public static void main(String[] args) {

      String brokerlist = "127.0.0.1:9092";

      String topic = "data.event";
      KafkaConnector connector = new KafkaConnector(brokerlist);
      for(int i = 0; i < 10; i++) {
          connector.sendRecord(topic, i+"DEFAULT_TENANT_4714", i+"Message from MqttPublishSample");
      }
      connector.close();
  }
}
