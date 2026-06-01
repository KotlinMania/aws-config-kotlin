// port-lint: source default_provider/auth_scheme_preference.rs
package io.github.kotlinmania.awsconfig.defaultprovider.authschemepreference

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AuthSchemePreferenceTest {
    @Test
    fun logErrorOnInvalidValue() {
        val error = parseAuthSchemeNames("scheme1, , \tscheme2").exceptionOrNull()
        assertTrue(
            error.toString().contains(
                "Not a valid comma-separated auth scheme names: Empty name found",
            ),
        )
    }

    @Test
    fun parsesFullyQualifiedAndTrimmedNames() {
        val parsed =
            parseAuthSchemeNames(
                "aws.auth#sigv4, smithy.api#httpBasicAuth, smithy.api#httpDigestAuth, " +
                    "\thttpBearerAuth \t, httpApiKeyAuth ",
            ).getOrThrow()
        assertEquals(
            listOf("sigv4", "httpBasicAuth", "httpDigestAuth", "httpBearerAuth", "httpApiKeyAuth"),
            parsed.schemes.map { it.value },
        )
    }
}
