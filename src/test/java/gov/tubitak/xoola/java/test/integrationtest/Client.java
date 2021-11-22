package gov.tubitak.xoola.java.test.integrationtest;

public class Client {

  static {
    System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "DEBUG");
  }

  public static void main(String[] args) {
    IntegrationTestViaUI.client();
  }
}