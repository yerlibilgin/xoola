package org.interop.xoola.java.test;

import org.apache.log4j.PropertyConfigurator;
import org.interop.xoola.core.Xoola;
import org.interop.xoola.core.XoolaProperty;
import org.interop.xoola.core.XoolaTierMode;

import java.io.FileReader;
import java.util.Properties;

/**
 * @author: Bulut
 * @date: 07/03/17.
 */
public class SimpleServer {
   public static void main(String[] args) throws Exception {
      PropertyConfigurator.configure("logging.properties");
      Properties xoolaProperties = new Properties();
      xoolaProperties.load(new FileReader("xoola.properties"));

      xoolaProperties.put(XoolaProperty.MODE, XoolaTierMode.SERVER);
      Xoola server = Xoola.init(xoolaProperties);

      server.registerObject("MYOBJECT", new Runnable() {
         @Override
         public void run() {
            System.out.println("Running on server");
         }
      });
      server.start();


      Thread.sleep(1000000L);
   }
}
