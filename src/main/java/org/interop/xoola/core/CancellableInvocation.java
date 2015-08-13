package org.interop.xoola.core;

/**
 * Created by yerlibilgin on 11/05/15.
 *
 * Objects implementing this interface provide a service to the
 * outside world about cancelling whatever job they are doing now.
 *
 * The primary aim of this interface is to enable clients to cancel the
 * current "invocation" that xoola caller thead is frozen on.
 *
 * This must be called from another thread that initiated the call and the thread should not have locked
 * on the same resouce (by chance).
 */
public interface CancellableInvocation {
  public void cancel();
}
