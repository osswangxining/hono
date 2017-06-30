package org.eclipse.hono.analysis.action;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Pattern;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

public class MailAction {
  private static final Logger LOGGER = LoggerFactory.getLogger(MailAction.class);

  private static final Pattern PATTERN_PARAMETER_TIMESTAMP = Pattern.compile("[{][{]timestamp}}");
  private static final Pattern PATTERN_PARAMETER_TENANTID = Pattern.compile("[{][{]tenantId}}");
  private static final Pattern PATTERN_PARAMETER_DEVICETYPE = Pattern.compile("[{][{]deviceType}}");
  private static final Pattern PATTERN_PARAMETER_DEVICEID = Pattern.compile("[{][{]deviceId}}");
  private static final Pattern PATTERN_PARAMETER_RULEID = Pattern.compile("[{][{]ruleId}}");
  private static final Pattern PATTERN_PARAMETER_RULENAME = Pattern.compile("[{][{]ruleName}}");
  private static final Pattern PATTERN_PARAMETER_RULEDESCRIPTION = Pattern.compile("[{][{]ruleDescription}}");
  private static final Pattern PATTERN_PARAMETER_RULECONDITION = Pattern.compile("[{][{]ruleCondition}}");
  private static final Pattern PATTERN_PARAMETER_EVENT = Pattern.compile("[{][{]event}}");

  public static final MailAction INSTANCE = new MailAction();
  // key by device id
  private ConcurrentHashMap<String, List<String>> actions = new ConcurrentHashMap<>();

  private MailAction() {

  }

