package gov.tubitak.xoola.tcpcom.connmanager.server;

public interface ClientAccessController {

  /**
   * is the provided client id allowed to create connection to the system?
   */
  boolean clientIsAllowed(String id);
}
