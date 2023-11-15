// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0

package org.gxf.soapbridge.exceptions


class ProxyMessageException : Exception {
    constructor() : super()
    constructor(message: String?) : super(message)
    constructor(message: String?, t: Throwable?) : super(message, t)
}