  public void startConsumer(String kafkaConnection, String consumerGroup) {

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

    List<String> topics = Arrays.asList(Application.TOPIC_ACTION_MAIL);

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
            List<String> list = actions.get(key);
            if (list == null) {
              list = new ArrayList<String>();
            }
            list.add(value);
            LOGGER.info("topic: {}, key: {}, value: {}", new Object[] { topic, key, value });

            actions.put(key, list);
          }
        }

        handle(actions);
        long consumedTime = System.currentTimeMillis() - begin;
        LOGGER.info("consumed time(ms): {}", new Object[] { consumedTime });
        consumer.commitSync();
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      consumer.close();
    }
  }

  private void handle(ConcurrentHashMap<String, List<String>> actions) {
    if (actions == null)
      return;

    if (actions.size() < 10) {
      Iterator<Entry<String, List<String>>> iterator = actions.entrySet().iterator();
      while (iterator.hasNext()) {
        Entry<String, List<String>> entry = iterator.next();
        List<String> values = entry.getValue();
        List<String> failedList = handle(values);
        if (failedList != null && !failedList.isEmpty()) {
          LOGGER.info("failed: {}", failedList.toString());
        }
      }
    } else {
      ExecutorService executorService = Executors.newFixedThreadPool(10);
      Iterator<Entry<String, List<String>>> iterator = actions.entrySet().iterator();
      while (iterator.hasNext()) {
        Entry<String, List<String>> entry = iterator.next();
        List<String> values = entry.getValue();
        MailProcessor processor = new MailProcessor(values);
        Future<List<String>> future = executorService.submit(processor);
        try {
          List<String> failedList = future.get();
          if (failedList != null && !failedList.isEmpty()) {
            LOGGER.info("failed: {}", failedList.toString());
          }
        } catch (InterruptedException | ExecutionException e) {
          e.printStackTrace();
        }
      }

      executorService.shutdown();
    }
  }

  class MailProcessor implements Callable<List<String>> {
    private List<String> values;

    public MailProcessor(List<String> values) {
      this.values = values;
    }

    @Override
    public List<String> call() throws Exception {
      return handle(values);
    }

  }

  private List<String> handle(List<String> values) {
    if (values == null)
      return null;
    List<String> failed = new ArrayList<>();
    for (String value : values) {
      String handle = handle(value);
      if (handle != null) {
        failed.add(handle);
      }
    }
    return failed;
  }

  /**
   * 
   * @param value
   */
  private String handle(String value) {
    JsonParser jsonParser = new JsonParser();
    JsonElement jsonElement = null;
    try {
      jsonElement = jsonParser.parse(value);
    } catch (Exception e) {
      e.printStackTrace();
    }
    if (jsonElement == null || !(jsonElement instanceof JsonObject)) {
      LOGGER.error("The value is not valid: {}", value);
      return value;
    }

    JsonObject jsonObject = (JsonObject) jsonElement;
    JsonPrimitive typeAsJsonPrimitive = jsonObject.getAsJsonPrimitive("type");
    if (typeAsJsonPrimitive == null) {
      LOGGER.error("The type of action should not be empty");
      return value;
    }
    if (!typeAsJsonPrimitive.getAsString().equalsIgnoreCase(Application.ACTION_TYPE_MAIL)) {
      LOGGER.error("The type of action should be \"" + Application.ACTION_TYPE_MAIL + "\" instead of \"{}\"",
          typeAsJsonPrimitive.getAsString());
      return value;
    }
    JsonArray toAsJsonArray = jsonObject.getAsJsonArray("to");
    if (toAsJsonArray == null || toAsJsonArray.size() == 0) {
      LOGGER.error("The filed \"to\" of action should not be empty");
      return value;
    }

    JsonPrimitive timestampAsJsonPrimitive = jsonObject.getAsJsonPrimitive("timestamp");
    long currentTimeMillis = System.currentTimeMillis();
    if (timestampAsJsonPrimitive != null) {
      try {
        currentTimeMillis = timestampAsJsonPrimitive.getAsLong();
      } catch (Exception e) {
        //
      }
    }
    JsonPrimitive tenantIdAsJsonPrimitive = jsonObject.getAsJsonPrimitive("tenantId");
    JsonPrimitive deviceTypeAsJsonPrimitive = jsonObject.getAsJsonPrimitive("deviceType");
    JsonPrimitive deviceIdAsJsonPrimitive = jsonObject.getAsJsonPrimitive("deviceId");
    JsonArray eventAsJsonArray = jsonObject.getAsJsonArray("event");
    JsonObject ruleAsJsonObject = jsonObject.getAsJsonObject("rule");
    JsonPrimitive ruleIdAsJsonPrimitive = null;
    JsonPrimitive ruleNameAsJsonPrimitive = null;
    JsonPrimitive ruleDescriptionAsJsonPrimitive = null;
    JsonPrimitive ruleConditionAsJsonPrimitive = null;
    if (ruleAsJsonObject != null) {
      ruleIdAsJsonPrimitive = ruleAsJsonObject.getAsJsonPrimitive("id");
      ruleNameAsJsonPrimitive = ruleAsJsonObject.getAsJsonPrimitive("name");
      ruleDescriptionAsJsonPrimitive = ruleAsJsonObject.getAsJsonPrimitive("description");
      ruleConditionAsJsonPrimitive = ruleAsJsonObject.getAsJsonPrimitive("condition");
    }

    JsonPrimitive includeRawDataAsJsonPrimitive = jsonObject.getAsJsonPrimitive("includeRawData");
    boolean includeRawData = (includeRawDataAsJsonPrimitive == null) ? false
        : includeRawDataAsJsonPrimitive.getAsBoolean();

    String ruleName = (ruleNameAsJsonPrimitive == null) ? "" : ruleNameAsJsonPrimitive.getAsString();
    String ruleCondition = (ruleConditionAsJsonPrimitive == null) ? "": ruleConditionAsJsonPrimitive.getAsString();
    String ruleDescription = (ruleDescriptionAsJsonPrimitive == null) ? "":ruleDescriptionAsJsonPrimitive.getAsString();
    String events = (eventAsJsonArray == null) ? "" : eventAsJsonArray.toString();
    String body = getBody(ruleName, ruleDescription, ruleCondition, currentTimeMillis, includeRawData, events);

    LOGGER.info("to: {}", toAsJsonArray.toString());
    JsonArray ccAsJsonArray = jsonObject.getAsJsonArray("cc");
    if (ccAsJsonArray != null && ccAsJsonArray.size() > 0) {
      LOGGER.info("cc: {}", ccAsJsonArray.toString());
    }
    JsonPrimitive titleAsJsonPrimitive = jsonObject.getAsJsonPrimitive("title");
    if(titleAsJsonPrimitive != null) {
      LOGGER.info("title: {}", titleAsJsonPrimitive.toString());
    }
    LOGGER.info("body: {}", body);

    return null;
  }

  private String getBody(String ruleName, String ruleDescription, String ruleCondition, long currentTimeMillis,
      boolean includeRawData, String events) {
    StringBuffer body = new StringBuffer();
    body.append("<p><b>Rule Name</b>: ").append(ruleName).append("</p>");
    body.append("<p><b>Rule Description</b>: ").append(ruleDescription).append("</p>");
    body.append("<p><b>Rule Condition</b>: ").append(ruleCondition).append("</p>");
    body.append("<p><b>Date Time</b>: ").append(currentTimeMillis).append("</p>");
    if (includeRawData) {
      body.append("<p><b>Incoming Event(s)</b>: ").append(events).append("</p>");
    }
    return body.toString();
  }
}
