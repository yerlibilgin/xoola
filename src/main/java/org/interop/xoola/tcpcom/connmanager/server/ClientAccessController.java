package org.interop.xoola.tcpcom.connmanager.server;

public interface ClientAccessController {
 /**
  * is the provided client id allowed to create connection to the system?
  * @return
  */
 public boolean clientIsAllowed(String id);
}
