package gov.tubitak.xoola.java.test;

import java.io.FileReader;
import java.util.Properties;
import gov.tubitak.xoola.core.Xoola;
import gov.tubitak.xoola.core.XoolaProperty;
import gov.tubitak.xoola.core.XoolaTierMode;
import gov.tubitak.xoola.java.test.model.Remote;
import gov.tubitak.xoola.java.test.model.RemoteInterface;
import gov.tubitak.xoola.tcpcom.connmanager.server.ClientAccessController;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by yerlibilgin on 11/05/15.
 */
public class InterruptTest {

  public static class OneToOneControl implements ClientAccessController {
    String client = "";

    {
      client = InterruptTest.xoolaProperties.get(XoolaProperty.CLIENTID).toString();
    }

    @Override
    public boolean clientIsAllowed(String id) {
      return client.equals(id);
    }

  }

  private static final Logger LOGGER = LoggerFactory.getLogger(TestXoolaOneToOne.class);
  private static Xoola client;
  private static Xoola server;

  @Rule
  public TestName name = new TestName();
  public static Properties xoolaProperties;

  @BeforeClass
  public static void setUp() throws Throwable {
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
    LOGGER.debug("Running test " + name.getMethodName());
  }


  /**
   * One big test case for all the purpose.
   *
   * @throws InterruptedException
   */
  @Test
  public void testInterrupt() throws InterruptedException {
    Remote acaip = new Remote();
    server.registerObject("remoteTestObject", acaip);

    RemoteInterface proxy = client.get(RemoteInterface.class, "remoteTestObject");

    final Thread mainThread = Thread.currentThread();

    new Thread() {
      @Override
      public void run() {
        //wait 3 milliseconds and then interrupt the main thread.
        try {
          Thread.sleep(10000);
          mainThread.interrupt();
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }.start();
    boolean interrupted = false;
    try {
      proxy.tooLongMethod();
    } catch (Exception ex) {
      LOGGER.error(ex.getMessage(), ex);
      Throwable cause = ex;
      while(cause.getCause() != null) cause = cause.getCause();

      if (cause instanceof InterruptedException)
        interrupted = true;
    }

    if (interrupted == false)
      throw new RuntimeException("Thread was not interrupted!");

    server.unregisterObject("remoteTestObject");

  }
}
