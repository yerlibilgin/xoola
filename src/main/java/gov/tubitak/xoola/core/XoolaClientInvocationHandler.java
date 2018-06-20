package gov.tubitak.xoola.core;

import gov.tubitak.xoola.transport.Invocation;
import java.util.Properties;
import gov.tubitak.xoola.exception.XCommunicationException;
import gov.tubitak.xoola.tcpcom.connmanager.client.NettyClient;
import gov.tubitak.xoola.transport.Invocation;
import gov.tubitak.xoola.util.ObjectUtils;

public class XoolaClientInvocationHandler extends XoolaInvocationHandler {

  private NettyClient nettyClient;
  private String id;

  public XoolaClientInvocationHandler(Properties properties) {
    super(properties);
    this.nettyClient = new NettyClient(properties, this);
    this.id = ObjectUtils.getOrDefault(properties.get(XoolaProperty.CLIENTID), "xoolaClient");
  }

  @Override
  protected void sendMessage(String remoteName, Invocation message) {
    nettyClient.send(remoteName, message);
  }

  @Override
  public <T> T get(Class<T> interfaze, String remoteName, String remoteObjectName, boolean async) {
    if (nettyClient.isAvailable()) {
      return createProxyForClass(interfaze, remoteName, remoteObjectName, async);
    }

    throw new XCommunicationException("Not connected to any servers");
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void stop() {
    nettyClient.stop();
  }

  @Override
  public void start() {
   nettyClient.start();
  }
}
