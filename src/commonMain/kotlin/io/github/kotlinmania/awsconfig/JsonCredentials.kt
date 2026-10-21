// port-lint: source json_credentials.rs
package io.github.kotlinmania.awsconfig

/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

import io.github.kotlinmania.serde.SerdeError
import io.github.kotlinmania.serde.SerdeResult
import io.github.kotlinmania.serdecore.de.Deserialize
import io.github.kotlinmania.serdecore.de.Deserializer
import io.github.kotlinmania.serdecore.de.IgnoredAny
import io.github.kotlinmania.serdecore.de.MapAccess
import io.github.kotlinmania.serdecore.de.StringDeserialize
import io.github.kotlinmania.serdecore.de.Visitor
import io.github.kotlinmania.serdecore.ser.Serialize
import io.github.kotlinmania.serdecore.ser.Serializer
import io.github.kotlinmania.serdejson.Value
import io.github.kotlinmania.serdejson.fromStr
import kotlin.time.Instant

internal sealed class InvalidJsonCredentials(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {
    /** The response did not contain valid JSON */
    internal class JsonError(
        error: Throwable,
    ) : InvalidJsonCredentials("invalid JSON in response: ${error.message ?: error}", error)

    /** The response was missing a required field */
    internal class MissingField(
        val field: String,
    ) : InvalidJsonCredentials("Expected field `$field` in response but it was missing")

    /** A field was invalid */
    internal class InvalidField(
        val field: String,
        val err: Throwable,
    ) : InvalidJsonCredentials("Invalid field in response: `$field`. ${err.message ?: err}", err)

    /** Another unhandled error occurred */
    internal class Other(
        message: String,
    ) : InvalidJsonCredentials(message)

    fun fmt(): String = message ?: toString()

    companion object {
        fun from(err: Throwable): InvalidJsonCredentials = JsonError(err)
    }
}

internal data class RefreshableCredentials(
    val accessKeyId: String,
    val secretAccessKey: String,
    val sessionToken: String,
    val accountId: String?,
    val expiration: Instant,
) : Serialize {
    fun fmt(): String = toString()

    override fun toString(): String =
        "RefreshableCredentials(accessKeyId=$accessKeyId, " +
            "secretAccessKey=** redacted **, " +
            "sessionToken=** redacted **" +
            accountId?.let { ", accountId=$it" }.orEmpty() +
            ", expiration=$expiration)"

    override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
        serializer.serializeStruct("RefreshableCredentials", 5).flatMap { struct ->
            struct
                .serializeField(
                    "accessKeyId",
                    object : Serialize {
                        override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = serializer.serializeStr(accessKeyId)
                    },
                ).flatMap {
                    struct.serializeField(
                        "secretAccessKey",
                        object : Serialize {
                            override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = serializer.serializeStr("** redacted **")
                        },
                    )
                }.flatMap {
                    struct.serializeField(
                        "sessionToken",
                        object : Serialize {
                            override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = serializer.serializeStr("** redacted **")
                        },
                    )
                }.flatMap {
                    if (accountId != null) {
                        struct.serializeField(
                            "accountId",
                            object : Serialize {
                                override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = serializer.serializeStr(accountId)
                            },
                        )
                    } else {
                        SerdeResult.success(Unit)
                    }
                }.flatMap {
                    struct.serializeField(
                        "expiration",
                        object : Serialize {
                            override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = serializer.serializeStr(expiration.toString())
                        },
                    )
                }.flatMap { struct.end() }
        }

    companion object : Deserialize<RefreshableCredentials> {
        override fun <D : Deserializer> deserialize(deserializer: D): SerdeResult<RefreshableCredentials> =
            deserializer.deserializeMap(
                object : Visitor<RefreshableCredentials> {
                    override fun expecting(): String = "struct RefreshableCredentials"

                    override fun <A : MapAccess> visitMap(access: A): SerdeResult<RefreshableCredentials> {
                        var akid: String? = null
                        var secret: String? = null
                        var token: String? = null
                        var acct: String? = null
                        var exp: Instant? = null

                        while (true) {
                            val keyOutcome = access.nextKey(StringDeserialize)
                            if (keyOutcome is SerdeResult.Failure) return keyOutcome
                            val key = (keyOutcome as SerdeResult.Success).value ?: break
                            when {
                                key.equals("accessKeyId", ignoreCase = true) ||
                                    key.equals("AccessKeyId", ignoreCase = true) -> {
                                    val v = access.nextValue(StringDeserialize)
                                    if (v is SerdeResult.Failure) return v
                                    akid = (v as SerdeResult.Success).value
                                }

                                key.equals("secretAccessKey", ignoreCase = true) ||
                                    key.equals("SecretAccessKey", ignoreCase = true) -> {
                                    val v = access.nextValue(StringDeserialize)
                                    if (v is SerdeResult.Failure) return v
                                    secret = (v as SerdeResult.Success).value
                                }

                                key.equals("sessionToken", ignoreCase = true) ||
                                    key.equals("SessionToken", ignoreCase = true) ||
                                    key.equals("token", ignoreCase = true) ||
                                    key.equals("Token", ignoreCase = true) -> {
                                    val v = access.nextValue(StringDeserialize)
                                    if (v is SerdeResult.Failure) return v
                                    token = (v as SerdeResult.Success).value
                                }

                                key.equals("accountId", ignoreCase = true) ||
                                    key.equals("AccountId", ignoreCase = true) -> {
                                    val v = access.nextValue(StringDeserialize)
                                    if (v is SerdeResult.Failure) return v
                                    acct = (v as SerdeResult.Success).value
                                }

                                key.equals("expiration", ignoreCase = true) ||
                                    key.equals("Expiration", ignoreCase = true) ||
                                    key.equals("expiresAt", ignoreCase = true) ||
                                    key.equals("ExpiresAt", ignoreCase = true) -> {
                                    val v = access.nextValue(StringDeserialize)
                                    if (v is SerdeResult.Failure) return v
                                    val expStr = (v as SerdeResult.Success).value
                                    exp =
                                        try {
                                            Instant.parse(expStr)
                                        } catch (_: Exception) {
                                            null
                                        }
                                }

                                else -> {
                                    val ignored = access.nextValue(IgnoredAny)
                                    if (ignored is SerdeResult.Failure) return ignored
                                }
                            }
                        }

                        if (akid == null) return SerdeResult.failure(SerdeError.missingField("AccessKeyId"))
                        if (secret == null) return SerdeResult.failure(SerdeError.missingField("SecretAccessKey"))
                        if (token == null) return SerdeResult.failure(SerdeError.missingField("Token"))
                        if (exp == null) return SerdeResult.failure(SerdeError.missingField("Expiration"))

                        return SerdeResult.success(
                            RefreshableCredentials(
                                accessKeyId = akid,
                                secretAccessKey = secret,
                                sessionToken = token,
                                accountId = acct,
                                expiration = exp,
                            ),
                        )
                    }
                },
            )
    }
}

internal sealed class JsonCredentials : Serialize {
    data class RefreshableCredentials(
        val credentials: io.github.kotlinmania.awsconfig.RefreshableCredentials,
    ) : JsonCredentials() {
        override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
            credentials.serialize(serializer)
    }

    data class Error(
        val code: String,
        val message: String,
    ) : JsonCredentials() {
        override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> =
            serializer.serializeStruct("Error", 2).flatMap { struct ->
                struct
                    .serializeField(
                        "code",
                        object : Serialize {
                            override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = serializer.serializeStr(code)
                        },
                    ).flatMap {
                        struct.serializeField(
                            "message",
                            object : Serialize {
                                override fun <Ok> serialize(serializer: Serializer<Ok>): SerdeResult<Ok> = serializer.serializeStr(message)
                            },
                        )
                    }.flatMap { struct.end() }
            }
    }
}

/**
 * Deserialize an IMDS response from a string
 *
 * There are two levels of error here: the top level distinguishes between a successfully parsed
 * response from the credential provider vs. something invalid / unexpected. The inner error
 * distinguishes between a successful response that contains credentials vs. an error with a code and
 * error message.
 *
 * Keys are case insensitive.
 */
internal fun parseJsonCredentials(
    credentialsResponse: String,
): Result<JsonCredentials> {
    var code: String? = null
    var accessKeyId: String? = null
    var secretAccessKey: String? = null
    var sessionToken: String? = null
    var accountId: String? = null
    var expiration: String? = null
    var message: String? = null

    val parseLoopResult =
        jsonParseLoop(credentialsResponse.encodeToByteArray()) { key, value ->
            val stringValue =
                when (value) {
                    is Value.Str -> value.value
                    else -> null
                }
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

                (key.equals("Token", ignoreCase = true) || key.equals("SessionToken", ignoreCase = true)) && stringValue != null -> {
                    sessionToken = stringValue
                }

                key.equals("AccountId", ignoreCase = true) && stringValue != null -> {
                    accountId = stringValue
                }

                (key.equals("Expiration", ignoreCase = true) || key.equals("ExpiresAt", ignoreCase = true)) && stringValue != null -> {
                    expiration = stringValue
                }

                // Error case handling: message will be set
                key.equals("Message", ignoreCase = true) && stringValue != null -> {
                    message = stringValue
                }

                else -> {}
            }
            Result.success(Unit)
        }

    if (parseLoopResult.isFailure) {
        return Result.failure(parseLoopResult.exceptionOrNull()!!)
    }

    return when (val codeVal = code) {
        // IMDS does not appear to reply with a `Code` missing, but documentation indicates it
        // may be possible
        null, "Success" -> {
            val resolvedAccessKeyId = accessKeyId ?: return Result.failure(InvalidJsonCredentials.MissingField("AccessKeyId"))
            val resolvedSecretAccessKey = secretAccessKey ?: return Result.failure(InvalidJsonCredentials.MissingField("SecretAccessKey"))
            val resolvedSessionToken = sessionToken ?: return Result.failure(InvalidJsonCredentials.MissingField("Token"))
            val resolvedExpiration = expiration ?: return Result.failure(InvalidJsonCredentials.MissingField("Expiration"))
            val expirationInstant =
                try {
                    Instant.parse(resolvedExpiration)
                } catch (err: IllegalArgumentException) {
                    return Result.failure(InvalidJsonCredentials.InvalidField("Expiration", err))
                }
            Result.success(
                JsonCredentials.RefreshableCredentials(
                    RefreshableCredentials(
                        accessKeyId = resolvedAccessKeyId,
                        secretAccessKey = resolvedSecretAccessKey,
                        sessionToken = resolvedSessionToken,
                        accountId = accountId,
                        expiration = expirationInstant,
                    ),
                ),
            )
        }

        else -> {
            Result.success(
                JsonCredentials.Error(
                    code = codeVal,
                    message = message ?: "no message",
                ),
            )
        }
    }
}

internal fun jsonParseLoop(
    input: ByteArray,
    f: (String, Value) -> Result<Unit>,
): Result<Unit> {
    val jsonString = input.decodeToString()
    val outcome = fromStr(jsonString)
    if (outcome.isFailure) {
        return Result.failure(
            InvalidJsonCredentials.JsonError(
                Exception(outcome.exceptionOrNull()?.message ?: "JSON parse error"),
            ),
        )
    }
    val root = outcome.getOrThrow()
    if (root !is Value.Object) {
        return Result.failure(
            InvalidJsonCredentials.JsonError(
                IllegalArgumentException("expected a JSON document starting with `{`"),
            ),
        )
    }
    for ((key, value) in root.value) {
        val res = f(key, value)
        if (res.isFailure) {
            return res
        }
    }
    return Result.success(Unit)
}

internal fun jsonParseLoop(
    input: String,
    f: (String, Value) -> Result<Unit>,
): Result<Unit> = jsonParseLoop(input.encodeToByteArray(), f)
