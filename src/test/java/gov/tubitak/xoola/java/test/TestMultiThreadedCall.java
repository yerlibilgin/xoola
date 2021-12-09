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
import org.slf4j.impl.SimpleLogger;

import java.io.FileReader;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Intended for tests.
 *
 * @author yerlibilgin
 */
@SuppressWarnings("unused")
public class TestMultiThreadedCall {
  static {
    System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "TRACE");
    System.setProperty(SimpleLogger.SHOW_DATE_TIME_KEY, "true");
  }

  private static final Logger LOGGER = LoggerFactory.getLogger(TestMultiThreadedCall.class);

  public static class MultiControl implements ClientAccessController {
    @Override
    public boolean clientIsAllowed(String id) {
      int idi = Integer.parseInt(id);
      return 0 <= idi && idi < 100;
    }
  }

  private static Xoola server;
  private static Xoola client;

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

    clientProperties.put(XoolaProperty.CLIENTID, "1");
    client = Xoola.init(clientProperties);

    server.start();

    final CountDownLatch latch = new CountDownLatch(1);
    client.addConnectionListener(new XoolaConnectionListener() {
      @Override
      public void disconnected(XoolaInvocationHandler xoolaInvocationHandler, XoolaChannelState xcs) {
      }

      @Override
      public void connected(XoolaInvocationHandler xoolaInvocationHandler, XoolaChannelState xcs) {
        latch.countDown();
      }
    });
    client.start();

    latch.await();
  }

  @AfterClass
  public static void tearDown() throws InterruptedException {
    Thread.sleep(1500);
    client.close();
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
  public void testInvokeServer() throws InterruptedException {
    MultiFaceLongJob mf = new MultiFaceLongJob();
    server.registerObject("multiFace", mf);


    ExecutorService service = Executors.newFixedThreadPool(10);

    for (int i = 0; i < 30; i++) {
      int finalI = i;
      Thread.sleep(20);
      service.submit(new Runnable() {
        @Override
        public void run() {
          long start = System.currentTimeMillis();
          IMultiFace imf = client.get(IMultiFace.class, "multiFace");
          imf.workWithIndex(finalI);
          long diff = System.currentTimeMillis() - start;

          LOGGER.debug(">>> Difference: {}", diff);
          if (diff > 1900) {
            LOGGER.warn(">>>>>>>>>>>>>>>>>>>>>>---- Call #{} too Much time: {}", finalI, diff);
          }
        }
      });
    }

    service.awaitTermination(100, TimeUnit.SECONDS);
  }

  public interface IMultiFace {
    int workWithIndex(int id);
  }

  public static class MultiFaceLongJob implements IMultiFace {
    private static final Logger LOGGER = LoggerFactory.getLogger(MultiFaceLongJob.class);

    @Override
    public int workWithIndex(int id) {
      try {
        LOGGER.debug("--->>> Multiface call start ID: {}", id);
        Thread.sleep(1000);
        LOGGER.debug("--->>> Multiface call end   ID: {}", id);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }

      return id;
    }

  }
}
