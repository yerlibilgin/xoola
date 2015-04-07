/*
 * XoolA is a remote method call bridge between java and dotnet platforms.
 * Copyright (C) 2010 Muhammet YILDIZ, Doğan ERSÖZ
 * 
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package org.interop.xoola.tcpcom.connmanager.server;

import java.util.HashMap;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.group.DefaultChannelGroup;

/**
 * @author muhammet
 * 
 */
public class XoolaIdMappedChannelGroup extends DefaultChannelGroup {
  private HashMap<String, Integer> channelIdMap = new HashMap<String, Integer>();
  private HashMap<Integer, String> clientIdMap = new HashMap<Integer, String>();

  public XoolaIdMappedChannelGroup(String name) {
    super(name);
  }

  public void setChannelForName(String name, Channel channel) {
    this.channelIdMap.put(name, channel.getId());
    this.clientIdMap.put(channel.getId(), name);
  }

  public Channel getChannelFor(String name) {
    if (channelIdMap.containsKey(name))
      return find(channelIdMap.get(name));
    throw new IllegalArgumentException("No channel with name " + name);
  }

  public String getNameForChannel(Channel channel) {
    return this.clientIdMap.get(channel.getId());
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.jboss.netty.channel.group.DefaultChannelGroup#remove(java.lang.Object)
   */
  @Override
  public boolean remove(Object o) {
    try {
      return super.remove(o);
    } finally {
      if (o instanceof Integer) {
        Integer new_name = (Integer) o;
        channelIdMap.remove(clientIdMap.remove(new_name));
      } else if (o instanceof Channel) {
        Channel new_name = (Channel) o;
        channelIdMap.remove(clientIdMap.remove(new_name.getId()));
      }
    }
  }
}
