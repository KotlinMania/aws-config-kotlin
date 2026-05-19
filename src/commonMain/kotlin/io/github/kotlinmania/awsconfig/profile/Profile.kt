// port-lint: source profile.rs
package io.github.kotlinmania.awsconfig.profile

/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// Load configuration from AWS Profiles.
//
// AWS profiles are typically stored in `~/.aws/config` and `~/.aws/credentials`. For more details
// see the `load` function.

internal sealed class ErrorTakingResult<out T, out E> {
    internal data class Ok<T>(val value: T) : ErrorTakingResult<T, Nothing>()
    internal data class Err<E>(val error: E) : ErrorTakingResult<Nothing, E>()
}

internal class ErrorTakingOnceCell<T, E> {
    private val mutex = Mutex()
    private var cell: Cell<T, E>? = null

    internal suspend fun getOrInit(
        init: suspend () -> ErrorTakingResult<T, E>,
        takenError: E,
    ): ErrorTakingResult<T, E> = mutex.withLock {
        when (val current = cell) {
            is Cell.Value -> ErrorTakingResult.Ok(current.value)
            is Cell.Error -> {
                val error = current.error
                current.error = takenError
                ErrorTakingResult.Err(error)
            }
            null -> when (val initialized = init()) {
                is ErrorTakingResult.Ok -> {
                    cell = Cell.Value(initialized.value)
                    ErrorTakingResult.Ok(initialized.value)
                }
                is ErrorTakingResult.Err -> {
                    cell = Cell.Error(initialized.error)
                    ErrorTakingResult.Err(initialized.error)
                }
            }
        }
    }

    private sealed class Cell<T, E> {
        data class Value<T, E>(val value: T) : Cell<T, E>()
        data class Error<T, E>(var error: E) : Cell<T, E>()
    }
}
