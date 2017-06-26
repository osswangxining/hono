package org.eclipse.hono.service.kafka;

import java.util.Properties;

import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;

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
   * @param callback 
   */
  public void sendRecord(String topic,  String key, String value, Callback callback) {

      try{
          ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, value);
          producer.send(record, callback);
      } catch (Exception e) {
          e.printStackTrace();
      }

  }


  public void close() {
      producer.close();
  }

//  public static void main(String[] args) {
//
//      String brokerlist = "127.0.0.1:9092";
//
//      String topic = "topicname";
//      KafkaConnector connector = new KafkaConnector(brokerlist);
//      for(int i = 0; i < 200; i++) {
//          connector.sendRecord(topic, i+"", i+"");
//      }
//      connector.close();
//  }
}
