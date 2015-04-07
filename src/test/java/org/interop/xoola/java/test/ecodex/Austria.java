package org.interop.xoola.java.test.ecodex;

import java.io.IOException;
import java.util.Properties;
import java.util.Queue;

import javax.swing.JFrame;

import org.interop.xoola.core.Xoola;
import org.interop.xoola.core.XoolaChannelState;
import org.interop.xoola.core.XoolaConnectionListener;
import org.interop.xoola.core.XoolaInvocationHandler;
import org.interop.xoola.core.XoolaProperty;
import org.interop.xoola.core.XoolaTierMode;

public class Austria implements XoolaConnectionListener {
 private Xoola client;

 Queue < MyHighTechnologicalMessage > queue;

 public Austria() throws IOException {
  // create a Xoola client to the main test jvm
  Properties p = new Properties();
  p.load(this.getClass().getResourceAsStream("ecodex.properties"));
  p.put(XoolaProperty.CLIENTID, "Austria");
  p.put(XoolaProperty.MODE, XoolaTierMode.CLIENT); // I am a client
  client = Xoola.init(p);
  client.addConnectionListener(this);
  client.registerObject("myClientObject", new ClientInterface() {
   public String strangeAddMethod(int a, double b) {
    return "Austria " + (a + b);
   }
  });
  client.start();
 }

 @SuppressWarnings("unchecked")
 @Override
 public void connected(XoolaInvocationHandler xoolaInvocationHandler, XoolaChannelState xcs) {
  // get the remote queue
  // the server has to have registered the queue to xoola with the name
  // austriaQueue
  queue = xoolaInvocationHandler.get(Queue.class, "queue");
  // now add something to queue
  queue.offer(new MyHighTechnologicalMessage("I am austria"));
 }

 @Override
 public void disconnected(XoolaInvocationHandler xoolaInvocationHandler, XoolaChannelState xcs) {
 }

 public static void main(String[] args) throws IOException {
  new Austria();
  // close this frame to kill austria side
  JFrame frame = new JFrame("Austria");
  frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
  frame.setLocationByPlatform(true);
  frame.setSize(200, 20);
  frame.setVisible(true);
 }
}
