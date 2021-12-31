package gov.tubitak.xoola.internal;

import gov.tubitak.xoola.transport.TransportObject;

import java.io.Serializable;

public class Response implements Serializable, TransportObject {

  public String invocationUID;
  public Object value;

  public static Response create(String invocationUID, Object value) {
    Response response = new Response();
    response.invocationUID = invocationUID;
    response.value = value;
    return response;
  }
}
