/**
 * Copyright (c) 2016, 2017 Red Hat and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Red Hat - initial creation
 *    Bosch Software Innovations GmbH - extend AbtractApplication
 */

package org.eclipse.hono.streaming.analysis;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * The Hono Streaming Analysis main application class.
 */
@ComponentScan(basePackages = "org.eclipse.hono.streaming.analysis")
@Configuration
@EnableAutoConfiguration
@SpringBootApplication
public class Application implements CommandLineRunner {
  private static final Logger LOGGER = Logger.getLogger(Application.class.getName());

  private static final String TELEMETRY_ENDPOINT = "telemetry";
  private static final String EVENT_ENDPOINT = "event";
  public KafkaConsumer<String, String> consumer;
 

  /**
   * Starts the Streaming Analysis application.
   * 
   * @param args
   *          Command line args passed to the application.
   */
  public static void main(final String[] args) {
//    SpringApplication.run(Application.class, args);
    SpringApplication app = new SpringApplication(Application.class);
    app.setBannerMode(Banner.Mode.CONSOLE);
    app.run(args);

  }

  @Override
  public void run(String... arg0) throws Exception {
    startConsumer();
  }
  
  private void startConsumer() {
    String kafkaConnection = System.getenv("KAFKA_CONNECTION");
//    kafkaConnection = "127.0.0.1:9002";
    LOGGER.log(Level.INFO, "kafkaConnection: {0}", kafkaConnection);
    Properties props = new Properties();
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConnection);
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
    
    List<String> topics = new ArrayList<String>();
    topics.add("data." + TELEMETRY_ENDPOINT);
    topics.add("data." + EVENT_ENDPOINT);
    
    consumer.subscribe(topics);
    try {
      while (true) {
        ConsumerRecords<String, String> records = consumer.poll(Long.MAX_VALUE);
        for (TopicPartition partition : records.partitions()) {
          List<ConsumerRecord<String, String>> partitionRecords = records.records(partition);
          for (ConsumerRecord<String, String> record : partitionRecords) {
            String key = record.key();
            String value = record.value();
            String topic = record.topic();
            KafkaConnector connector = new KafkaConnector(kafkaConnection);
            String targetTopic = "action.webhook";
            
            LOGGER.log(Level.INFO, "topic: {0}, key: {1}, value: {2}", new Object[]{topic, key, value});
            if(topic != null) {
              connector.sendRecord(targetTopic, key, value, (metadata, exception) -> {
                if (exception != null) {
                  LOGGER.log(Level.INFO, exception.getMessage());
                } else {
                  LOGGER.log(Level.INFO, "Message has been successfully sent.");
                }
              });
            }
            connector.close();
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
}
