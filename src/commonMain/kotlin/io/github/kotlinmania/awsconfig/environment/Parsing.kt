// port-lint: source environment/mod.rs
package io.github.kotlinmania.awsconfig.environment

internal class InvalidBooleanValue(
    private val value: String,
) : Throwable("$value is not a valid boolean") {
    fun fmt(): String = message ?: "$value is not a valid boolean"

    override fun toString(): String = fmt()
}

internal fun parseBool(value: String): Result<Boolean> =
    when {
        value.equals("false", ignoreCase = true) -> Result.success(false)
        value.equals("true", ignoreCase = true) -> Result.success(true)
        else -> Result.failure(InvalidBooleanValue(value))
    }

internal class InvalidUintValue(
    private val value: String,
) : Throwable("$value is not a valid u32") {
    fun fmt(): String = message ?: "$value is not a valid u32"

    override fun toString(): String = fmt()
}

internal fun parseUint(value: String): Result<UInt> =
    value.toUIntOrNull()
        ?.let { Result.success(it) }
        ?: Result.failure(InvalidUintValue(value))

internal class InvalidUrlValue(
    private val value: String,
) : Throwable("$value is not a valid URL") {
    fun fmt(): String = message ?: "$value is not a valid URL"

    override fun toString(): String = fmt()
}

internal fun parseUrl(value: String): Result<String> {
    val schemeEnd = value.indexOf(':')
    if (schemeEnd <= 0) {
        return Result.failure(InvalidUrlValue(value))
    }

    val scheme = value.substring(0, schemeEnd)
    val hasValidScheme = scheme.first().isLetter() &&
        scheme.all { it.isLetterOrDigit() || it == '+' || it == '-' || it == '.' }
    if (!hasValidScheme) {
        return Result.failure(InvalidUrlValue(value))
    }

    val remainder = value.substring(schemeEnd + 1)
    if (remainder.isEmpty()) {
        return Result.failure(InvalidUrlValue(value))
    }

    val lowerScheme = scheme.lowercase()
    if (lowerScheme == "http" || lowerScheme == "https") {
        if (!remainder.startsWith("//")) {
            return Result.failure(InvalidUrlValue(value))
        }
        val authority = remainder.drop(2).takeWhile { it != '/' && it != '?' && it != '#' }
        if (authority.isEmpty() || authority.startsWith("@")) {
            return Result.failure(InvalidUrlValue(value))
        }
    }

    // We discard the parse result because it includes a trailing slash.
    return Result.success(value)
}
