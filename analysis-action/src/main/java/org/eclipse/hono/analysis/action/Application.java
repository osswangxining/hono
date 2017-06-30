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

package org.eclipse.hono.analysis.action;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.eclipse.hono.analysis.action.pojo.Action;
import org.eclipse.hono.analysis.action.pojo.ActionElementAdapter;
import org.eclipse.hono.analysis.action.pojo.Mail;
import org.eclipse.hono.analysis.action.pojo.Rule;
import org.eclipse.hono.analysis.action.pojo.RuleOutput;
import org.eclipse.hono.analysis.action.pojo.WebHook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * The Hono Streaming Analysis main application class.
 */
@ComponentScan(basePackages = "org.eclipse.hono.analysis.action")
@Configuration
@EnableAutoConfiguration
@SpringBootApplication
public class Application implements CommandLineRunner {
  private static final Logger LOGGER = LoggerFactory.getLogger(Application.class);

  public static final String TOPIC_RULE_OUTPUT = "rule.output";
  public static final String TOPIC_ACTION_WEBHOOK = "action.webhook";
  public static final String TOPIC_ACTION_MAIL = "action.mail";
  public static final List<String> TOPIC_ACTION_ALL = Arrays.asList(TOPIC_ACTION_WEBHOOK, TOPIC_ACTION_MAIL);
  public static final String ACTION_TYPE_WEBHOOK = "webhook";
  public static final String ACTION_TYPE_MAIL = "mail";
  public static final List<String> ACTION_TYPE_ALL = Arrays.asList(ACTION_TYPE_WEBHOOK, ACTION_TYPE_MAIL);

  /**
   * Starts the Streaming Analysis application.
   * 
   * @param args
   *          Command line args passed to the application.
   */
  public static void main(final String[] args) {
    // SpringApplication.run(Application.class, args);
    SpringApplication app = new SpringApplication(Application.class);
    app.setBannerMode(Banner.Mode.CONSOLE);
    app.run(args);

    Runtime.getRuntime().addShutdownHook(new Thread() {
      public void run() {
        System.out.println("shutting down");
      }
    });
  }

  @Override
  public void run(String... arg0) throws Exception {
    String kafkaConnection = System.getenv("KAFKA_CONNECTION");
    if (kafkaConnection == null) {
      kafkaConnection = "127.0.0.1:9092";
    }

    new WebHookActionThread(kafkaConnection).start();
    new MailActionThread(kafkaConnection).start();
    new MainActionThread(kafkaConnection).start();
  }

  class WebHookActionThread extends Thread {
    private String _kafkaConnection;

    public WebHookActionThread(String kafkaConnection) {
      this._kafkaConnection = kafkaConnection;
    }

    @Override
    public void run() {
      String webhookConsumerGroup = System.getenv("KAFKA_WEBHOOK_CONSUMER_GROUP");
      if (webhookConsumerGroup == null) {
        webhookConsumerGroup = "webhookConsumerGroup" + System.currentTimeMillis();
      }
      WebHookAction.INSTANCE.startConsumer(_kafkaConnection, webhookConsumerGroup);
    }
  }

  class MailActionThread extends Thread {
    private String _kafkaConnection;

    public MailActionThread(String kafkaConnection) {
      this._kafkaConnection = kafkaConnection;
    }

    @Override
    public void run() {
      String mailConsumerGroup = System.getenv("KAFKA_MAIL_CONSUMER_GROUP");
      if (mailConsumerGroup == null) {
        mailConsumerGroup = "mailConsumerGroup" + System.currentTimeMillis();
      }
      MailAction.INSTANCE.startConsumer(_kafkaConnection, mailConsumerGroup);
    }
  }

  class MainActionThread extends Thread {
    private String _kafkaConnection;

    public MainActionThread(String kafkaConnection) {
      this._kafkaConnection = kafkaConnection;
    }

    @Override
    public void run() {
      String mainConsumerGroup = System.getenv("KAFKA_MAIN_CONSUMER_GROUP");
      if (mainConsumerGroup == null) {
        mainConsumerGroup = "mainConsumerGroup" + System.currentTimeMillis();
      }
      startConsumer(_kafkaConnection, mainConsumerGroup);

    }
  }

