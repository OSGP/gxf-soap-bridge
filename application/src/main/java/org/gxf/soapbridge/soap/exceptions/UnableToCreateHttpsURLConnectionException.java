// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package org.gxf.soapbridge.soap.exceptions;

import java.io.Serial;

public class UnableToCreateHttpsURLConnectionException extends ProxyServerException {

  @Serial private static final long serialVersionUID = -8807766325167125880L;

  public UnableToCreateHttpsURLConnectionException(
      final String message, final Throwable throwable) {
    super(message, throwable);
  }
}
