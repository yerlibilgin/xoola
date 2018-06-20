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
package gov.tubitak.xoola.core;

/**
 * @author muhammet
 */
public interface XoolaPropertyDefaults {
   String HOST = "localhost";
   String SERVERID = "xoolaServer";
   String CLIENTID = "xoolaClient";

   int PORT = 25000;
   int NETWORK_RESPONSE_TIMEOUT = 50000;
   int PING_TIMEOUT = 50000;
   int RECONNECT_RETRY_TIMEOUT = 10000;
   int HANDSHAKE_TIMEOUT = 20000;

   String CLIENT_ACCESS_CONTROLLER_CLASS = "CLIENT_ACCESS_CONTROLLER_CLASS";
   String CLASS_LOADER_PROVIDER_CLASS = "CLASS_LOADER_PROVIDER_CLASS";
}
