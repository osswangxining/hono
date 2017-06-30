package org.eclipse.hono.analysis.action;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Pattern;

import javax.net.ssl.SSLContext;
import javax.xml.bind.DatatypeConverter;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.config.SocketConfig.Builder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
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

public class WebHookAction {
  private static final Logger LOGGER = LoggerFactory.getLogger(WebHookAction.class);
  
  private static final String[] HTTP_METHODS = new String[] { "get", "post", "put", "delete", "head", "patch" };
  private static final Pattern PATTERN_PARAMETER_TIMESTAMP = Pattern.compile("[{][{]timestamp}}");
  private static final Pattern PATTERN_PARAMETER_TENANTID = Pattern.compile("[{][{]tenantId}}");
  private static final Pattern PATTERN_PARAMETER_DEVICETYPE = Pattern.compile("[{][{]deviceType}}");
  private static final Pattern PATTERN_PARAMETER_DEVICEID = Pattern.compile("[{][{]deviceId}}");
  private static final Pattern PATTERN_PARAMETER_RULEID = Pattern.compile("[{][{]ruleId}}");
  private static final Pattern PATTERN_PARAMETER_RULENAME = Pattern.compile("[{][{]ruleName}}");
  private static final Pattern PATTERN_PARAMETER_RULEDESCRIPTION = Pattern.compile("[{][{]ruleDescription}}");
  private static final Pattern PATTERN_PARAMETER_RULECONDITION = Pattern.compile("[{][{]ruleCondition}}");
  private static final Pattern PATTERN_PARAMETER_EVENT = Pattern.compile("[{][{]event}}");

