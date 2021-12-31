/*
 * Copyright 2021-TUBITAK BILGEM
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package gov.tubitak.xoola.transport;

/**
 * Represents a remote invocation
 *
 * @author yerlibilgin
 */
public class Invocation implements TransportObject {
  private String invocationUID;

  /**
   * Create method call invocation.
   *
   * @param objectName the object name
   * @param methodName the method name
   * @param args       the args
   * @return the invocation
   */
  public static Invocation createMethodCall(String objectName, String methodName, Object... args) {
    return new Invocation(objectName, methodName, InvocationType.CALL, args);
  }

  /**
   * creates a copy of the given transport object
   *
   * @param object the object
   * @return invocation invocation
   */
  public static Invocation copy(Invocation object) {
    return new Invocation(object.objectName, object.methodName, object.type, object.params);
  }

  /**
   * Create empty object invocation.
   *
   * @return invocation invocation
   */
  public static Invocation createEmptyObject() {
    return new Invocation(null, null, null, null);
  }

  /**
   * The Object name.
   */
  public String objectName;
  /**
   * The Method name.
   */
  public String methodName;
  /**
   * The Type.
   */
  public InvocationType type;
  /**
   * The Params.
   */
  public Object[] params;

  /**
   * Instantiates a new Invocation.
   */
  public Invocation() {
  }

  private Invocation(String objectName, String methodName, InvocationType type, Object[] params) {
    this.objectName = objectName;
    this.methodName = methodName;
    this.type = type;
    this.params = params;
  }

  public String getInvocationUID() {
    return invocationUID;
  }

  public void setInvocationUID(String invocationUID) {
    this.invocationUID = invocationUID;
  }
}
