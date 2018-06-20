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
package gov.tubitak.xoola.core;

import gov.tubitak.xoola.exception.XCommunicationException;
import gov.tubitak.xoola.transport.Invocation;
import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import org.slf4j.Logger;
import gov.tubitak.xoola.exception.XCommunicationException;
import gov.tubitak.xoola.transport.Invocation;
import org.slf4j.LoggerFactory;

/**
 * This class only holds a name per remote object proxy. The rest is handled by
 * RemoteInvocationEndpoint.
 *
 * @author dogan, muhammet
 *
 */
public class RemoteProxyHandler implements java.lang.reflect.InvocationHandler {
 private static final Logger LOGGER = LoggerFactory.getLogger(RemoteProxyHandler.class);
 private final String remoteObjectName;
 private final XoolaInvocationHandler handler;
 private boolean async;
 private ExecutorService threadPool;
 private String remoteName;

 public RemoteProxyHandler(String remoteName, String remoteObjectName, XoolaInvocationHandler handler, boolean async) {
  this.remoteName = remoteName;
  this.remoteObjectName = remoteObjectName;
  this.handler = handler;
  this.async = async;
  threadPool = java.util.concurrent.Executors.newCachedThreadPool();
 }

 /**
  * @throws XCommunicationException
  */
 @Override
 public Object invoke(Object proxy, final Method m, final Object[] args) {
  LOGGER.debug("invoke " + remoteName);
  if (async) {
   Runnable r = new Runnable() {
    @Override
    public void run() {
     if (!m.getName().equals("toString")) {
      try {
       Invocation invocation = Invocation.createMethodCall(remoteObjectName, m.getName(), args);
       handler.invokeRemote(remoteName, invocation);
      } catch (Exception e) {
       LOGGER.error("Error ocurred during asynchronous call " + m + " [" + e.getMessage() + "]", e);
      }
     }
    }
   };

   threadPool.execute(r);
   return null;
  } else {
   if (m.getName().equals("toString")) {
    return "Remote" + proxy.getClass().getSimpleName() + "[" + remoteObjectName + "]";
   }
   try {
    Invocation invocation = Invocation.createMethodCall(remoteObjectName, m.getName(), args);
    Object remote = handler.invokeRemote(remoteName, invocation);
    return remote;
   } catch (Exception e) {
    throw new XCommunicationException(e);
   }
  }
 }
}
