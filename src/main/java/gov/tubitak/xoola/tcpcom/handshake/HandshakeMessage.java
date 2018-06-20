package gov.tubitak.xoola.tcpcom.handshake;

import gov.tubitak.xoola.transport.TransportObject;

public class HandshakeMessage implements TransportObject {
 /**
  *
  */
 private static final long serialVersionUID = -6054120973034178121L;
 public final String message;

 public HandshakeMessage(String message) {
  this.message = message;
 }

}
