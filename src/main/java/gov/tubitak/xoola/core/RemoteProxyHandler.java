/*
 * Copyright 2021-TUBITAK BILGEM
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gov.tubitak.xoola.core;

import gov.tubitak.xoola.exception.XCommunicationException;
import gov.tubitak.xoola.transport.Invocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;

/**
 * This class only holds a name per remote object proxy. The rest is handled by
 * RemoteInvocationEndpoint.
 *
 * @author yerlibilgin
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
   * @throws XCommunicationException if an exception occurs during communication
   */
  @Override
  public Object invoke(Object proxy, final Method m, final Object[] args) {
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Invoke {}, async: {}, method name: {}", remoteName, async, m.getName());
    }

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
        return handler.invokeRemote(remoteName, invocation);
      } catch (Exception e) {
        throw new XCommunicationException(e);
      }
    }
  }
}
