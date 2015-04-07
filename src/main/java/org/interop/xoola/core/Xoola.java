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
package org.interop.xoola.core;

import java.util.Properties;
import java.util.concurrent.CountDownLatch;

/**
 * 
 * @author dogan, muhammet
 * 
 */
public class Xoola {
 public XoolaInvocationHandler handler;

 /**
  * @param xoolaHandler
  */
 Xoola(XoolaInvocationHandler xoolaHandler) {
  this.handler = xoolaHandler;
 }

 /**
  * This method is used to get a proxy for the given remote object.
  */
 public < T > T get(Class < T > interfaze, String remoteObjectName) {
  return (T) this.handler.get(interfaze, remoteObjectName);
 }

 public < T > T get(Class < T > interfaze, String remoteObjectName, boolean async) {
  return (T) this.handler.get(interfaze, remoteObjectName, async);
 }

 public < T > T get(Class < T > interfaze, String remoteName, String remoteObjectName) {
  return (T) this.handler.get(interfaze, remoteName, remoteObjectName);
 }

 public < T > T get(Class < T > interfaze, String remoteName, String remoteObjectName, boolean async) {
  return (T) this.handler.get(interfaze, remoteName, remoteObjectName, async);
 }

 /**
  * Check {@link XoolaInvocationHandler#unregister(String name)}
  */
 public void unregisterObject(String name) {
  this.handler.unregister(name);
 }

 /**
  * Check
  * {@link XoolaInvocationHandler#registerObject(String name, Object object)}
  */
 public void registerObject(String name, Object object) {
  this.handler.registerObject(name, object);
 }

 /**
  *
  */
 public void close() {
  this.handler.stop();
 }

 /**
  * @param p1
  * @return
  */
 public static Xoola init(Properties properties) {
  Object tierMode = properties.get(XoolaProperty.MODE);
  if (tierMode == null)
   throw new IllegalArgumentException("Xoola properties do not contain MODE");
  if (tierMode.equals(XoolaTierMode.CLIENT))
   return new Xoola(new XoolaClientInvocationHandler(properties));
  if (tierMode.equals(XoolaTierMode.SERVER))
   return new Xoola(new XoolaServerInvocationHandler(properties));
  throw new IllegalArgumentException("Illegal Xoola tier mode " + tierMode);
 }

 /**
  * @param connectionListener
  */
 public void addConnectionListener(XoolaConnectionListener connectionListener) {
  this.handler.addConnectionListener(connectionListener);
 }

 public String getId() {
  return handler.getId();
 }

 public void start() {
  this.handler.start();
 }

 public void stop() {
  this.handler.stop();
 }

 public void waitForConnection() throws InterruptedException {
  final CountDownLatch latch = new CountDownLatch(1);
  addConnectionListener(new XoolaConnectionListener() {
   @Override
   public void disconnected(XoolaInvocationHandler xoolaInvocationHandler, XoolaChannelState xcs) {
   }

   @Override
   public void connected(XoolaInvocationHandler xoolaInvocationHandler, XoolaChannelState xcs) {
    latch.countDown();
   }
  });
  latch.await();
 }
}
