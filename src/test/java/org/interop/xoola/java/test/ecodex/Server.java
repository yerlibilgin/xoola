package org.interop.xoola.java.test.ecodex;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.LinkedBlockingQueue;

import javax.swing.JFrame;

import org.apache.log4j.PropertyConfigurator;
import org.interop.xoola.core.Xoola;
import org.interop.xoola.core.XoolaChannelState;
import org.interop.xoola.core.XoolaConnectionListener;
import org.interop.xoola.core.XoolaInvocationHandler;
import org.interop.xoola.core.XoolaProperty;
import org.interop.xoola.core.XoolaTierMode;

public class Server {
  private Xoola server;
  private LinkedBlockingQueue<MyHighTechnologicalMessage> blockingQueue;

  public Server() throws InterruptedException, IOException {
    Properties p = new Properties();
    p.load(this.getClass().getResourceAsStream("ecodex.properties"));
    p.put(XoolaProperty.MODE, XoolaTierMode.SERVER); // I am a server
    server = Xoola.init(p);
    server.addConnectionListener(new XoolaConnectionListener() {
     @Override
     public void disconnected(XoolaInvocationHandler xoolaInvocationHandler, XoolaChannelState xcs) {
      System.out.println(xcs.remoteId + " disconnected");
     }
     
     @Override
     public void connected(XoolaInvocationHandler xoolaInvocationHandler, XoolaChannelState xcs) {
      System.out.println(xcs.remoteId + " connected");
      ClientInterface ci = server.get(ClientInterface.class, xcs.remoteId, "myClientObject");
      String daral = ci.strangeAddMethod(30, 56.6);
      System.out.println(">>> Strange add method called " + daral);
     }
    });

    // create queues
    blockingQueue = new LinkedBlockingQueue<MyHighTechnologicalMessage>();
    // register the queues to corresponding xoola servers so that other party
    // can
    // call them
    server.registerObject("queue", blockingQueue);

    // create two threads that wait on each queue, take one object and finish
    Thread clientThread = new Thread() {
      public void run() {
        while (true) {
          try {
            MyHighTechnologicalMessage msg = blockingQueue.take();
            System.out.println("Client thread received message" + msg.esasMesaj);
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }
      }
    };
    clientThread.start();
    
    server.start();
  }

  public static void main(String[] args) throws InterruptedException, IOException {
    PropertyConfigurator.configure("logging.properties");
    new Server();
    JFrame frame = new JFrame("TEST JVM");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setLocationByPlatform(true);
    frame.setSize(200, 20);
    frame.setVisible(true);
  }
}
