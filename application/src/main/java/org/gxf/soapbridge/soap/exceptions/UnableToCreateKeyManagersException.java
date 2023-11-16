// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package org.gxf.soapbridge.soap.exceptions;

import java.io.Serial;

public class UnableToCreateKeyManagersException extends ProxyServerException {

  /** Serial Version UID. */
  @Serial
  private static final long serialVersionUID = -100586751704652623L;

  public UnableToCreateKeyManagersException(final String message, final Throwable t) {
    super(message, t);
  }
}
