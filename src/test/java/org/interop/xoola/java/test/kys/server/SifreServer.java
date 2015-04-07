package org.interop.xoola.java.test.kys.server;

import java.io.IOException;
import java.util.Properties;

import javax.swing.JFrame;

import org.apache.log4j.PropertyConfigurator;
import org.interop.xoola.core.Xoola;
import org.interop.xoola.core.XoolaProperty;
import org.interop.xoola.core.XoolaTierMode;

public class SifreServer {
  private Xoola server;

  public SifreServer() throws InterruptedException, IOException {
    Properties p = new Properties();
    p.load(this.getClass().getResourceAsStream("sifre.properties"));
    p.put(XoolaProperty.MODE, XoolaTierMode.SERVER); // I am a server
    server = Xoola.init(p);    
    /*
     * merveyse şifre girecek olan tarafın nesnesini register et.
     */
    server.registerObject("sifreRemote", new SifreRemote());
    server.start();
  }

  public static void main(String[] args) throws InterruptedException, IOException {
    PropertyConfigurator.configure("logging.properties");
    new SifreServer();
    JFrame frame = new JFrame("Merveys Nöbetçisi");
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setLocationByPlatform(true);
    frame.setSize(200, 20);
    frame.setVisible(true);
  }
}
