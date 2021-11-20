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
import gov.tubitak.xoola.exception.XInvocationException;
import gov.tubitak.xoola.transport.Invocation;
import gov.tubitak.xoola.transport.Response;
import gov.tubitak.xoola.util.ObjectUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Observable;
import java.util.Observer;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The base invocation handler for a client request
 *
 * @author yerlibilgin
 */
public abstract class XoolaInvocationHandler extends Observable implements CancellableInvocation {

  private static final Logger LOGGER = LoggerFactory.getLogger(XoolaInvocationHandler.class);

  /**
   * The type Observer wrapper.
   */
  public class ObserverWrapper implements Observer {

    private final XoolaConnectionListener connectionStateListener;

    /**
     * Instantiates a new Observer wrapper.
     *
     * @param connectionStateListener the connection state listener
     */
    public ObserverWrapper(XoolaConnectionListener connectionStateListener) {
      this.connectionStateListener = connectionStateListener;
    }

    @Override
    public void update(Observable o, Object arg) {
      XoolaChannelState xcs = (XoolaChannelState) arg;
      if (LOGGER.isDebugEnabled())
        LOGGER.debug("update connection observer {}", Boolean.TRUE.equals(xcs.connected));
      if (xcs.connected) {
        this.connectionStateListener.connected(XoolaInvocationHandler.this, xcs);
      } else {
        this.connectionStateListener.disconnected(XoolaInvocationHandler.this, xcs);
      }
    }
  }

  /**
   * The Names map.
   */
  final HashMap<String, Object> NAMES_MAP = new HashMap<String, Object>();
  private AtomicInteger invocationCounter;

  private Response receipt;
  private Properties properties;
  private Object mutex;

  /**
   * Instantiates a new Xoola invocation handler.
   *
   * @param properties the properties
   */
  public XoolaInvocationHandler(Properties properties) {
    this.properties = properties;
    this.mutex = new Object();
  }

  /**
   * Invokes the method of the given remote object with args params.
   *
   * @param remoteClientName the remote client name
   * @param message          the message
   * @return the result of the remote operation
   * @throws XCommunicationException  if the invocation doesn't finish in the given time
   * @throws XInvocationException     if a remote error occurs.
   * @throws IllegalArgumentException if a null result comes.
   */
  public synchronized Object invokeRemote(String remoteClientName, Invocation message) {
    synchronized (this.mutex) {
      receipt = null; //reset it in any case.
      sendMessage(remoteClientName, message);
      long timeout = ObjectUtils
          .getOrDefault(this.properties.get(XoolaProperty.NETWORK_RESPONSE_TIMEOUT), XoolaPropertyDefaults.NETWORK_RESPONSE_TIMEOUT);
      try {
        this.mutex.wait(timeout);
      } catch (InterruptedException e) {
        //someone interrupted me.
        //consume receipt just in case
        consumeReceipt();
        throw new XCommunicationException(e);
      }
    }
    Response result = this.consumeReceipt();

    if (result != null) {
      if (result.returnValue instanceof Throwable) {
        throw new XCommunicationException((Throwable) result.returnValue);
      } else {
        return result.returnValue;
      }
    }

    throw new XCommunicationException("Couldn't get a result in the given time");
  }

  @Override
  public void cancel() {
    try {
      synchronized (this.mutex) {
        this.mutex.notify();
      }
    } catch (Exception ex) {
    }
  }

  /**
   * Send message.
   *
   * @param remoteName the remote name
   * @param message    the message
   */
  protected abstract void sendMessage(String remoteName, Invocation message);

  /**
   * Receive invocation object.
   *
   * @param invocation the invocation
   * @return the object
   */
  public Object receiveInvocation(Invocation invocation) {
    Response response = new Response();
    if (this.NAMES_MAP.containsKey(invocation.objectName)) {
      // System.out.print(invocation.objectName + "." + invocation.methodName + ".(");
      Object callee = this.NAMES_MAP.get(invocation.objectName);
      Object args[] = invocation.params;

      Class<?> argTypes[] = null;
      if (args == null) {
        argTypes = new Class<?>[0];
      } else {
        // for (Object object : args) {
        // System.out.print(((object == null) ? "null" : object.getClass()) + ",");
        // }
        // LOGGER.debug(")");
        argTypes = new Class<?>[args.length];
        for (int i = 0; i < args.length; i++) {
          if (args[i] == null) {
            argTypes[i] = null;
          } else {
            argTypes[i] = args[i].getClass();
          }
        }
      }
      try {
        Method m = findMethod(callee.getClass(), invocation.methodName, argTypes);
        m.setAccessible(true);
        Object o = m.invoke(callee, args);
        response.returnValue = o;
      } catch (Throwable e) {
        response.returnValue = e;
      }

    } else {
      LOGGER.warn("No registered object named {}", invocation.objectName);
      response.returnValue = new UnsupportedOperationException("No registered object named \"" + invocation.objectName + "\"");
    }
    return response;
  }

  private Method findMethod(Class<? extends Object> class1, String methodName, Class<?>[] argTypes) throws NoSuchMethodException {
    Method[] allMethods = class1.getMethods();
    for (Method method : allMethods) {
      if (method.getName().equals(methodName)) {
        if (matchParameters(method, argTypes)) {
          return method;
        }
      }
    }
    throw new NoSuchMethodException(class1.getName() + "." + methodName);
  }

