package gov.tubitak.xoola.tcpcom.connmanager.server;

public class DotPrinter {

  int MAX = 20;
  int current = 1;

  public void printDots(String prefix) {
    int i = 0;
    System.out.print(prefix);
    for (; i < current; i++) {
      System.out.print('.');
    }
    for (; i < MAX; i++) {
      System.out.print(' ');
    }
    System.out.print('\r');
    current = ++current % MAX;
  }
}
