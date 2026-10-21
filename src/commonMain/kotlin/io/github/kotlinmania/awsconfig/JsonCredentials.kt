// port-lint: source src/json_credentials.rs
package io.github.kotlinmania.awsconfig

/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

import kotlin.time.Instant

internal sealed class InvalidJsonCredentials(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {
    /** The response did not contain valid JSON */
    internal class JsonError(
        error: Throwable,
    ) : InvalidJsonCredentials("invalid JSON in response: ${error.message ?: error}", error) {
        constructor(message: String) : this(Exception(message))
    }

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

    override fun toString(): String = fmt()

    companion object {
        fun from(error: Throwable): InvalidJsonCredentials =
            when (error) {
                is InvalidJsonCredentials -> error
                else -> JsonError(error)
            }
    }
}

internal data class RefreshableCredentials(
    val accessKeyId: String,
    val secretAccessKey: String,
    val sessionToken: String,
    val accountId: String? = null,
    val expiration: Instant,
) {
    override fun toString(): String {
        val accountPart = if (accountId != null) ", account_id: \"$accountId\"" else ""
        return "RefreshableCredentials { access_key_id: \"$accessKeyId\", secret_access_key: \"** redacted **\", session_token: \"** redacted **\"$accountPart, expiration: $expiration }"
    }
}

internal sealed class JsonCredentials {
    internal data class Refreshable(
        val credentials: RefreshableCredentials,
    ) : JsonCredentials()

    internal data class Error(
        val code: String,
        val message: String,
    ) : JsonCredentials()
}

internal sealed class Token {
    internal data object StartObject : Token()

    internal data object EndObject : Token()

    internal data class ObjectKey(
        val key: String,
    ) : Token()

    internal data class ValueString(
        val value: String,
    ) : Token()

    internal data class ValueNumber(
        val value: String,
    ) : Token()

    internal data class ValueBool(
        val value: Boolean,
    ) : Token()

    internal data object ValueNull : Token()

    internal data object StartArray : Token()

    internal data object EndArray : Token()
}

/**
 * Deserialize an IMDS response from a string or byte array.
 *
 * There are two levels of error here: the top level distinguishes between a successfully parsed
 * response from the credential provider vs. something invalid / unexpected. The inner error
 * distinguishes between a successful response that contains credentials vs. an error with a code and
 * error message.
 *
 * Keys are case insensitive.
 */
internal fun parseJsonCredentials(credentialsResponse: String): Result<JsonCredentials> =
    parseJsonCredentials(credentialsResponse.encodeToByteArray())

internal fun parseJsonCredentials(credentialsResponse: ByteArray): Result<JsonCredentials> {
    var code: String? = null
    var accessKeyId: String? = null
    var secretAccessKey: String? = null
    var sessionToken: String? = null
    var accountId: String? = null
    var expiration: String? = null
    var message: String? = null

    val parseResult =
        jsonParseLoop(credentialsResponse) { key, token ->
            if (token is Token.ValueString) {
                val value = token.value
                when {
                    key.equals("Code", ignoreCase = true) -> {
                        code = value
                    }

                    key.equals("AccessKeyId", ignoreCase = true) -> {
                        accessKeyId = value
                    }

                    key.equals("SecretAccessKey", ignoreCase = true) -> {
                        secretAccessKey = value
                    }

                    key.equals("Token", ignoreCase = true) || key.equals("SessionToken", ignoreCase = true) -> {
                        sessionToken = value
                    }

                    key.equals("AccountId", ignoreCase = true) -> {
                        accountId = value
                    }

                    key.equals("Expiration", ignoreCase = true) || key.equals("ExpiresAt", ignoreCase = true) -> {
                        expiration = value
                    }

                    key.equals("Message", ignoreCase = true) -> {
                        message = value
                    }
                }
            }
            Result.success(Unit)
        }

    if (parseResult.isFailure) {
        return Result.failure(parseResult.exceptionOrNull()!!)
    }

    return when {
        code == null || code.equals("Success", ignoreCase = true) -> {
            val resolvedAccessKeyId =
                accessKeyId
                    ?: return Result.failure(InvalidJsonCredentials.MissingField("AccessKeyId"))
            val resolvedSecretAccessKey =
                secretAccessKey
                    ?: return Result.failure(InvalidJsonCredentials.MissingField("SecretAccessKey"))
            val resolvedSessionToken =
                sessionToken
                    ?: return Result.failure(InvalidJsonCredentials.MissingField("Token"))
            val expirationStr =
                expiration
                    ?: return Result.failure(InvalidJsonCredentials.MissingField("Expiration"))

            val expirationInstant =
                try {
                    Instant.parse(expirationStr)
                } catch (e: Exception) {
                    return Result.failure(InvalidJsonCredentials.InvalidField("Expiration", e))
                }

            Result.success(
                JsonCredentials.Refreshable(
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
                    code = code,
                    message = message ?: "no message",
                ),
            )
        }
    }
}

internal fun jsonParseLoop(
    input: ByteArray,
    f: (String, Token) -> Result<Unit>,
): Result<Unit> {
    val text = input.decodeToString()
    var i = 0
    val n = text.length

    fun skipWhitespace() {
        while (i < n && text[i].isWhitespace()) {
            i++
        }
    }

    skipWhitespace()
    if (i >= n || text[i] != '{') {
        return Result.failure(
            InvalidJsonCredentials.JsonError(
                Exception("expected a JSON document starting with `{`"),
            ),
        )
    }
    i++ // consume '{'

    fun parseString(): String {
        val sb = StringBuilder()
        while (i < n) {
            val c = text[i++]
            if (c == '"') {
                return sb.toString()
            }
            if (c == '\\') {
                if (i >= n) throw IllegalArgumentException("unexpected end of escape in string")
                when (val esc = text[i++]) {
                    '"' -> {
                        sb.append('"')
                    }

                    '\\' -> {
                        sb.append('\\')
                    }

                    '/' -> {
                        sb.append('/')
                    }

                    'b' -> {
                        sb.append('\b')
                    }

                    'f' -> {
                        sb.append('\u000c')
                    }

                    'n' -> {
                        sb.append('\n')
                    }

                    'r' -> {
                        sb.append('\r')
                    }

                    't' -> {
                        sb.append('\t')
                    }

                    'u' -> {
                        if (i + 4 > n) throw IllegalArgumentException("invalid unicode escape")
                        val hex = text.substring(i, i + 4)
                        i += 4
                        sb.append(hex.toInt(16).toChar())
                    }

                    else -> {
                        throw IllegalArgumentException("unknown escape character: $esc")
                    }
                }
            } else {
                sb.append(c)
            }
        }
        throw IllegalArgumentException("unclosed string literal")
    }

    fun skipValue() {
        skipWhitespace()
        if (i >= n) throw IllegalArgumentException("unexpected end of JSON while skipping value")
        when (text[i]) {
            '{' -> {
                i++
                var depth = 1
                var inStr = false
                while (i < n && depth > 0) {
                    val ch = text[i++]
                    if (inStr) {
                        if (ch == '\\' && i < n) {
                            i++
                        } else if (ch == '"') {
                            inStr = false
                        }
                    } else {
                        if (ch == '"') {
                            inStr = true
                        } else if (ch == '{') {
                            depth++
                        } else if (ch == '}') {
                            depth--
                        }
                    }
                }
                if (depth > 0) throw IllegalArgumentException("unclosed nested object")
            }

            '[' -> {
                i++
                var depth = 1
                var inStr = false
                while (i < n && depth > 0) {
                    val ch = text[i++]
                    if (inStr) {
                        if (ch == '\\' && i < n) {
                            i++
                        } else if (ch == '"') {
                            inStr = false
                        }
                    } else {
                        if (ch == '"') {
                            inStr = true
                        } else if (ch == '[') {
                            depth++
                        } else if (ch == ']') {
                            depth--
                        }
                    }
                }
                if (depth > 0) throw IllegalArgumentException("unclosed nested array")
            }

            '"' -> {
                i++
                parseString()
            }

            else -> {
                while (i < n && text[i] != ',' && text[i] != '}' && text[i] != ']' && !text[i].isWhitespace()) {
                    i++
                }
            }
        }
    }

    try {
        skipWhitespace()
        if (i < n && text[i] == '}') {
            i++ // empty object
        } else {
            while (i < n) {
                skipWhitespace()
                if (i >= n) {
                    return Result.failure(
                        InvalidJsonCredentials.JsonError(Exception("unexpected end of JSON object")),
                    )
                }
                if (text[i] == '}') {
                    i++
                    break
                }
                if (text[i] != '"') {
                    return Result.failure(
                        InvalidJsonCredentials.Other("expected object key, found: ${text[i]}"),
                    )
                }
                i++ // consume '"'
                val key = parseString()
                skipWhitespace()
                if (i >= n || text[i] != ':') {
                    return Result.failure(
                        InvalidJsonCredentials.JsonError(Exception("expected ':' after object key")),
                    )
                }
                i++ // consume ':'
                skipWhitespace()
                if (i >= n) {
                    return Result.failure(
                        InvalidJsonCredentials.JsonError(Exception("unexpected end of JSON after ':'")),
                    )
                }
                val token =
                    when (text[i]) {
                        '"' -> {
                            i++
                            val strVal = parseString()
                            Token.ValueString(strVal)
                        }

                        '{' -> {
                            Token.StartObject
                        }

                        '[' -> {
                            Token.StartArray
                        }

                        't', 'f' -> {
                            val isTrue = text.startsWith("true", i)
                            val isFalse = text.startsWith("false", i)
                            if (!isTrue && !isFalse) throw IllegalArgumentException("invalid boolean")
                            Token.ValueBool(isTrue)
                        }

                        'n' -> {
                            if (!text.startsWith("null", i)) throw IllegalArgumentException("invalid null")
                            Token.ValueNull
                        }

                        else -> {
                            val sb = StringBuilder()
                            var j = i
                            while (j < n && (text[j].isDigit() || text[j] == '.' || text[j] == '-' || text[j] == '+' || text[j] == 'e' || text[j] == 'E')) {
                                sb.append(text[j++])
                            }
                            Token.ValueNumber(sb.toString())
                        }
                    }
                val callbackRes = f(key, token)
                if (callbackRes.isFailure) {
                    return callbackRes
                }
                if (token !is Token.ValueString) {
                    skipValue()
                }
                skipWhitespace()
                if (i < n && text[i] == ',') {
                    i++ // consume ','
                    skipWhitespace()
                } else if (i < n && text[i] == '}') {
                    i++ // consume '}'
                    break
                } else {
                    return Result.failure(
                        InvalidJsonCredentials.JsonError(Exception("expected ',' or '}'")),
                    )
                }
            }
        }

        skipWhitespace()
        if (i < n) {
            return Result.failure(
                InvalidJsonCredentials.Other("found more JSON tokens after completing parsing"),
            )
        }
        return Result.success(Unit)
    } catch (e: IllegalArgumentException) {
        return Result.failure(InvalidJsonCredentials.JsonError(e))
    }
}

internal fun jsonParseLoop(
    input: String,
    f: (String, Token) -> Result<Unit>,
): Result<Unit> = jsonParseLoop(input.encodeToByteArray(), f)
