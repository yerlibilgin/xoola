package gov.tubitak.xoola.exception;

public class XoolaException extends IllegalStateException {
  public XoolaException() {
  }

  public XoolaException(String s) {
    super(s);
  }

  public XoolaException(String message, Throwable cause) {
    super(message, cause);
  }

  public XoolaException(Throwable cause) {
    super(cause);
  }
}
