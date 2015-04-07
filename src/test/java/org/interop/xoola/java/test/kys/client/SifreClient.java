package org.interop.xoola.java.test.kys.client;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.Properties;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.UIManager;

import org.interop.xoola.core.Xoola;
import org.interop.xoola.core.XoolaChannelState;
import org.interop.xoola.core.XoolaConnectionListener;
import org.interop.xoola.core.XoolaInvocationHandler;
import org.interop.xoola.core.XoolaProperty;
import org.interop.xoola.core.XoolaTierMode;
import org.interop.xoola.java.test.kys.common.ISifreRemote;

public class SifreClient {
  static ISifreRemote remote;
  public static void main(String[] args) throws IOException {
    setLaf();
    
    Properties p = new Properties();
    p.load(SifreClient.class.getResourceAsStream("sifre.properties"));
    p.put(XoolaProperty.MODE, XoolaTierMode.CLIENT); // I am a client
    final Xoola client = Xoola.init(p);
    client.addConnectionListener(new XoolaConnectionListener() {

      @Override
      public void disconnected(XoolaInvocationHandler xoolaInvocationHandler, XoolaChannelState xcs) {
        //baglanti kesildi
        remote = null;
      }

      @Override
      public void connected(XoolaInvocationHandler xoolaInvocationHandler, XoolaChannelState xcs) {
        // bağlantı sağlandı.
        remote = client.get(ISifreRemote.class, "sifreRemote");
      }
    });
    client.start();

    final JFrame jf = new JFrame("Merveys tarafı");

    JButton sifreIste = new JButton("Pin Iste");
    sifreIste.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        if (remote == null){
          JOptionPane.showMessageDialog(jf, "Bagli degil", "Hata", JOptionPane.ERROR_MESSAGE);
          return;
        }
        String girilenSifre = remote.sifreGir();
        System.out.println("Girilen Şifre: " + girilenSifre);
        JOptionPane.showMessageDialog(jf, girilenSifre, "Girilen Şifre", JOptionPane.INFORMATION_MESSAGE);
      }
    });

    jf.add(sifreIste);
    jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    jf.setSize(200, 200);
    jf.setVisible(true);
  }
  private static void setLaf() {
    try{UIManager.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");}catch(Exception ex){
      try{UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());}catch(Exception ex2){}      
    }
  }
}
