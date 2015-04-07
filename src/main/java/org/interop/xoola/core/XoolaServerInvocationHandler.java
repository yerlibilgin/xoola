package org.interop.xoola.core;

import java.util.Properties;

import org.apache.log4j.Logger;
import org.interop.xoola.exception.XIOException;
import org.interop.xoola.tcpcom.connmanager.server.NettyServer;
import org.interop.xoola.transport.Invocation;

public class XoolaServerInvocationHandler extends XoolaInvocationHandler {
 private static final Logger LOGGER = Logger.getLogger(XoolaServerInvocationHandler.class);
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
