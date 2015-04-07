package org.interop.xoola.java.test.kys.server;

import javax.swing.JOptionPane;

import org.interop.xoola.java.test.kys.common.ISifreRemote;

public class SifreRemote implements ISifreRemote{
  @Override
  public String sifreGir() {
    String result = JOptionPane.showInputDialog(null, "Sifre Gir lan");
    return result;
  }
}
