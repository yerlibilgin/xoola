package org.interop.xoola.java.test;

import org.apache.log4j.PropertyConfigurator;
import org.interop.xoola.core.*;

import java.io.FileReader;
import java.util.Properties;

/**
 * @author: Bulut
 * @date: 07/03/17.
 */
public class SimpleClient {
  public static void main(String[] args) throws Exception {
    PropertyConfigurator.configure("logging.properties");
    Properties xoolaProperties = new Properties();
    xoolaProperties.load(new FileReader("xoola.properties"));

    xoolaProperties.put(XoolaProperty.MODE, XoolaTierMode.CLIENT);
    final Xoola client = Xoola.init(xoolaProperties);
    client.addConnectionListener(new XoolaConnectionListener() {
      @Override
      public void connected(XoolaInvocationHandler xoolaInvocationHandler, XoolaChannelState xcs) {
        System.out.println("Client connected");


        Runnable runnable = client.get(Runnable.class, "MYOBJECT");
        System.out.println("Connection Succeeded");

        runnable.run();
      }

      @Override
      public void disconnected(XoolaInvocationHandler xoolaInvocationHandler, XoolaChannelState xcs) {
        System.out.println("Client disconnected");
      }
    });

    client.start();
    Thread.sleep(10000000L);
  }
}
