package org.interop.xoola.java.test;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.interop.xoola.core.Xoola;
import org.interop.xoola.core.XoolaProperty;
import org.interop.xoola.core.XoolaTierMode;
import org.interop.xoola.tcpcom.connmanager.server.ClientAccessController;
import org.junit.*;
import org.junit.Test;
import org.junit.rules.TestName;

import java.io.FileReader;
import java.util.Properties;

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
  private static final Logger LOGGER = Logger.getLogger(TestXoolaOneToOne.class);
  private static Xoola client;
  private static Xoola server;

  @Rule
  public TestName name = new TestName();
  public static Properties xoolaProperties;

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

    new Thread(){
      @Override
      public void run() {
        //wait 3 milliseconds and then interrupt the main thread.
        try {
          Thread.sleep(3000);
          mainThread.interrupt();
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }.start();
    try {
      proxy.tooLongMethod();

      throw new Exception("Thread was not interrupted!");
    }catch (Exception ex) {
      ex.printStackTrace();


    }

    server.unregisterObject("remoteTestObject");

  }
}
