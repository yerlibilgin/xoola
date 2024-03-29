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
 * The type Xoola channel state.
 *
 * @author yerlibilgin
 */
public class XoolaChannelState {
  /**
   * The Remote id.
   */
  public String remoteId;
  /**
   * The Connected.
   */
  public boolean connected;

  /**
   * Instantiates a new Xoola channel state.
   */
  public XoolaChannelState() {
  }

  /**
   * Instantiates a new Xoola channel state.
   *
   * @param remoteId  the remote id
   * @param connected the connected
   */
  public XoolaChannelState(String remoteId, boolean connected) {
    this.remoteId = remoteId;
    this.connected = connected;
  }
}
