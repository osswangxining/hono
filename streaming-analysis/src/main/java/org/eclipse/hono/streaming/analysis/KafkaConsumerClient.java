package org.eclipse.hono.streaming.analysis;

import java.util.List;
import java.util.Properties;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;

public class KafkaConsumerClient {
  public KafkaConsumer<String, String> consumer;

  public KafkaConsumerClient(String brokerList) {
    Properties props = new Properties();
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, brokerList);
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest"); // "earliest"
                                                                  // else
                                                                  // "latest"
    props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
    props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
        "org.apache.kafka.common.serialization.StringDeserializer");
    props.put(ConsumerConfig.GROUP_ID_CONFIG, "group");

    consumer = new KafkaConsumer<>(props);
  }

  public void process(List<String> topic) {
    consumer.subscribe(topic);
    try {
      while (true) {
        ConsumerRecords<String, String> records = consumer.poll(Long.MAX_VALUE);
        for (TopicPartition partition : records.partitions()) {
          List<ConsumerRecord<String, String>> partitionRecords = records.records(partition);
          for (ConsumerRecord<String, String> record : partitionRecords) {
            System.out.println(record.value());
          }
        }
        consumer.commitSync();

      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      consumer.close();
    }

  }

//  public static void main(String[] args) {
//    List<String> topic = new ArrayList<String>();
//    topic.add("topicname");
//
//    String brokerlist = "127.0.0.1:9092";
//    new KafkaConsumerClient(brokerlist).process(topic);
//
//  }
}
