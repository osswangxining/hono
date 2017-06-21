package org.eclipse.hono.mqttserver;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class MQTTProtocolTest2 {

  public static void main(String[] args) throws IOException {
    ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream();
    DataOutputStream dataOutputStream = new DataOutputStream(arrayOutputStream);
    dataOutputStream.write(0xff);
    dataOutputStream.write(0xff);
    dataOutputStream.write(0xff);
    dataOutputStream.write(0x7f);

    InputStream arrayInputStream = new ByteArrayInputStream(arrayOutputStream.toByteArray());

    // 模拟服务器/客户端解析
    System.out.println("result is " + bytes2Length(arrayInputStream));
  }

  /**
   * 转化字节为 int类型长度
   * 
   * @param in
   * @return
   * @throws IOException
   */
  private static int bytes2Length(InputStream in) throws IOException {
    int multiplier = 1;
    int length = 0;
    int digit = 0;
    do {
      digit = in.read(); // 一个字节的有符号或者无符号，转换转换为四个字节有符号 int类型
      length += (digit & 0x7f) * multiplier;
      multiplier *= 128;
    } while ((digit & 0x80) != 0);

    return length;
  }
}
