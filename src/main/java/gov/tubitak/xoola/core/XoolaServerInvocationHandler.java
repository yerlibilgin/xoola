package gov.tubitak.xoola.core;

import gov.tubitak.xoola.tcpcom.connmanager.server.NettyServer;
import gov.tubitak.xoola.transport.Invocation;
import java.util.Properties;
import org.slf4j.Logger;
import gov.tubitak.xoola.exception.XIOException;
import gov.tubitak.xoola.tcpcom.connmanager.server.NettyServer;
import gov.tubitak.xoola.transport.Invocation;
import org.slf4j.LoggerFactory;

public class XoolaServerInvocationHandler extends XoolaInvocationHandler {
 private static final Logger LOGGER = LoggerFactory.getLogger(XoolaServerInvocationHandler.class);
 private NettyServer nettyServer;
 private String id;

 public XoolaServerInvocationHandler(Properties properties) {
  super(properties);
  this.nettyServer = new NettyServer(properties, this);
  this.id = properties.get(XoolaProperty.SERVERID).toString();
 }

 @Override
 protected void sendMessage(String remoteName, Invocation message) {
  this.nettyServer.send(remoteName, message);
 }

 @Override
 public < T > T get(Class < T > interfaze, String remoteName, String remoteObjectName, boolean async) {
  LOGGER.info("Get remote interface for client: " + remoteName + " Object: " + remoteObjectName);
  if (remoteObjectName == null && !nettyServer.getServerRegistry().hasUser(remoteObjectName))
   throw new XIOException("Remote client not connected");

  return createProxyForClass(interfaze, remoteName, remoteObjectName, async);
 }

 @Override
 public void stop() {
  try {
   this.nettyServer.stop();
  } catch (Exception ex) {
  }

  nettyServer.getServerRegistry().clear();
 }

 @Override
 public String getId() {
  return id;
 }

 @Override
 public void start() {
  this.nettyServer.start();
 }
}
