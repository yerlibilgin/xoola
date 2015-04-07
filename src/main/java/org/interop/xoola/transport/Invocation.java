/*
 * XoolA is a remote method call bridge between java and dotnet platforms.
 * Copyright (C) 2010 Muhammet YILDIZ, Doğan ERSÖZ
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.interop.xoola.transport;

/**
 * @author dogan, muhammet
 * 
 */
public class Invocation implements TransportObject {
  private static final long serialVersionUID = -199341454272572550L;

  public static Invocation createMethodCall(String objectName, String methodName, Object... args) {
    return new Invocation(objectName, methodName, InvocationType.CALL, args);
  }

  /**
   * creates a copy of the given transport object
   * 
   * @param object
   * @return
   */
  public static Invocation copy(Invocation object) {
    return new Invocation(object.objectName, object.methodName, object.type, object.params);
  }

  /**
   * @return
   */
  public static Invocation createEmptyObject() {
    return new Invocation(null, null, null, null);
  }

  public static void main(String[] args) {
    Invocation in = createEmptyObject();
    Invocation in2 = createEmptyObject();

    System.out.println(in.getClass());
    System.out.println(in2.getClass());
  }

  public String objectName;
  public String methodName;
  public InvocationType type;
  public Object[] params;

  /**
   * 
   */
  public Invocation() {
  }

  /**
   * @param objectName
   * @param methodName
   * @param type
   * @param params
   * @param returnValue
   */
  private Invocation(String objectName, String methodName, InvocationType type, Object[] params) {
    this.objectName = objectName;
    this.methodName = methodName;
    this.type = type;
    this.params = params;
  }
}
