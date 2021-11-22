/*
 * Created by JFormDesigner on Mon Nov 22 12:20:32 TRT 2021
 */

package gov.tubitak.xoola.java.test.integrationtest;

import java.awt.*;
import javax.swing.*;

/**
 * @author Muhammet YILDIZ
 */
public class SimpleUI extends JFrame {
  public SimpleUI() {
    initComponents();
  }

  private void initComponents() {
    // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
    label1 = new JLabel();
    labelConnectionStatus = new JLabel();
    button1 = new JButton();
    tfOut = new JTextField();
    label2 = new JLabel();
    tfReceived = new JTextField();

    //======== this ========
    Container contentPane = getContentPane();
    contentPane.setLayout(new GridBagLayout());
    ((GridBagLayout)contentPane.getLayout()).columnWidths = new int[] {0, 0, 273};

    //---- label1 ----
    label1.setText("ConnectionStatus");
    contentPane.add(label1, new GridBagConstraints(1, 1, 1, 1, 0.0, 0.0,
      GridBagConstraints.CENTER, GridBagConstraints.BOTH,
      new Insets(0, 0, 5, 5), 0, 0));

    //---- labelConnectionStatus ----
    labelConnectionStatus.setOpaque(true);
    labelConnectionStatus.setBackground(Color.red);
    contentPane.add(labelConnectionStatus, new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0,
      GridBagConstraints.CENTER, GridBagConstraints.BOTH,
      new Insets(0, 0, 5, 0), 0, 0));

    //---- button1 ----
    button1.setText("Send Message");
    contentPane.add(button1, new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0,
      GridBagConstraints.CENTER, GridBagConstraints.BOTH,
      new Insets(0, 0, 5, 5), 0, 0));
    contentPane.add(tfOut, new GridBagConstraints(2, 3, 1, 1, 0.0, 0.0,
      GridBagConstraints.CENTER, GridBagConstraints.BOTH,
      new Insets(0, 0, 5, 0), 0, 0));

    //---- label2 ----
    label2.setText("Received");
    contentPane.add(label2, new GridBagConstraints(1, 4, 1, 1, 0.0, 0.0,
      GridBagConstraints.CENTER, GridBagConstraints.BOTH,
      new Insets(0, 0, 5, 5), 0, 0));
    contentPane.add(tfReceived, new GridBagConstraints(2, 4, 1, 1, 0.0, 0.0,
      GridBagConstraints.CENTER, GridBagConstraints.BOTH,
      new Insets(0, 0, 5, 0), 0, 0));
    setSize(520, 350);
    setLocationRelativeTo(getOwner());
    // JFormDesigner - End of component initialization  //GEN-END:initComponents
  }

  // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
  private JLabel label1;
  JLabel labelConnectionStatus;
  JButton button1;
  JTextField tfOut;
  private JLabel label2;
  JTextField tfReceived;
  // JFormDesigner - End of variables declaration  //GEN-END:variables
}
