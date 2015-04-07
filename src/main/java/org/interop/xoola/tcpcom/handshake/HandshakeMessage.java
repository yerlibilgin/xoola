package org.interop.xoola.tcpcom.handshake;

import org.interop.xoola.transport.TransportObject;

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
