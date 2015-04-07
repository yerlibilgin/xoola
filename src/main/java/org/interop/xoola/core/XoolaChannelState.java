package org.interop.xoola.core;

public class XoolaChannelState {
  public String remoteId;
  public boolean connected;

  public XoolaChannelState() {
  }

  public XoolaChannelState(String remoteId, boolean connected) {
    this.remoteId = remoteId;
    this.connected = connected;
  }
}
