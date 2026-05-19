// port-lint: source default_provider/auth_scheme_preference.rs
package io.github.kotlinmania.awsconfig.defaultprovider.authschemepreference

/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

internal object Env {
    internal const val AUTH_SCHEME_PREFERENCE: String = "AWS_AUTH_SCHEME_PREFERENCE"
}

internal object ProfileKey {
    internal const val AUTH_SCHEME_PREFERENCE: String = "auth_scheme_preference"
}

internal data class AuthSchemeId(
    val value: String,
) {
    internal companion object {
        internal fun from(value: String): AuthSchemeId = AuthSchemeId(value)
    }
}

internal data class AuthSchemePreference(
    val schemes: List<AuthSchemeId>,
) {
    internal companion object {
        internal fun from(schemes: Iterable<AuthSchemeId>): AuthSchemePreference =
            AuthSchemePreference(schemes.toList())
    }
}

internal fun parseAuthSchemeNames(csv: String): Result<AuthSchemePreference> {
    val schemes = mutableListOf<AuthSchemeId>()
    for (segment in csv.split(',')) {
        val trimmed = segment.trim().replace(" ", "").replace("\t", "")
        if (trimmed.isEmpty()) {
            return Result.failure(
                InvalidAuthSchemeNamesCsv(
                    "Empty name found in `$csv`.",
                ),
            )
        }
        val schemeName = trimmed.substringAfterLast('#')
        schemes += AuthSchemeId.from(schemeName)
    }
    return Result.success(AuthSchemePreference.from(schemes))
}

internal class InvalidAuthSchemeNamesCsv(
    private val value: String,
) : Exception("Not a valid comma-separated auth scheme names: $value")
