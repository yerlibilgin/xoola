package gov.tubitak.xoola.java.test.integrationtest;

import gov.tubitak.xoola.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.util.Properties;
import java.util.function.Consumer;

public class IntegrationTestViaUI {

  static {
    System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "DEBUG");
  }
  private static final Logger LOGGER = LoggerFactory.getLogger(IntegrationTestViaUI.class);

  public static void server() {
    final String title = "Server";

    final SimpleUI ui = createUI(title);
    Properties properties = new Properties();
    properties.setProperty(XoolaProperty.HOST, "localhost");
    properties.setProperty(XoolaProperty.PORT, "2500");
    properties.setProperty(XoolaProperty.MODE, XoolaTierMode.SERVER);
    properties.setProperty(XoolaProperty.CLIENTID, "meinClient");
    properties.setProperty(XoolaProperty.SERVERID, "meinServer");
    final Xoola xoola = Xoola.init(properties);
    xoola.registerObject("haydarServer", (Consumer<String>) s -> ui.tfReceived.setText("Client sent " + s));
    xoola.start();
    ui.button1.addActionListener(e -> {
      final Consumer haydar = xoola.get(Consumer.class, "meinClient", "haydarClient");
      haydar.accept(ui.tfOut.getText());
    });
    xoola.addConnectionListener(new XoolaConnectionListener() {
      @Override
      public void connected(XoolaInvocationHandler xoolaInvocationHandler, XoolaChannelState xcs) {
        LOGGER.debug("Client Connected");
        ui.labelConnectionStatus.setBackground(Color.green);
      }

      @Override
      public void disconnected(XoolaInvocationHandler xoolaInvocationHandler, XoolaChannelState xcs) {
        LOGGER.debug("Client Disconnected");
        ui.labelConnectionStatus.setBackground(Color.red);
      }
    });

  }

  public static void client() {
    final String title = "Client";
    final SimpleUI ui = createUI(title);

    Properties properties = new Properties();
    properties.setProperty(XoolaProperty.HOST, "localhost");
    properties.setProperty(XoolaProperty.PORT, "2500");
    properties.setProperty(XoolaProperty.MODE, XoolaTierMode.CLIENT);
    properties.setProperty(XoolaProperty.CLIENTID, "meinClient");
    properties.setProperty(XoolaProperty.SERVERID, "meinServer");

    final Xoola xoola = Xoola.init(properties);
    xoola.registerObject("haydarClient", (Consumer<String>) s -> ui.tfReceived.setText("Server sent " + s));

    ui.button1.addActionListener(e -> {
      final Consumer haydar = xoola.get(Consumer.class, "haydarServer");
      haydar.accept(ui.tfOut.getText());
    });
    xoola.addConnectionListener(new XoolaConnectionListener() {
      @Override
      public void connected(XoolaInvocationHandler xoolaInvocationHandler, XoolaChannelState xcs) {
        LOGGER.debug("Server Connected");
        ui.labelConnectionStatus.setBackground(Color.green);
      }

      @Override
      public void disconnected(XoolaInvocationHandler xoolaInvocationHandler, XoolaChannelState xcs) {
        LOGGER.debug("Server Disconnected");
        ui.labelConnectionStatus.setBackground(Color.red);
      }
    });
    xoola.start();
  }

  private static SimpleUI createUI(String title) {
    SimpleUI simpleUI = new SimpleUI();
    simpleUI.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
    final GraphicsDevice[] screenDevices = ge.getScreenDevices();

    for (GraphicsDevice screenDevice : screenDevices) {
      System.out.println(screenDevice.getIDstring());
      System.out.println(screenDevice.getDisplayMode());
      System.out.println(screenDevice.getDefaultConfiguration().getBounds());
    }

    if (screenDevices.length >= 3) {

      final Rectangle bounds = screenDevices[1].getDefaultConfiguration().getBounds();
      simpleUI.setLocation(bounds.x + (int) (Math.random() * bounds.width / 2),
          bounds.y + (int) (Math.random() * bounds.height / 2)
      );

    }
    simpleUI.setTitle(title);
    simpleUI.setVisible(true);

    return simpleUI;
  }
}