  public static final WebHookAction INSTANCE = new WebHookAction();
  //key by device id
  private ConcurrentHashMap<String, List<String>> actions = new ConcurrentHashMap<>();
  private WebHookAction() {

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

    List<String> topics = Arrays.asList(Application.TOPIC_ACTION_WEBHOOK);

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
            if(list == null) {
              list = new ArrayList<String>();
            }
            list.add(value);
            LOGGER.info("topic: {}, key: {}, value: {}", new Object[] { topic, key, value });
            
            actions.put(key, list);
          }
        }
        
        handle(actions);
        long consumedTime = System.currentTimeMillis() - begin;
        LOGGER.info("consumed time(ms): {}", new Object[]{consumedTime});
        consumer.commitSync();
      }
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      consumer.close();
    }
  }

  private void handle(ConcurrentHashMap<String, List<String>> actions) {
    if(actions == null) 
      return;
    
    if(actions.size() < 10) {
      Iterator<Entry<String, List<String>>> iterator = actions.entrySet().iterator();
      while(iterator.hasNext()) {
        Entry<String, List<String>> entry = iterator.next();
        List<String> values = entry.getValue();
        List<String> failedList = handle(values);
        if(failedList != null && !failedList.isEmpty()) {
          LOGGER.info("failed: {}", failedList.toString());
        }
      }
    } else {
      ExecutorService executorService = Executors.newFixedThreadPool(10);
      Iterator<Entry<String, List<String>>> iterator = actions.entrySet().iterator();
      while(iterator.hasNext()) {
        Entry<String, List<String>> entry = iterator.next();
        List<String> values = entry.getValue();
        WebHookProcessor processor = new WebHookProcessor(values);
        Future<List<String>> future = executorService.submit(processor);
        try {
          List<String> failedList = future.get();
          if(failedList != null && !failedList.isEmpty()) {
            LOGGER.info("failed: {}", failedList.toString());
          }
        } catch (InterruptedException | ExecutionException e) {
          e.printStackTrace();
        }
      }
      
      executorService.shutdown();
    }
  }
  
 class WebHookProcessor implements Callable<List<String>> {
   private List<String> values;
    public WebHookProcessor(List<String> values) {
      this.values = values;
    }
    
    @Override
    public List<String> call() throws Exception {
      return handle(values);
    }
    
  }
  
  private List<String> handle(List<String> values) {
    if(values == null)
      return null;
    List<String> failed = new ArrayList<>();
    for (String value : values) {
      String handle = handle(value);
      if(handle != null) {
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
    if (!typeAsJsonPrimitive.getAsString().equalsIgnoreCase(Application.ACTION_TYPE_WEBHOOK)) {
      LOGGER.error("The type of action should be \"webhook\" instead of \"{}\"", typeAsJsonPrimitive.getAsString());
      return value;
    }
    JsonObject operationAsJsonObject = jsonObject.getAsJsonObject("operation");
    if (operationAsJsonObject == null) {
      LOGGER.error("The operation of action should not be empty");
      return value;
    }
    JsonPrimitive urlAsJsonPrimitive = operationAsJsonObject.getAsJsonPrimitive("url");
    if (urlAsJsonPrimitive == null) {
      LOGGER.error("The url of action operation should not be empty");
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

    String url = urlAsJsonPrimitive.getAsString();

    url = PATTERN_PARAMETER_TIMESTAMP.matcher(url).replaceAll("" + currentTimeMillis);
    if (tenantIdAsJsonPrimitive != null) {
      url = PATTERN_PARAMETER_TENANTID.matcher(url).replaceAll(tenantIdAsJsonPrimitive.getAsString());
    }
    if (deviceTypeAsJsonPrimitive != null) {
      url = PATTERN_PARAMETER_DEVICETYPE.matcher(url).replaceAll(deviceTypeAsJsonPrimitive.getAsString());
    }
    if (deviceIdAsJsonPrimitive != null) {
      url = PATTERN_PARAMETER_DEVICEID.matcher(url).replaceAll(deviceIdAsJsonPrimitive.getAsString());
    }
    if (ruleIdAsJsonPrimitive != null) {
      url = PATTERN_PARAMETER_RULEID.matcher(url).replaceAll(ruleIdAsJsonPrimitive.getAsString());
    }
    if (ruleNameAsJsonPrimitive != null) {
      url = PATTERN_PARAMETER_RULENAME.matcher(url).replaceAll(ruleNameAsJsonPrimitive.getAsString());
    }

    try {
      URI.create(url);
    } catch (Exception e) {
      LOGGER.error("The url({}) of action operation is not valid: {}", new Object[] { url, e.getMessage() });
      return value;
    }
    JsonPrimitive methodAsJsonPrimitive = operationAsJsonObject.getAsJsonPrimitive("method");
    if (methodAsJsonPrimitive == null) {
      LOGGER.error("The method of action operation should not be empty");
      return value;
    }
    String lowerCaseMethodName = methodAsJsonPrimitive.getAsString().toLowerCase();
    if (!(Arrays.asList(HTTP_METHODS).contains(lowerCaseMethodName))) {
      LOGGER.error("The method({}) of action operation should be one of the following: {}",
          new Object[] { methodAsJsonPrimitive.getAsString(), Arrays.asList(HTTP_METHODS) });
      return value;
    }
    JsonPrimitive usernameAsJsonPrimitive = operationAsJsonObject.getAsJsonPrimitive("username");
    String username = usernameAsJsonPrimitive == null ? null : usernameAsJsonPrimitive.getAsString();
    JsonPrimitive passwordAsJsonPrimitive = operationAsJsonObject.getAsJsonPrimitive("password");
    String password = passwordAsJsonPrimitive == null ? null : passwordAsJsonPrimitive.getAsString();
    JsonPrimitive contentTypeAsJsonPrimitive = operationAsJsonObject.getAsJsonPrimitive("contentType");
    String contentType = contentTypeAsJsonPrimitive == null ? null : contentTypeAsJsonPrimitive.getAsString();
    JsonArray headersAsJsonArray = operationAsJsonObject.getAsJsonArray("headers");
    Map<String, String> headers = new HashMap<String, String>();
    if (headersAsJsonArray != null) {
      for (int i = 0; i < headersAsJsonArray.size(); i++) {
        JsonElement jsonElement2 = headersAsJsonArray.get(i);
        if (jsonElement2 != null && jsonElement2 instanceof JsonObject) {
          JsonObject kv = (JsonObject) jsonElement2;
          JsonPrimitive keyAsJsonPrimitive = kv.getAsJsonPrimitive("key");
          JsonPrimitive valueAsJsonPrimitive = kv.getAsJsonPrimitive("value");
          if (keyAsJsonPrimitive != null && valueAsJsonPrimitive != null) {
            headers.put(keyAsJsonPrimitive.getAsString(), valueAsJsonPrimitive.getAsString());
          }
        }
      }
    }
    JsonPrimitive bodyAsJsonPrimitive = operationAsJsonObject.getAsJsonPrimitive("body");
    String body = bodyAsJsonPrimitive == null ? "" : bodyAsJsonPrimitive.getAsString();
    body = PATTERN_PARAMETER_TIMESTAMP.matcher(body).replaceAll("" + currentTimeMillis);
    if (tenantIdAsJsonPrimitive != null) {
      body = PATTERN_PARAMETER_TENANTID.matcher(body).replaceAll(tenantIdAsJsonPrimitive.getAsString());
    }
    if (deviceTypeAsJsonPrimitive != null) {
      body = PATTERN_PARAMETER_DEVICETYPE.matcher(body).replaceAll(deviceTypeAsJsonPrimitive.getAsString());
    }
    if (deviceIdAsJsonPrimitive != null) {
      body = PATTERN_PARAMETER_DEVICEID.matcher(body).replaceAll(deviceIdAsJsonPrimitive.getAsString());
    }
    if (ruleIdAsJsonPrimitive != null) {
      body = PATTERN_PARAMETER_RULEID.matcher(body).replaceAll(ruleIdAsJsonPrimitive.getAsString());
    }
    if (ruleNameAsJsonPrimitive != null) {
      body = PATTERN_PARAMETER_RULENAME.matcher(body).replaceAll(ruleNameAsJsonPrimitive.getAsString());
    }
    if (eventAsJsonArray != null) {
      body = PATTERN_PARAMETER_EVENT.matcher(body).replaceAll(eventAsJsonArray.toString());
    }
    if (ruleConditionAsJsonPrimitive != null) {
      body = PATTERN_PARAMETER_RULECONDITION.matcher(body).replaceAll(ruleConditionAsJsonPrimitive.getAsString());
    }
    if (ruleDescriptionAsJsonPrimitive != null) {
      body = PATTERN_PARAMETER_RULEDESCRIPTION.matcher(body).replaceAll(ruleDescriptionAsJsonPrimitive.getAsString());
    }

    JsonPrimitive retryAsJsonPrimitive = operationAsJsonObject.getAsJsonPrimitive("retry");
    int retryCount = 0;
    if (retryAsJsonPrimitive != null) {
      try {
        retryCount = retryAsJsonPrimitive.getAsInt();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    switch (lowerCaseMethodName) {
    case "get":
      get(url, username, password, headers, contentType, retryCount);
      break;
    case "post":
      post(url, username, password, headers, contentType, body, retryCount);
      break;
    case "put":
      put(url, username, password, headers, contentType, body, retryCount);
      break;
    case "delete":
      delete(url, username, password, headers, contentType, retryCount);
      break;
    case "head":
      head(url, username, password, headers, contentType, retryCount);
      break;
    case "patch":
      patch(url, username, password, headers, contentType, body, retryCount);
      break;
    default:
      break;
    }
    
    return null;
  }

  /**
   * Create HttpClient with SSL
   */
  private static CloseableHttpClient createHttpsClient(int retryCount) {

    Registry<ConnectionSocketFactory> sslSocketFactoryRegistry = createAcceptAllSSLSocketFactoryRegistry();
    PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager(sslSocketFactoryRegistry);
    cm.setMaxTotal(200);
    cm.setDefaultMaxPerRoute(1);
    Builder scBuilder = SocketConfig.copy(SocketConfig.DEFAULT);
    // scBuilder.setSoTimeout(10000);
    cm.setDefaultSocketConfig(scBuilder.build());
    DefaultHttpRequestRetryHandler retryHandler = new DefaultHttpRequestRetryHandler(retryCount, false);

    return HttpClients.custom().setRetryHandler(retryHandler).setConnectionManager(cm).build();
  }

  private static Registry<ConnectionSocketFactory> createAcceptAllSSLSocketFactoryRegistry() {
    SSLConnectionSocketFactory sslConnSockFactory = createAcceptAllSSLSocketFactory();

    // Create the registry
    return RegistryBuilder.<ConnectionSocketFactory>create().register("http", new PlainConnectionSocketFactory())
        .register("https", sslConnSockFactory).build();
  }

  private static SSLConnectionSocketFactory createAcceptAllSSLSocketFactory() {
    SSLConnectionSocketFactory sslConnSockFactory = null;

    try {
      // Create a trust strategy that accepts all certificates
      SSLContext sslContext = createAcceptsAllCertsSSLContext();

      // Create a host name verifier that accepts all host names

      // Create the SSL connections socket factory
      sslConnSockFactory = new SSLConnectionSocketFactory(sslContext, new String[] { "TLSv1.2" }, null,
          NoopHostnameVerifier.INSTANCE);
    } catch (Exception e) {
      // Do nothing
    }

    return sslConnSockFactory;
  }

  private static SSLContext createAcceptsAllCertsSSLContext()
      throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException {
    return (new SSLContextBuilder().loadTrustMaterial(null, new TrustStrategy() {
      public boolean isTrusted(X509Certificate[] certificate, String authType) throws CertificateException {
        return true;
      }
    }).useProtocol("TLS").build());
  }

  private static void handleRequestBase(HttpRequestBase requestBase, String url, String username, String password,
      Map<String, String> headers, String contentType, int retryCount) {
    if (headers != null) {
      Set<Entry<String, String>> entrySet = headers.entrySet();
      Iterator<Entry<String, String>> iterator = entrySet.iterator();
      while (iterator.hasNext()) {
        Entry<String, String> entry = iterator.next();
        String key = entry.getKey();
        String value = entry.getValue();
        requestBase.setHeader(key, value);
      }
    }
    if (StringUtils.isNotBlank(username) && StringUtils.isNotBlank(password)) {
      try {
        String authEncodedString = DatatypeConverter.printBase64Binary((username + ":" + password).getBytes("UTF-8"));
        requestBase.addHeader("Authorization", "Basic " + authEncodedString);
      } catch (UnsupportedEncodingException e1) {
        e1.printStackTrace();
      }
    }
    requestBase.setHeader("Connection", "close");
    requestBase.addHeader("Accept", contentType);
    requestBase.addHeader("Content-Type", contentType);

    CloseableHttpClient httpclient = (CloseableHttpClient) createHttpsClient(retryCount);
    RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(5000).setConnectionRequestTimeout(5000)
        .setSocketTimeout(10000).setRedirectsEnabled(true).build();
    requestBase.setConfig(requestConfig);

    try {

      CloseableHttpResponse httpResponse = httpclient.execute(requestBase);

      String strResult = "";
      int statusCode = -1;
      if (httpResponse != null) {
        statusCode = httpResponse.getStatusLine().getStatusCode();
        strResult = EntityUtils.toString(httpResponse.getEntity());
      }
      LOGGER.info("statusCode: [{}], response: [{}]", new Object[] { statusCode, strResult });
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      try {
        if (httpclient != null) {
          httpclient.close();
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  public static void get(String url, String username, String password, Map<String, String> headers, String contentType,
      int retryCount) {
    HttpGet get = new HttpGet(url);
    handleRequestBase(get, url, username, password, headers, contentType, retryCount);
  }

  public static void post(String url, String username, String password, Map<String, String> headers, String contentType,
      String body, int retryCount) {
    HttpPost post = new HttpPost(url);

    StringEntity inputEntity = new StringEntity(body, "UTF-8");
    post.setEntity(inputEntity);

    handleRequestBase(post, url, username, password, headers, contentType, retryCount);
  }

  public static void put(String url, String username, String password, Map<String, String> headers, String contentType,
      String body, int retryCount) {
    HttpPut put = new HttpPut(url);

    StringEntity inputEntity = new StringEntity(body, "UTF-8");
    put.setEntity(inputEntity);

    handleRequestBase(put, url, username, password, headers, contentType, retryCount);
  }

  public static void delete(String url, String username, String password, Map<String, String> headers,
      String contentType, int retryCount) {
    HttpDelete delete = new HttpDelete(url);
    handleRequestBase(delete, url, username, password, headers, contentType, retryCount);
  }

  public static void head(String url, String username, String password, Map<String, String> headers, String contentType,
      int retryCount) {
    HttpHead head = new HttpHead(url);
    handleRequestBase(head, url, username, password, headers, contentType, retryCount);
  }

  public static void patch(String url, String username, String password, Map<String, String> headers,
      String contentType, String body, int retryCount) {
    HttpPatch patch = new HttpPatch(url);

    StringEntity inputEntity = new StringEntity(body, "UTF-8");
    patch.setEntity(inputEntity);

    handleRequestBase(patch, url, username, password, headers, contentType, retryCount);
  }

}
