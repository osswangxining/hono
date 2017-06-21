package org.eclipse.hono.mqttserver;

public class MQTTProtocolTest {

  public static void main(String[] args) {
    byte publishFixHeader = 50;// 0 0 1 1 0 0 1 0

    doGetBit(publishFixHeader);
    int ori = 224;// 1110000,DISCONNECT ,Message Type (14)
    byte flag = (byte) ori; // 有符号byte
    System.out.println((byte)flag);
    doGetBit(flag);
    doGetBit_v2(ori);
  }

  public static void doGetBit(byte flags) {
    boolean retain = (flags & 1) > 0;
    int qosLevel = (flags & 0x06) >> 1;
    boolean dupFlag = (flags & 8) > 0;
    int messageType = (flags >> 4) & 0x0f;

    System.out.format("Message type:%d, DUP flag:%s, QoS level:%d, RETAIN:%s\n", messageType, dupFlag, qosLevel,
        retain);
  }

  public static void doGetBit_v2(int flags) {
    boolean retain = (flags & 1) > 0;
    int qosLevel = (flags & 0x06) >> 1;
    boolean dupFlag = (flags & 8) > 0;
    int messageType = flags >> 4;

    System.out.format("Message type:%d, DUP flag:%s, QoS level:%d, RETAIN:%s\n", messageType, dupFlag, qosLevel,
        retain);
  }
}
