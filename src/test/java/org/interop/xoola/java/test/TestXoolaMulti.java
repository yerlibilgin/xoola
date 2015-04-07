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
package org.interop.xoola.java.test;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;

import junit.framework.Assert;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.interop.xoola.core.Xoola;
import org.interop.xoola.core.XoolaChannelState;
import org.interop.xoola.core.XoolaConnectionListener;
import org.interop.xoola.core.XoolaInvocationHandler;
import org.interop.xoola.core.XoolaProperty;
import org.interop.xoola.core.XoolaTierMode;
import org.interop.xoola.tcpcom.connmanager.server.ClientAccessController;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

/**
 * Intended for tests.
 * 
 * @author yerlibilgin
 * 
 */
@SuppressWarnings("unused")
public class TestXoolaMulti {

 public static class MultiControl implements ClientAccessController {
  @Override
  public boolean clientIsAllowed(String id) {
   int idi = Integer.parseInt(id);
   System.err.println(idi);
   return 0 <= idi && idi < 100;
  }
 }

 private static final Logger LOGGER = Logger.getLogger(TestXoolaMulti.class);
 private static Xoola server;
 private static ArrayList < Xoola > clients = new ArrayList < Xoola >();

 @Rule
 public TestName name = new TestName();

 @BeforeClass
 public static void setUp() throws Throwable {
  PropertyConfigurator.configure("logging.properties");
  Properties serverProperties = new Properties();
  serverProperties.load(new FileReader("xoola.properties"));

  serverProperties.put(XoolaProperty.CLIENT_ACCESS_CONTROLLER_CLASS, MultiControl.class.getName());
  serverProperties.put(XoolaProperty.MODE, XoolaTierMode.SERVER);
  server = Xoola.init(serverProperties);

  Properties clientProperties = new Properties();
  clientProperties.load(new FileReader("xoola.properties"));
  clientProperties.put(XoolaProperty.MODE, XoolaTierMode.CLIENT);
  int num = 10;
  for (int i = 0; i < num; ++i) {
   clientProperties.put(XoolaProperty.CLIENTID, "" + i);
   Xoola client = Xoola.init(clientProperties);
   clients.add(client);
  }

  server.start();

  final CountDownLatch latch = new CountDownLatch(num);
  for (Xoola xoola : clients) {
   xoola.addConnectionListener(new XoolaConnectionListener() {
    @Override
    public void disconnected(XoolaInvocationHandler xoolaInvocationHandler, XoolaChannelState xcs) {
    }

    @Override
    public void connected(XoolaInvocationHandler xoolaInvocationHandler, XoolaChannelState xcs) {
     latch.countDown();
    }
   });
   xoola.start();
  }
  latch.await();
 }

 @AfterClass
 public static void tearDown() {
  for (Xoola client : clients) {
   client.close();
  }
  server.close();
 }

 @Before
 public void before() {
  System.out.println("Running test " + name.getMethodName());
 }

 @Test
 public void testInvokeServer() throws InterruptedException {
  MultiFace mf = new MultiFace();
  server.registerObject("multiFace", mf);

  for (Xoola client : clients) {
   IMultiFace imf = client.get(IMultiFace.class, "multiFace");
   imf.incrementAndGet();
  }

  Assert.assertEquals(clients.size(), mf.i);
 }

 @Test
 public void testInvokeClients() throws InterruptedException {
  MultiFace mf[] = new MultiFace[clients.size()];

  for (Xoola client : clients) {
   int id = Integer.parseInt(client.getId());
   mf[id] = new MultiFace();
   client.registerObject("multiFace", mf[id]);
  }

  for (Xoola client : clients) {
   IMultiFace imf = server.get(IMultiFace.class, client.getId(), "multiFace");
   int clid = Integer.parseInt(client.getId());
   for (int i = 0; i < clid; i++) {
    imf.incrementAndGet();
   }
   System.err.println(mf[clid].i);
   Assert.assertEquals(clid, mf[clid].i);
  }
 }

 public interface IMultiFace {
  public int incrementAndGet();
 }

 public class MultiFace implements IMultiFace {
  public int i = 0;

  @Override
  public synchronized int incrementAndGet() {
   return ++i;
  }

 }
}
