package org.interop.xoola.java.test.ecodex;

import java.io.Serializable;

/**
 * The message that travels across the net. You can send ANY serializable
 * message. You can call any objects any method but the parameters and return
 * types have to be serializable...
 * 
 * @author muhammet
 * 
 */
@SuppressWarnings("serial")
public class MyHighTechnologicalMessage implements Serializable {
 public String esasMesaj;

 public MyHighTechnologicalMessage() {
 }

 public MyHighTechnologicalMessage(String esasMesaj) {
  this.esasMesaj = esasMesaj;
 }
}
