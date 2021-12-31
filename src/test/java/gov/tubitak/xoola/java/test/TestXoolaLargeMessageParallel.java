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

import java.io.FileReader;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import gov.tubitak.xoola.core.Xoola;
import gov.tubitak.xoola.core.XoolaProperty;
import gov.tubitak.xoola.core.XoolaTierMode;
import gov.tubitak.xoola.internal.tcpcom.connmanager.server.ClientAccessController;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Intended for tests.
 *
 * @author yerlibilgin
 */
@SuppressWarnings("unused")
public class TestXoolaLargeMessageParallel {
  private static final Logger LOGGER = LoggerFactory.getLogger(TestXoolaLargeMessageParallel.class);
  private static final int _1M = 1024 * 1024;
  private static Xoola server;
  private static Xoola client;

  public static class DummyControl implements ClientAccessController {

    /**
     * is the provided client id allowed to create connection to the system?
     *
     * @param id
     * @return
     */
    @Override
    public boolean clientIsAllowed(String id) {
      return true;
    }
  }

  @Rule
  public TestName name = new TestName();

  static MultiFace multiFace = new MultiFace();

  @BeforeClass
  public static void setUp() throws Throwable {
    Properties serverProperties = new Properties();
    serverProperties.load(new FileReader("xoola.properties"));

    serverProperties.put(XoolaProperty.MODE, XoolaTierMode.SERVER);
    serverProperties.put(XoolaProperty.CLIENT_ACCESS_CONTROLLER_CLASS, DummyControl.class.getName());
    server = Xoola.init(serverProperties);

    Properties clientProperties = new Properties();
    clientProperties.load(new FileReader("xoola.properties"));
    clientProperties.put(XoolaProperty.MODE, XoolaTierMode.CLIENT);
    clientProperties.put(XoolaProperty.CLIENTID, "CLIIIIIENNNT");
    client = Xoola.init(clientProperties);

    server.registerObject("multiFace", multiFace);
    server.start();
    client.start();
    Thread.sleep(1000);
  }

  @AfterClass
  public static void tearDown() {
    client.close();
    server.close();
  }

  @Before
  public void before() {
    LOGGER.debug("Running test " + name.getMethodName());
  }

  static long end = 0;
  @Test
  public void test100M() throws InterruptedException {
    long a = System.currentTimeMillis();
    try {
      final ILargeMessageService imf = client.get(ILargeMessageService.class, "multiFace");
      final byte[] b = new byte[100 * _1M];
      new Random().nextBytes(b);

      imf.initiate(b.length);

      int threadCount = 20;
      final int blockSize = b.length / threadCount;

      ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
      FutureTask<Object> futures[] = new FutureTask[threadCount];
      for (int i = 0; i < threadCount; ++i) {
        final int j = i;
        FutureTask<Object> future =
           new FutureTask<Object>(new Callable<Object>() {
             public String call() {
               byte[] sub = new byte[blockSize];
               System.arraycopy(b, j * blockSize, sub, 0, blockSize);
               imf.transfer(sub, j * blockSize, blockSize);
               return null;
             }
           });
        futures[i] = future;
        executorService.execute(future);
      }

      for (FutureTask<Object> ft : futures) {
        try {
          ft.get();
        } catch (ExecutionException e) {
          e.printStackTrace();
        }
      }

      LOGGER.debug("Time: " + (end - a));

      org.junit.Assert.assertArrayEquals(multiFace.buffer, b);

    } finally {


    }
  }


  public interface ILargeMessageService {
    public void transfer(byte[] param, int index, int length);

    public void initiate(int bytes);
  }

  public static class MultiFace implements ILargeMessageService {


    private byte[] buffer;

    @Override
    public synchronized void transfer(byte[] param, int index, int length) {
      LOGGER.debug(index + "");
      System.arraycopy(param, 0, buffer, index, length);

      LOGGER.debug("bitti");
      end = System.currentTimeMillis();
    }

    @Override
    public void initiate(int bytes) {
      this.buffer = new byte[bytes];
    }

  }
}
