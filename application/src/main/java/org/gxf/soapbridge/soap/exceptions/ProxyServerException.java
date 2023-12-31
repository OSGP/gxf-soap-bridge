// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0
package org.gxf.soapbridge.soap.exceptions;

import java.io.Serial;

/** Base type for exceptions for proxy server component. */
public class ProxyServerException extends Exception {

  /** Serial Version UID. */
  @Serial private static final long serialVersionUID = -8696835428244659385L;

  public ProxyServerException(final String message) {
    super(message);
  }

  public ProxyServerException(final String message, final Throwable t) {
    super(message, t);
  }
}
