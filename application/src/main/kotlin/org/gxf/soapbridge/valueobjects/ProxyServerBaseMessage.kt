// SPDX-FileCopyrightText: Copyright Contributors to the GXF project
//
// SPDX-License-Identifier: Apache-2.0

package org.gxf.soapbridge.valueobjects

import java.util.*


/** Base class for proxy-server messages.  */
abstract class ProxyServerBaseMessage(val connectionId: String) {
    var signature: String? = null

    protected abstract fun getFieldsForMessage(): List<String>

    /** Constructs a string separated by '~' from the fields of this instance.  */
    fun constructString() = getFieldsForMessage().joinToString(SEPARATOR, postfix = SEPARATOR)

    /** Constructs a string separated by '~' from the fields of this instance followed by the signature.  */
    fun constructSignedString() = constructString() + signature

    companion object {
        const val SEPARATOR = "~"
        fun encode(input: String): String = Base64.getEncoder().encodeToString(input.toByteArray())
        fun decode(input: String?): String = String(Base64.getDecoder().decode(input))
    }
}
