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
                    key.eqIgnoreAsciiCase("Code") -> {
                        code = value
                    }

                    key.eqIgnoreAsciiCase("AccessKeyId") -> {
                        accessKeyId = value
                    }

                    key.eqIgnoreAsciiCase("SecretAccessKey") -> {
                        secretAccessKey = value
                    }

                    key.eqIgnoreAsciiCase("Token") || key.eqIgnoreAsciiCase("SessionToken") -> {
                        sessionToken = value
                    }

                    key.eqIgnoreAsciiCase("AccountId") -> {
                        accountId = value
                    }

                    key.eqIgnoreAsciiCase("Expiration") || key.eqIgnoreAsciiCase("ExpiresAt") -> {
                        expiration = value
                    }

                    key.eqIgnoreAsciiCase("Message") -> {
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
        code == null || code == "Success" -> {
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

private fun String.eqIgnoreAsciiCase(other: String): Boolean {
    if (this.length != other.length) return false
    for (i in indices) {
        val c1 = this[i]
        val c2 = other[i]
        if (c1 == c2) continue
        val lower1 = if (c1 in 'A'..'Z') c1 + 32 else c1
        val lower2 = if (c2 in 'A'..'Z') c2 + 32 else c2
        if (lower1 != lower2) return false
    }
    return true
}

private fun validateAndDecodeUtf8(bytes: ByteArray): String {
    var i = 0
    val n = bytes.size
    val sb = StringBuilder(n)
    while (i < n) {
        val b0 = bytes[i++].toInt() and 0xFF
        when {
            b0 < 0x80 -> {
                sb.append(b0.toChar())
            }

            b0 in 0xC2..0xDF -> {
                if (i >= n) throw IllegalArgumentException("truncated UTF-8 sequence")
                val b1 = bytes[i++].toInt() and 0xFF
                if (b1 !in 0x80..0xBF) throw IllegalArgumentException("invalid UTF-8 continuation byte")
                val cp = ((b0 and 0x1F) shl 6) or (b1 and 0x3F)
                sb.append(cp.toChar())
            }

            b0 in 0xE0..0xEF -> {
                if (i + 1 >= n) throw IllegalArgumentException("truncated UTF-8 sequence")
                val b1 = bytes[i++].toInt() and 0xFF
                val b2 = bytes[i++].toInt() and 0xFF
                if (b1 !in 0x80..0xBF || b2 !in 0x80..0xBF) throw IllegalArgumentException("invalid UTF-8 continuation byte")
                if (b0 == 0xE0 && b1 < 0xA0) throw IllegalArgumentException("overlong UTF-8 sequence")
                if (b0 == 0xED && b1 >= 0xA0) throw IllegalArgumentException("UTF-8 encoding of surrogate code point")
                val cp = ((b0 and 0x0F) shl 12) or ((b1 and 0x3F) shl 6) or (b2 and 0x3F)
                sb.append(cp.toChar())
            }

            b0 in 0xF0..0xF4 -> {
                if (i + 2 >= n) throw IllegalArgumentException("truncated UTF-8 sequence")
                val b1 = bytes[i++].toInt() and 0xFF
                val b2 = bytes[i++].toInt() and 0xFF
                val b3 = bytes[i++].toInt() and 0xFF
                if (b1 !in 0x80..0xBF || b2 !in 0x80..0xBF || b3 !in 0x80..0xBF) {
                    throw IllegalArgumentException("invalid UTF-8 continuation byte")
                }
                if (b0 == 0xF0 && b1 < 0x90) throw IllegalArgumentException("overlong UTF-8 sequence")
                if (b0 == 0xF4 && b1 > 0x8F) throw IllegalArgumentException("UTF-8 code point out of range")
                val cp = ((b0 and 0x07) shl 18) or ((b1 and 0x3F) shl 12) or ((b2 and 0x3F) shl 6) or (b3 and 0x3F)
                if (cp > 0x10FFFF) throw IllegalArgumentException("code point out of range")
                val high = (((cp - 0x10000) ushr 10) + 0xD800).toChar()
                val low = (((cp - 0x10000) and 0x3FF) + 0xDC00).toChar()
                sb.append(high).append(low)
            }

            else -> {
                throw IllegalArgumentException("invalid UTF-8 lead byte: $b0")
            }
        }
    }
    return sb.toString()
}

internal fun jsonParseLoop(
    input: ByteArray,
    f: (String, Token) -> Result<Unit>,
): Result<Unit> {
    val text: String
    try {
        text = validateAndDecodeUtf8(input)
    } catch (e: IllegalArgumentException) {
        return Result.failure(InvalidJsonCredentials.JsonError(e))
    }

    var i = 0
    val n = text.length

    fun skipWhitespace() {
        while (i < n && (text[i] == ' ' || text[i] == '\t' || text[i] == '\n' || text[i] == '\r')) {
            i++
        }
    }

    fun parseAsciiHex4(): Int {
        if (i + 4 > n) throw IllegalArgumentException("invalid unicode escape: truncated")
        var value = 0
        for (idx in 0 until 4) {
            val c = text[i++]
            val digit =
                when (c) {
                    in '0'..'9' -> c - '0'
                    in 'a'..'f' -> c - 'a' + 10
                    in 'A'..'F' -> c - 'A' + 10
                    else -> throw IllegalArgumentException("invalid hex character in unicode escape: '$c'")
                }
            value = (value shl 4) or digit
        }
        return value
    }

    fun parseString(): String {
        val sb = StringBuilder()
        while (i < n) {
            val c = text[i++]
            if (c == '"') {
                return sb.toString()
            }
            if (c.code < 0x20) {
                throw IllegalArgumentException("unescaped control character in string: ${c.code}")
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
                        val codeUnit = parseAsciiHex4()
                        if (codeUnit in 0xD800..0xDBFF) {
                            // High surrogate, must be followed by \uXXXX low surrogate
                            if (i + 6 > n || text[i] != '\\' || text[i + 1] != 'u') {
                                throw IllegalArgumentException("unpaired high surrogate")
                            }
                            i += 2 // skip \u
                            val lowUnit = parseAsciiHex4()
                            if (lowUnit !in 0xDC00..0xDFFF) {
                                throw IllegalArgumentException("invalid low surrogate code unit: $lowUnit")
                            }
                            sb.append(codeUnit.toChar())
                            sb.append(lowUnit.toChar())
                        } else if (codeUnit in 0xDC00..0xDFFF) {
                            throw IllegalArgumentException("unpaired low surrogate")
                        } else {
                            sb.append(codeUnit.toChar())
                        }
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

    fun parseNumber(): String {
        val start = i
        if (i < n && text[i] == '-') {
            i++
        }
        if (i >= n) throw IllegalArgumentException("expected digit after '-' in number")
        if (text[i] == '0') {
            i++
            if (i < n && text[i] in '0'..'9') {
                throw IllegalArgumentException("leading zeros are not permitted in JSON numbers")
            }
        } else if (text[i] in '1'..'9') {
            while (i < n && text[i] in '0'..'9') {
                i++
            }
        } else {
            throw IllegalArgumentException("expected digit in number, found: ${text[i]}")
        }
        if (i < n && text[i] == '.') {
            i++
            if (i >= n || text[i] !in '0'..'9') {
                throw IllegalArgumentException("expected digit after '.' in number")
            }
            while (i < n && text[i] in '0'..'9') {
                i++
            }
        }
        if (i < n && (text[i] == 'e' || text[i] == 'E')) {
            i++
            if (i < n && (text[i] == '+' || text[i] == '-')) {
                i++
            }
            if (i >= n || text[i] !in '0'..'9') {
                throw IllegalArgumentException("expected digit after exponent in number")
            }
            while (i < n && text[i] in '0'..'9') {
                i++
            }
        }
        return text.substring(start, i)
    }

    fun skipValue() {
        skipWhitespace()
        if (i >= n) throw IllegalArgumentException("unexpected end of JSON while parsing value")
        when (text[i]) {
            '"' -> {
                i++
                parseString()
            }

            '{' -> {
                i++ // consume '{'
                skipWhitespace()
                if (i < n && text[i] == '}') {
                    i++ // empty object
                } else {
                    while (true) {
                        skipWhitespace()
                        if (i >= n) throw IllegalArgumentException("unexpected end of JSON in nested object")
                        if (text[i] != '"') throw IllegalArgumentException("expected string key in nested object")
                        i++ // consume '"'
                        parseString()
                        skipWhitespace()
                        if (i >= n || text[i] != ':') throw IllegalArgumentException("expected ':' in nested object")
                        i++ // consume ':'
                        skipValue()
                        skipWhitespace()
                        if (i < n && text[i] == ',') {
                            i++
                            skipWhitespace()
                            if (i < n && text[i] == '}') {
                                throw IllegalArgumentException("trailing comma before '}' in nested object")
                            }
                        } else if (i < n && text[i] == '}') {
                            i++
                            break
                        } else {
                            throw IllegalArgumentException("expected ',' or '}' in nested object")
                        }
                    }
                }
            }

            '[' -> {
                i++ // consume '['
                skipWhitespace()
                if (i < n && text[i] == ']') {
                    i++ // empty array
                } else {
                    while (true) {
                        skipValue()
                        skipWhitespace()
                        if (i < n && text[i] == ',') {
                            i++
                            skipWhitespace()
                            if (i < n && text[i] == ']') {
                                throw IllegalArgumentException("trailing comma before ']' in nested array")
                            }
                        } else if (i < n && text[i] == ']') {
                            i++
                            break
                        } else {
                            throw IllegalArgumentException("expected ',' or ']' in nested array")
                        }
                    }
                }
            }

            't' -> {
                if (text.startsWith("true", i)) {
                    i += 4
                } else {
                    throw IllegalArgumentException("invalid literal starting with 't'")
                }
            }

            'f' -> {
                if (text.startsWith("false", i)) {
                    i += 5
                } else {
                    throw IllegalArgumentException("invalid literal starting with 'f'")
                }
            }

            'n' -> {
                if (text.startsWith("null", i)) {
                    i += 4
                } else {
                    throw IllegalArgumentException("invalid literal starting with 'n'")
                }
            }

            '-', in '0'..'9' -> {
                parseNumber()
            }

            else -> {
                throw IllegalArgumentException("unexpected character while parsing value: ${text[i]}")
            }
        }
    }

    try {
        skipWhitespace()
        if (i >= n || text[i] != '{') {
            return Result.failure(
                InvalidJsonCredentials.JsonError(
                    Exception("expected a JSON document starting with `{`"),
                ),
            )
        }
        i++ // consume '{'
        skipWhitespace()
        if (i < n && text[i] == '}') {
            i++ // empty object
        } else {
            while (true) {
                skipWhitespace()
                if (i >= n) {
                    return Result.failure(
                        InvalidJsonCredentials.JsonError(Exception("unexpected end of JSON object")),
                    )
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

                        't' -> {
                            if (!text.startsWith("true", i)) throw IllegalArgumentException("invalid boolean")
                            i += 4
                            Token.ValueBool(true)
                        }

                        'f' -> {
                            if (!text.startsWith("false", i)) throw IllegalArgumentException("invalid boolean")
                            i += 5
                            Token.ValueBool(false)
                        }

                        'n' -> {
                            if (!text.startsWith("null", i)) throw IllegalArgumentException("invalid null")
                            i += 4
                            Token.ValueNull
                        }

                        '-', in '0'..'9' -> {
                            val numStr = parseNumber()
                            Token.ValueNumber(numStr)
                        }

                        else -> {
                            throw IllegalArgumentException("unexpected character starting value: ${text[i]}")
                        }
                    }
                val callbackRes = f(key, token)
                if (callbackRes.isFailure) {
                    return callbackRes
                }
                if (token is Token.StartObject || token is Token.StartArray) {
                    skipValue()
                }
                skipWhitespace()
                if (i < n && text[i] == ',') {
                    i++ // consume ','
                    skipWhitespace()
                    if (i < n && text[i] == '}') {
                        return Result.failure(
                            InvalidJsonCredentials.JsonError(Exception("trailing comma before '}'")),
                        )
                    }
                    if (i >= n) {
                        return Result.failure(
                            InvalidJsonCredentials.JsonError(Exception("unexpected end of JSON after ','")),
                        )
                    }
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