  private boolean matchParameters(Method method, Class<?>[] argTypes) {
    Class<?>[] types = method.getParameterTypes();
    if (types.length != argTypes.length) {
      return false;
    }

    for (int i = 0; i < types.length; i++) {
      Class<?> type = types[i];
      Class<?> type2 = argTypes[i];
      if (type2 == null) {
        if (type.isPrimitive()) {
          return false;
        }
      } else if (!type.isAssignableFrom(type2) && !comparePrimitives(type, type2)) {
        return false;
      }
    }

    return true;
  }

  private boolean comparePrimitives(Class<?> type, Class<?> type2) {
    return

        checkPrimitive("int", "java.lang.Integer", type, type2) || checkPrimitive("boolean", "java.lang.Boolean", type, type2) ||
            checkPrimitive("double", "java.lang.Double", type, type2) || checkPrimitive("float", "java.lang.Float", type, type2) ||
            checkPrimitive("byte", "java.lang.Byte", type, type2) || checkPrimitive("short", "java.lang.Short", type, type2) ||
            checkPrimitive("char", "java.lang.Character", type, type2);
  }

  private boolean checkPrimitive(String primitive, String wrapper, Class<?> type, Class<?> type2) {
    return type.getName().equals(primitive) && type2.getName().equals(wrapper);
  }

  /**
   * Receive response.
   *
   * @param receipt the receipt
   */
  public void receiveResponse(Response receipt) {
    this.receipt = receipt;
    synchronized (this.mutex) {
      this.mutex.notifyAll();
    }
  }

  /**
   * Add connection listener.
   *
   * @param connectionStateListener the connection state listener
   */
  public void addConnectionListener(XoolaConnectionListener connectionStateListener) {
    this.addObserver(new ObserverWrapper(connectionStateListener));
  }

  /**
   * Consume receipt response.
   *
   * @return the response
   */
  protected synchronized Response consumeReceipt() {
    try {
      return this.receipt;
    } finally {
      this.receipt = null;
    }
  }

  /**
   * Unregister.
   *
   * @param name the name
   */
  public void unregister(String name) {
    this.NAMES_MAP.remove(name);
  }

  /**
   * Registers an object with <code>name</code> as a service. The remote client
   * will use that name to call a method of that object
   *
   * @param name   Name for remote call
   * @param object The object (as a remote service)
   * @throws IllegalArgumentException If an object is already registered with the given name
   */
  public void registerObject(String name, Object object) {
    if (this.NAMES_MAP.containsKey(name)) {
      throw new IllegalArgumentException("An object for key [" + name + "] has already been registered");
    }
    this.NAMES_MAP.put(name, object);
  }

  /**
   * Connected.
   *
   * @param remoteId the remote id
   */
  public void connected(String remoteId) {
    LOGGER.debug("connected(" + remoteId + ")");
    this.setChanged();
    this.notifyObservers(new XoolaChannelState(remoteId, true));
  }

  /**
   * Disconnected.
   *
   * @param remoteId the remote id
   */
  public void disconnected(String remoteId) {
    LOGGER.debug("disconnected(" + remoteId + ")");
    this.setChanged();
    this.notifyObservers(new XoolaChannelState(remoteId, false));
  }

  /**
   * Get t.
   *
   * @param <T>              the type parameter
   * @param interfaze        the interfaze
   * @param remoteObjectName the remote object name
   * @return the t
   */
  public <T> T get(Class<T> interfaze, String remoteObjectName) {
    return get(interfaze, null, remoteObjectName, false);
  }

  /**
   * Get t.
   *
   * @param <T>              the type parameter
   * @param interfaze        the interfaze
   * @param remoteObjectName the remote object name
   * @param async            the async
   * @return the t
   */
  public <T> T get(Class<T> interfaze, String remoteObjectName, boolean async) {
    return get(interfaze, null, remoteObjectName, async);
  }

  /**
   * Get t.
   *
   * @param <T>              the type parameter
   * @param interfaze        the interfaze
   * @param remoteName       the remote name
   * @param remoteObjectName the remote object name
   * @return the t
   */
  public <T> T get(Class<T> interfaze, String remoteName, String remoteObjectName) {
    return get(interfaze, remoteName, remoteObjectName, false);
  }

  /**
   * Get t.
   *
   * @param <T>              the type parameter
   * @param interfaze        the interfaze
   * @param remoteName       the remote name
   * @param remoteObjectName the remote object name
   * @param async            the async
   * @return the t
   */
  public abstract <T> T get(Class<T> interfaze, String remoteName, String remoteObjectName, boolean async);

  /**
   * Create proxy for class t.
   *
   * @param <T>              the type parameter
   * @param interfaze        the interfaze
   * @param remoteName       the remote name
   * @param remoteObjectName the remote object name
   * @param async            the async
   * @return the t
   */
  @SuppressWarnings("unchecked")
  protected <T> T createProxyForClass(Class<T> interfaze, String remoteName, String remoteObjectName, boolean async) {
    if (LOGGER.isDebugEnabled())
      LOGGER.debug("createProxyForClass: {}, remoteName: {}, remoteObjectName:{}, async:{}"
          , interfaze.getName()
          , remoteName
          , remoteObjectName
          , async);
    return (T) Proxy.newProxyInstance(interfaze.getClassLoader(), new Class<?>[]{interfaze}, new RemoteProxyHandler(remoteName,
        remoteObjectName, this, async));
  }

  /**
   * Start.
   */
  public abstract void start();

  /**
   * Stop.
   */
  public abstract void stop();

  /**
   * Gets id.
   *
   * @return the id
   */
  public abstract String getId();
}
