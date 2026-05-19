// port-lint: source json_credentials.rs
package io.github.kotlinmania.awsconfig

/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.time.Instant

internal sealed class InvalidJsonCredentials(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {
    /** The response did not contain valid JSON. */
    internal class JsonError(internal val error: Throwable) :
        InvalidJsonCredentials("invalid JSON in response: ${error.message ?: error}", error)

    /** The response was missing a required field. */
    internal class MissingField(internal val field: String) :
        InvalidJsonCredentials("Expected field `$field` in response but it was missing")

    /** A field was invalid. */
    internal class InvalidField(
        internal val field: String,
        internal val error: Throwable,
    ) :
        InvalidJsonCredentials("Invalid field in response: `$field`. ${error.message ?: error}", error)

    /** Another unhandled error occurred. */
    internal class Other(internal val msg: String) : InvalidJsonCredentials(msg)

    internal fun fmt(): String = when (this) {
        is JsonError -> "invalid JSON in response: ${error.message ?: error}"
        is MissingField -> "Expected field `$field` in response but it was missing"
        is Other -> msg
        is InvalidField -> "Invalid field in response: `$field`. ${error.message ?: error}"
    }

    override fun toString(): String = fmt()

    internal companion object {
        internal fun from(error: SerializationException): InvalidJsonCredentials = JsonError(error)

        internal fun from(error: IllegalArgumentException): InvalidJsonCredentials = JsonError(error)
    }
}

internal data class RefreshableCredentials(
    val accessKeyId: String,
    val secretAccessKey: String,
    val sessionToken: String,
    val accountId: String?,
    val expiration: Instant,
) {
    internal fun fmt(): String =
        "RefreshableCredentials(accessKeyId=$accessKeyId, " +
            "secretAccessKey=** redacted **, " +
            "sessionToken=** redacted **" +
            accountId?.let { ", accountId=$it" }.orEmpty() +
            ", expiration=$expiration)"

    override fun toString(): String = fmt()
}

internal sealed class JsonCredentials {
    data class RefreshableCredentials(
        val credentials: io.github.kotlinmania.awsconfig.RefreshableCredentials,
    ) : JsonCredentials()

    data class Error(
        val code: String,
        val message: String,
    ) : JsonCredentials()
}

/**
 * Deserialize an IMDS response from a string.
 *
 * There are two levels of error here: the top level distinguishes between a successfully parsed
 * response from the credential provider vs. something invalid or unexpected. The inner error
 * distinguishes between a successful response that contains credentials vs. an error with a code and
 * error message.
 *
 * Keys are case insensitive.
 */
internal fun parseJsonCredentials(credentialsResponse: String): Result<JsonCredentials> {
    var code: String? = null
    var accessKeyId: String? = null
    var secretAccessKey: String? = null
    var sessionToken: String? = null
    var accountId: String? = null
    var expiration: String? = null
    var message: String? = null

    jsonParseLoop(credentialsResponse) { key, value ->
        val stringValue = value.stringContentOrNull()
        when {
            key.equals("Code", ignoreCase = true) && stringValue != null -> {
                code = stringValue
            }
            key.equals("AccessKeyId", ignoreCase = true) && stringValue != null -> {
                accessKeyId = stringValue
            }
            key.equals("SecretAccessKey", ignoreCase = true) && stringValue != null -> {
                secretAccessKey = stringValue
            }
            (
                key.equals("Token", ignoreCase = true) ||
                    key.equals("SessionToken", ignoreCase = true)
                ) && stringValue != null -> {
                sessionToken = stringValue
            }
            key.equals("AccountId", ignoreCase = true) && stringValue != null -> {
                accountId = stringValue
            }
            (
                key.equals("Expiration", ignoreCase = true) ||
                    key.equals("ExpiresAt", ignoreCase = true)
                ) && stringValue != null -> {
                expiration = stringValue
            }

            // Error case handling: message will be set.
            key.equals("Message", ignoreCase = true) && stringValue != null -> {
                message = stringValue
            }
        }
        Result.success(Unit)
    }.getOrElse {
        return Result.failure(it)
    }

    return try {
        Result.success(
            when (val codeValue = code) {
                // IMDS does not appear to reply with a `Code` missing, but documentation indicates it
                // may be possible.
                null, "Success" -> {
                    val parsedAccessKeyId =
                        accessKeyId ?: throw InvalidJsonCredentials.MissingField("AccessKeyId")
                    val parsedSecretAccessKey =
                        secretAccessKey ?: throw InvalidJsonCredentials.MissingField("SecretAccessKey")
                    val parsedSessionToken =
                        sessionToken ?: throw InvalidJsonCredentials.MissingField("Token")
                    val parsedExpiration =
                        expiration ?: throw InvalidJsonCredentials.MissingField("Expiration")
                    val expirationInstant = try {
                        Instant.parse(parsedExpiration)
                    } catch (error: IllegalArgumentException) {
                        throw InvalidJsonCredentials.InvalidField("Expiration", error)
                    }

                    JsonCredentials.RefreshableCredentials(
                        RefreshableCredentials(
                            accessKeyId = parsedAccessKeyId,
                            secretAccessKey = parsedSecretAccessKey,
                            sessionToken = parsedSessionToken,
                            accountId = accountId,
                            expiration = expirationInstant,
                        ),
                    )
                }
                else -> JsonCredentials.Error(
                    code = codeValue,
                    message = message ?: "no message",
                )
            },
        )
    } catch (error: InvalidJsonCredentials) {
        Result.failure(error)
    }
}

internal fun jsonParseLoop(
    input: String,
    f: (String, JsonElement) -> Result<Unit>,
): Result<Unit> {
    val root = parseJsonObject(input).getOrElse {
        return Result.failure(it)
    }
    for ((key, value) in root) {
        f(key, value).getOrElse {
            return Result.failure(it)
        }
    }
    return Result.success(Unit)
}

private fun parseJsonObject(input: String): Result<JsonObject> {
    val element = try {
        Json.parseToJsonElement(input)
    } catch (error: SerializationException) {
        return Result.failure(InvalidJsonCredentials.from(error))
    } catch (error: IllegalArgumentException) {
        return Result.failure(InvalidJsonCredentials.from(error))
    }

    return try {
        Result.success(element.jsonObject)
    } catch (error: IllegalArgumentException) {
        Result.failure(
            InvalidJsonCredentials.JsonError(
                IllegalArgumentException("expected a JSON document starting with `{`", error),
            ),
        )
    }
}

private fun JsonElement.stringContentOrNull(): String? =
    (this as? JsonPrimitive)?.takeIf { it.isString }?.content
