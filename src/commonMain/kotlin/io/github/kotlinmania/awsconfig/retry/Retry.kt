// port-lint: source retry.rs
package io.github.kotlinmania.awsconfig.retry

/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

// Retry configuration.

/** Errors for retry configuration. */
internal sealed class RetryConfigErrorKind {
    /** The configured retry mode was not recognized. */
    internal data class InvalidRetryMode(
        /** Cause of the error. */
        val source: Throwable,
    ) : RetryConfigErrorKind()

    /** Max attempts must be greater than zero. */
    internal data object MaxAttemptsMustNotBeZero : RetryConfigErrorKind()

    /** The max attempts value could not be parsed to an integer. */
    internal data class FailedToParseMaxAttempts(
        /** Cause of the error. */
        val source: Throwable,
    ) : RetryConfigErrorKind()
}

/** Failure to parse retry config from profile file or environment variable. */
class RetryConfigError internal constructor(
    internal val kind: RetryConfigErrorKind,
) : Exception(messageFor(kind), sourceFor(kind)) {
    internal fun fmt(): String = when (kind) {
        is RetryConfigErrorKind.InvalidRetryMode -> "invalid retry configuration"
        RetryConfigErrorKind.MaxAttemptsMustNotBeZero ->
            "invalid configuration: It is invalid to set max attempts to 0. " +
                "Unset it or set it to an integer greater than or equal to one."
        is RetryConfigErrorKind.FailedToParseMaxAttempts -> "failed to parse max attempts"
    }

    internal fun source(): Throwable? = when (kind) {
        is RetryConfigErrorKind.InvalidRetryMode -> kind.source
        is RetryConfigErrorKind.FailedToParseMaxAttempts -> kind.source
        RetryConfigErrorKind.MaxAttemptsMustNotBeZero -> null
    }

    override fun toString(): String = fmt()

    companion object {
        internal fun from(kind: RetryConfigErrorKind): RetryConfigError = RetryConfigError(kind)

        private fun messageFor(kind: RetryConfigErrorKind): String = when (kind) {
            is RetryConfigErrorKind.InvalidRetryMode -> "invalid retry configuration"
            RetryConfigErrorKind.MaxAttemptsMustNotBeZero ->
                "invalid configuration: It is invalid to set max attempts to 0. " +
                    "Unset it or set it to an integer greater than or equal to one."
            is RetryConfigErrorKind.FailedToParseMaxAttempts -> "failed to parse max attempts"
        }

        private fun sourceFor(kind: RetryConfigErrorKind): Throwable? = when (kind) {
            is RetryConfigErrorKind.InvalidRetryMode -> kind.source
            is RetryConfigErrorKind.FailedToParseMaxAttempts -> kind.source
            RetryConfigErrorKind.MaxAttemptsMustNotBeZero -> null
        }
    }
}
