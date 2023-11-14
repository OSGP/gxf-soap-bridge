// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package org.gxf.soapbridge.soap.exceptions;

public class ConnectionNotFoundInCacheException extends ProxyServerException {

  /** Serial Version UID. */
  private static final long serialVersionUID = -858760086093512799L;

  public ConnectionNotFoundInCacheException(final String message) {
    super(message);
  }
}
