// port-lint: source sensitive_command.rs
package io.github.kotlinmania.awsconfig

/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

internal class CommandWithSensitiveArgs<T : CharSequence> private constructor(
    private val value: T,
) {
    internal fun toOwnedString(): CommandWithSensitiveArgs<String> =
        CommandWithSensitiveArgs(value.toString())

    internal fun unredacted(): String = value.toString()

    override fun toString(): String {
        // Security: The arguments for command must be redacted since they can be sensitive.
        val command = value.toString()
        val index = command.indexOfFirst { it.isWhitespace() }
        return if (index >= 0) {
            "${command.substring(0, index)} ** arguments redacted **"
        } else {
            command
        }
    }

    internal companion object {
        internal fun <T : CharSequence> new(value: T): CommandWithSensitiveArgs<T> =
            CommandWithSensitiveArgs(value)
    }
}
