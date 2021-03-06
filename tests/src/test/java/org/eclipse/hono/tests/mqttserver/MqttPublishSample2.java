package org.eclipse.hono.tests.mqttserver;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;


public class MqttPublishSample2 {
  public static void main(String[] args) {
    String clientId = "4716";

    String topic = "event/DEFAULT_TENANT/" + clientId;
    String content = "Message from MqttPublishSample";
    int qos = 1;
    String broker = "tcp://127.0.0.1:1883";
    MemoryPersistence persistence = new MemoryPersistence();
    

    try {

      MqttClient sampleClient = new MqttClient(broker, clientId, persistence);
      MqttConnectOptions connOpts = new MqttConnectOptions();
      connOpts.setCleanSession(true);
      connOpts.setWill("/LWT", "Good Bye!".getBytes(), 2, true);
      System.out.println("Connecting to broker: " + broker);
      sampleClient.connect(connOpts);
      System.out.println("Connected");

      for (int i = 10; i < 20; i++) {
        System.out.println("Publishing message: " + content);
        MqttMessage message = new MqttMessage(("[" + i + "]" + content).getBytes());
        message.setQos(qos);
        sampleClient.publish(topic, message);
      }
      System.out.println("Message published");
      sampleClient.disconnect();
      System.out.println("Disconnected");
      System.exit(0);
    } catch (MqttException me) {
      System.out.println("reason " + me.getReasonCode());
      System.out.println("msg " + me.getMessage());
      System.out.println("loc " + me.getLocalizedMessage());
      System.out.println("cause " + me.getCause());
      System.out.println("excep " + me);
      me.printStackTrace();
    }
  }
}
