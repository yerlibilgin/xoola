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
package gov.tubitak.xoola.core;

/**
 *
 * Objects implementing this interface provide a service to the
 * outside world about cancelling whatever job they are doing now.
 *
 * The primary aim of this interface is to enable clients to cancel the
 * current "invocation" that xoola caller thead is frozen on.
 *
 * This must be called from another thread that initiated the call and the thread should not have locked
 * on the same resouce (by chance).
 * @author yerlibilgin
 */
public interface CancellableInvocation {
  /**
   * Call to send a cancel request
   */
  void cancel();
}
