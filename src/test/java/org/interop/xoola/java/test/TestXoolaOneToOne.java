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
import java.util.Properties;

import junit.framework.Assert;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.interop.xoola.core.Xoola;
import org.interop.xoola.core.XoolaProperty;
import org.interop.xoola.core.XoolaTierMode;
import org.interop.xoola.java.test.TestXoolaMulti.MultiControl;
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
public class TestXoolaOneToOne {

 public class OneToOneControl implements ClientAccessController {
  String client = "";
  {
   client = xoolaProperties.get(XoolaProperty.CLIENTID).toString();
  }

  @Override
  public boolean clientIsAllowed(String id) {
   return client.equals(id);
  }

 }

 private static final Logger LOGGER = Logger.getLogger(TestXoolaOneToOne.class);
 private static Xoola client;
 private static Xoola server;

 @Rule
 public TestName name = new TestName();
 private static Properties xoolaProperties;

 @BeforeClass
 public static void setUp() throws Throwable {
  PropertyConfigurator.configure("logging.properties");
  xoolaProperties = new Properties();
  xoolaProperties.load(new FileReader("xoola.properties"));

  xoolaProperties.put(XoolaProperty.MODE, XoolaTierMode.SERVER);
  xoolaProperties.put(XoolaProperty.CLIENT_ACCESS_CONTROLLER_CLASS, OneToOneControl.class.getName());
  server = Xoola.init(xoolaProperties);
  xoolaProperties.put(XoolaProperty.MODE, XoolaTierMode.CLIENT);
  client = Xoola.init(xoolaProperties);

  server.start();
  client.start();
  client.waitForConnection();
 }

 @AfterClass
 public static void tearDown() {
  client.close();
  server.close();
 }

 @Before
 public void before() {
  System.out.println("Running test " + name.getMethodName());
 }

 @Test
 public void testNullParam() throws InterruptedException {
  Remote acaip = new Remote();
  server.registerObject("remoteTestObject", acaip);
  client.registerObject("localTestObject", new Local());

  RemoteInterface proxy = client.get(RemoteInterface.class, "remoteTestObject");
  LocalInterface proxy2 = server.get(LocalInterface.class, xoolaProperties.get(XoolaProperty.CLIENTID).toString(), "localTestObject");

  int a = 10;
  Double b = 25.8974;
  int c = 15;
  Double d = 25.5;

  final int TIMES = 100;
  String strangeAddMethod = proxy2.strangeAddMethod2(c, d, null);
  Assert.assertEquals("localllll40.5", strangeAddMethod);
  server.unregisterObject("remoteTestObject");
  client.unregisterObject("localTestObject");
 }

 /**
  * One big test case for all the purpose.
  * 
  * @throws InterruptedException
  */
 @Test
 public void testInvoke() throws InterruptedException {
  Remote acaip = new Remote();
  server.registerObject("remoteTestObject", acaip);
  client.registerObject("localTestObject", new Local());

  RemoteInterface proxy = client.get(RemoteInterface.class, "remoteTestObject");
  LocalInterface proxy2 = server.get(LocalInterface.class, "heClient", "localTestObject");

  int a = 10;
  Double b = 25.8974;
  int c = 15;
  Double d = 25.5;

  final int TIMES = 100;
  for (int i = 0; i < TIMES; ++i) {
   proxy.strangeAddMethod3();
   String strangeAddMethod = proxy2.strangeAddMethod2(c, d, "Kanka");
   Assert.assertEquals("localllll40.5", strangeAddMethod);

   if (i % 200 == 0)
    LOGGER.debug("-----> " + i);

  }
  server.unregisterObject("remoteTestObject");
  client.unregisterObject("localTestObject");

 }

 /**
  * Async test
  */
 @Test
 public void testAsync() throws InterruptedException {
  Remote acaip = new Remote();
  server.registerObject("remoteAsync", acaip);
  RemoteInterface proxy = client.get(RemoteInterface.class, "remoteAsync", true);

  final int TIMES = 3;

  for (int j = 0; j < 3; j++) {
   for (int i = 0; i < TIMES; ++i) {
    proxy.strangeVoidMethod((long) i);
    Thread.sleep(20);
    if (i % 200 == 0)
     LOGGER.debug("-----> " + i);
   }
  }
 }
}