  private void startConsumer(String kafkaConnection, String consumerGroup) {

    LOGGER.info("kafkaConnection: {}, consumerGroup: {}", new Object[] { kafkaConnection, consumerGroup });
    Properties props = new Properties();
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConnection);
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest"); // "earliest"
    // else
    // "latest"
    props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
    props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
        "org.apache.kafka.common.serialization.StringDeserializer");
    props.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroup);

    KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);

    KafkaConnector connector = new KafkaConnector(kafkaConnection);

    List<String> topics = Arrays.asList(TOPIC_RULE_OUTPUT);

    consumer.subscribe(topics);
    try {
      while (true) {
        ConsumerRecords<String, String> records = consumer.poll(Long.MAX_VALUE);
        long begin = System.currentTimeMillis();
        for (TopicPartition partition : records.partitions()) {
          List<ConsumerRecord<String, String>> partitionRecords = records.records(partition);
          for (ConsumerRecord<String, String> record : partitionRecords) {
            String key = record.key();
            String value = record.value();
            String topic = record.topic();
            LOGGER.info("topic: {}, key: {}, value: {}", new Object[] { topic, key, value });
            List<String[]> result = preprocessor(value);
            if (result == null || result.size() == 0) {
              LOGGER.warn("no related actions for this rule output: {}", value);
              continue;
            }

            for (String[] strings : result) {
              String actionTopic = strings[0];
              String actionKey = key; // use same key: deviceid
              String actionValue = strings[2];
              LOGGER.info("topic for action: {}, key: {}, value: {}",
                  new Object[] { actionTopic, actionKey, actionValue });
              if (actionTopic != null) {
                connector.sendRecord(actionTopic, actionKey, actionValue, new Callback() {

                  @Override
                  public void onCompletion(RecordMetadata metadata, Exception exception) {
                    if (exception != null) {
                      exception.printStackTrace();
                      LOGGER.error(
                          "topic={}, key={}, value={}, The offset of the record we just sent is: {}, exception: {}",
                          new Object[] { actionTopic, actionKey, actionValue, metadata.offset(),
                              exception.getMessage() });
                    } else {
                      LOGGER.info("The message has been sent successfully.");
                    }
                  }
                });
              }
            }
          }
        }

        long consumedTime = System.currentTimeMillis() - begin;
        LOGGER.info("consumed time(ms): {}", new Object[] { consumedTime });
        consumer.commitSync();
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      consumer.close();
      connector.close();
    }
  }

  private List<String[]> preprocessor(String value) {
    Gson gson = new GsonBuilder().registerTypeAdapter(Action.class, new ActionElementAdapter()).create();
    RuleOutput ruleOutput = gson.fromJson(value, RuleOutput.class);
    if (ruleOutput == null)
      return null;
    Rule rule = ruleOutput.getRule();
    if (rule == null) {
      return null;
    }
    Action[] actions = rule.getActions();
    if (actions == null || actions.length < 1) {
      return null;
    }
    List<String[]> result = new ArrayList<>();
    Rule simplerule = new Rule();
    simplerule.setId(rule.getId());
    simplerule.setCondition(rule.getCondition());
    simplerule.setDescription(rule.getDescription());
    simplerule.setDisabled(rule.isDisabled());
    simplerule.setMessageSchemas(rule.getMessageSchemas());
    simplerule.setName(rule.getName());
    simplerule.setSeverity(rule.getSeverity());
    simplerule.setTenantId(rule.getTenantId());

    for (Action action : actions) {
      String type = action.getType();
      if (type == null || !ACTION_TYPE_ALL.contains(type)) {
        continue;
      }
      String[] strings = new String[3];
      if (type.equalsIgnoreCase(ACTION_TYPE_WEBHOOK)) {
        strings[0] = Application.TOPIC_ACTION_WEBHOOK;
      } else if (type.equalsIgnoreCase(ACTION_TYPE_MAIL)) {
        strings[0] = Application.TOPIC_ACTION_MAIL;
      }

      strings[1] = "";// @ TODO
      action.setDeviceId(ruleOutput.getDeviceId());
      action.setDeviceType(ruleOutput.getDeviceType());
      action.setEvent(ruleOutput.getEvent());
      action.setTenantId(ruleOutput.getTenantId());
      action.setTimestamp(ruleOutput.getTimestamp());
      action.setRule(simplerule);

      if (action instanceof WebHook) {
        WebHook webhook = (WebHook) action;
        strings[2] = gson.toJson(webhook);
      } else if (action instanceof Mail) {
        Mail mail = (Mail) action;
        strings[2] = gson.toJson(mail);
      } else {
        strings[2] = gson.toJson(action);
      }
      result.add(strings);
    }
    return result;
  }
}
