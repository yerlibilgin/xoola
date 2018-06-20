package gov.tubitak.xoola.tcpcom.connmanager.client;

import gov.tubitak.xoola.transport.TransportObject;
import gov.tubitak.xoola.transport.TransportObject;

public class PingPong implements TransportObject {
 private static final long serialVersionUID = 6757028768408150419L;

 public int p;

 public static final int PING = 0;
 public static final int PONG = 1;

 public PingPong() {
 }

 public PingPong(int p) {
  super();
  this.p = p;
 }
}
