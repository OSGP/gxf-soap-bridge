// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package org.gxf.soapbridge.soap.exceptions;

import java.io.Serial;

public class UnableToCreateTrustManagersException extends ProxyServerException {

  /** Serial Version UID. */
  @Serial
  private static final long serialVersionUID = -855694158211466200L;

  public UnableToCreateTrustManagersException(final String message, final Throwable t) {
    super(message, t);
  }
}
