// Copyright 2023 Alliander N.V.

package org.gxf.soapbridge.messaging.exceptions


class ProxyMessageException : Exception {
    constructor() : super()
    constructor(message: String?) : super(message)
    constructor(message: String?, t: Throwable?) : super(message, t)
}
