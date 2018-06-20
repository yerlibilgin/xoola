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
import gov.tubitak.xoola.exception.XInvocationException;
import gov.tubitak.xoola.transport.Invocation;
import gov.tubitak.xoola.transport.Response;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Observable;
import java.util.Observer;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import gov.tubitak.xoola.exception.XCommunicationException;
import gov.tubitak.xoola.exception.XInvocationException;
import gov.tubitak.xoola.transport.Invocation;
import gov.tubitak.xoola.transport.Response;
import gov.tubitak.xoola.util.ObjectUtils;

/**
 * @author dogan, muhammet
 */
public abstract class XoolaInvocationHandler extends Observable implements CancellableInvocation {

  private static final Logger LOGGER = LoggerFactory.getLogger(XoolaInvocationHandler.class);
  public class ObserverWrapper implements Observer {

    private final XoolaConnectionListener connectionStateListener;

    public ObserverWrapper(XoolaConnectionListener connectionStateListener) {
      this.connectionStateListener = connectionStateListener;
    }

    @Override
    public void update(Observable o, Object arg) {
      XoolaChannelState xcs = (XoolaChannelState) arg;
      LOGGER.debug("update connection observer (" + Boolean.TRUE.equals(xcs.connected) + ")");
      if (xcs.connected) {
        this.connectionStateListener.connected(XoolaInvocationHandler.this, xcs);
      } else {
        this.connectionStateListener.disconnected(XoolaInvocationHandler.this, xcs);
      }
    }
  }

  final HashMap<String, Object> NAMES_MAP = new HashMap<String, Object>();
  private AtomicInteger invocationCounter;

  private Response receipt;
  private Properties properties;
  private Object mutex;

  /**
   * @param properties
   */
  public XoolaInvocationHandler(Properties properties) {
    this.properties = properties;
    this.mutex = new Object();
  }

  /**
   * Invokes the method of the given remote object with args params.
   *
   * @return the result of the remote operation
   * @throws XCommunicationException
   *     if the invocation doesn't finish in the given time
   * @throws XInvocationException
   *     if a remote error occurs.
   * @throws IllegalArgumentException
   *     if a null result comes.
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

  protected abstract void sendMessage(String remoteName, Invocation message);

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
      LOGGER.warn("No registered object named \"" + invocation.objectName + "\"");
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

  public void receiveResponse(Response receipt) {
    this.receipt = receipt;
    synchronized (this.mutex) {
      this.mutex.notify();
    }
  }

  public void addConnectionListener(XoolaConnectionListener connectionStateListener) {
    this.addObserver(new ObserverWrapper(connectionStateListener));
  }

  protected synchronized Response consumeReceipt() {
    try {
      return this.receipt;
    } finally {
      this.receipt = null;
    }
  }

  /**
   * @param name
   */
  public void unregister(String name) {
    this.NAMES_MAP.remove(name);
  }

  /**
   * Registers an object with <code>name</code> as a service. The remote client
   * will use that name to call a method of that object
   *
   * @param name
   *     Name for remote call
   * @param object
   *     The object (as a remote service)
   * @throws IllegalArgumentException
   *     If an object is already registered with the given name
   */
  public void registerObject(String name, Object object) {
    if (this.NAMES_MAP.containsKey(name)) {
      throw new IllegalArgumentException("An object for key [" + name + "] has already been registered");
    }
    this.NAMES_MAP.put(name, object);
  }

  public void connected(String remoteId) {
    LOGGER.debug("connected(" + remoteId + ")");
    this.setChanged();
    this.notifyObservers(new XoolaChannelState(remoteId, true));
  }

  public void disconnected(String remoteId) {
    LOGGER.debug("disconnected(" + remoteId + ")");
    this.setChanged();
    this.notifyObservers(new XoolaChannelState(remoteId, false));
  }

  public <T> T get(Class<T> interfaze, String remoteObjectName) {
    return get(interfaze, null, remoteObjectName, false);
  }

  public <T> T get(Class<T> interfaze, String remoteObjectName, boolean async) {
    return get(interfaze, null, remoteObjectName, async);
  }

  public <T> T get(Class<T> interfaze, String remoteName, String remoteObjectName) {
    return get(interfaze, remoteName, remoteObjectName, false);
  }

  public abstract <T> T get(Class<T> interfaze, String remoteName, String remoteObjectName, boolean async);

  @SuppressWarnings("unchecked")
  protected <T> T createProxyForClass(Class<T> interfaze, String remoteName, String remoteObjectName, boolean async) {
    return (T) Proxy.newProxyInstance(interfaze.getClassLoader(), new Class<?>[]{interfaze}, new RemoteProxyHandler(remoteName,
        remoteObjectName, this, async));
  }

  public abstract void start();

  public abstract void stop();

  public abstract String getId();
}
