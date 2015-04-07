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
package org.interop.xoola.core;

/**
 * @author muhammet
 * 
 */
public interface XoolaProperty {
  public static final String MODE = "MODE";
  public static final String HOST = "HOST";
  public static final String PORT = "PORT";
  public static final String PING_PORT = "PING_PORT";
  public static final String SERVERID = "SERVERID";
  public static final String CLIENTID = "CLIENTID";
  public static final String NETWORK_RESPONSE_TIMEOUT = "NETWORK_RESPONSE_TIMEOUT";
  public static final String RECEIVE_BUFFER_SIZE = "RECEIVE_BUFFER_SIZE";
  public static final String PING_TIMEOUT = "PING_TIMEOUT";
  public static final String CLIENT_ACCESS_CONTROLLER_CLASS = "CLIENT_ACCESS_CONTROLLER_CLASS";
}
