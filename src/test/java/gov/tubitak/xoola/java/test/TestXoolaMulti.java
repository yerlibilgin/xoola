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
package gov.tubitak.xoola.java.test;

import gov.tubitak.xoola.core.*;
import gov.tubitak.xoola.tcpcom.connmanager.server.ClientAccessController;
import org.junit.*;
import org.junit.rules.TestName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

/**
 * Intended for tests.
 *
 * @author yerlibilgin
 */
@SuppressWarnings("unused")
public class TestXoolaMulti {
  static {
    System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "DEBUG");
  }

  private static final Logger LOGGER = LoggerFactory.getLogger(TestXoolaMulti.class);

  public static class MultiControl implements ClientAccessController {
    @Override
    public boolean clientIsAllowed(String id) {
      int idi = Integer.parseInt(id);
      return 0 <= idi && idi < 100;
    }
  }

  private static Xoola server;
  private static ArrayList<Xoola> clients = new ArrayList<Xoola>();

  @Rule
  public TestName name = new TestName();

  @BeforeClass
  public static void setUp() throws Throwable {
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
  public static void tearDown() throws InterruptedException {
    Thread.sleep(1500);
    for (Xoola client : clients) {
      client.close();
    }
    server.close();
  }

  @Before
  public void before() {
    LOGGER.debug("Running test " + name.getMethodName());
  }

  @After
  public void after() {
    LOGGER.debug("Finished " + name.getMethodName());
  }

  @Test
  public void testInvokeServer() {
    MultiFace mf = new MultiFace();
    server.registerObject("multiFace", mf);

    for (Xoola client : clients) {
      IMultiFace imf = client.get(IMultiFace.class, "multiFace");
      imf.incrementAndGet();
    }

    org.junit.Assert.assertEquals(clients.size(), mf.i);
  }

  @Test
  public void testInvokeClients() {
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
      Assert.assertEquals(clid, mf[clid].i);
    }
  }

  public interface IMultiFace {
    int incrementAndGet();
  }

  public class MultiFace implements IMultiFace {
    public int i = 0;

    @Override
    public synchronized int incrementAndGet() {
      return ++i;
    }

  }
}
