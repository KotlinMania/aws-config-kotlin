// port-lint: tests src/json_credentials.rs
package io.github.kotlinmania.awsconfig

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Instant

class JsonCredentialsTest {
    @Test
    fun jsonCredentialsSuccessResponse() {
        val response =
            """
            {
              "Code" : "Success",
              "LastUpdated" : "2021-09-17T20:57:08Z",
              "Type" : "AWS-HMAC",
              "AccessKeyId" : "ASIARTEST",
              "SecretAccessKey" : "xjtest",
              "Token" : "IQote///test",
              "AccountID" : "111122223333",
              "Expiration" : "2021-09-18T03:31:56Z"
            }
            """.trimIndent()
        val parsed = parseJsonCredentials(response).getOrThrow()
        assertEquals(
            JsonCredentials.Refreshable(
                RefreshableCredentials(
                    accessKeyId = "ASIARTEST",
                    secretAccessKey = "xjtest",
                    sessionToken = "IQote///test",
                    accountId = "111122223333",
                    expiration = Instant.fromEpochSeconds(1631935916),
                ),
            ),
            parsed,
        )
    }

    @Test
    fun jsonCredentialsInvalidJson() {
        val error = parseJsonCredentials("404: not found").exceptionOrNull()
        assertIs<InvalidJsonCredentials.JsonError>(error)
    }

    @Test
    fun jsonCredentialsNotJsonObject() {
        val error = parseJsonCredentials("[1,2,3]").exceptionOrNull()
        assertIs<InvalidJsonCredentials.JsonError>(error)
    }

    @Test
    fun jsonCredentialsMissingCode() {
        val resp =
            """
            {
                "LastUpdated" : "2021-09-17T20:57:08Z",
                "Type" : "AWS-HMAC",
                "AccessKeyId" : "ASIARTEST",
                "SecretAccessKey" : "xjtest",
                "Token" : "IQote///test",
                "AccountID" : "111122223333",
                "Expiration" : "2021-09-18T03:31:56Z"
            }
            """.trimIndent()
        val parsed = parseJsonCredentials(resp).getOrThrow()
        assertEquals(
            JsonCredentials.Refreshable(
                RefreshableCredentials(
                    accessKeyId = "ASIARTEST",
                    secretAccessKey = "xjtest",
                    sessionToken = "IQote///test",
                    accountId = "111122223333",
                    expiration = Instant.fromEpochSeconds(1631935916),
                ),
            ),
            parsed,
        )
    }

    @Test
    fun jsonCredentialsRequiredSessionToken() {
        val resp =
            """
            {
                "LastUpdated" : "2021-09-17T20:57:08Z",
                "Type" : "AWS-HMAC",
                "AccessKeyId" : "ASIARTEST",
                "SecretAccessKey" : "xjtest",
                "AccountID" : "111122223333",
                "Expiration" : "2021-09-18T03:31:56Z"
            }
            """.trimIndent()
        val parsed = parseJsonCredentials(resp).exceptionOrNull()
        assertIs<InvalidJsonCredentials.MissingField>(parsed)
        assertEquals(
            "Expected field `Token` in response but it was missing",
            parsed.message,
        )
    }

    @Test
    fun jsonCredentialsMissingAkid() {
        val resp =
            """
            {
                "Code": "Success",
                "LastUpdated" : "2021-09-17T20:57:08Z",
                "Type" : "AWS-HMAC",
                "SecretAccessKey" : "xjtest",
                "Token" : "IQote///test",
                "AccountID" : "111122223333",
                "Expiration" : "2021-09-18T03:31:56Z"
            }
            """.trimIndent()
        val error = parseJsonCredentials(resp).exceptionOrNull()
        assertIs<InvalidJsonCredentials.MissingField>(error)
        assertEquals("AccessKeyId", error.field)
    }

    @Test
    fun jsonCredentialsErrorResponse() {
        val response =
            """
            {
              "Code" : "AssumeRoleUnauthorizedAccess",
              "Message" : "EC2 cannot assume the role integration-test.",
              "LastUpdated" : "2021-09-17T20:46:56Z"
            }
            """.trimIndent()
        val parsed = parseJsonCredentials(response).getOrThrow()
        assertEquals(
            JsonCredentials.Error(
                code = "AssumeRoleUnauthorizedAccess",
                message = "EC2 cannot assume the role integration-test.",
            ),
            parsed,
        )
    }

    @Test
    fun jsonCredentialsEcs() {
        val response =
            """
            {
                "RoleArn":"arn:aws:iam::123456789:role/ecs-task-role",
                "AccessKeyId":"ASIARTEST",
                "SecretAccessKey":"SECRETTEST",
                "Token":"tokenEaCXVzLXdlc3QtMiJGMEQCIHt47W18eF4dYfSlmKGiwuJnqmIS3LMXNYfODBCEhcnaAiAnuhGOpcdIDxin4QFzhtgaCR2MpcVqR8NFJdMgOt0/xyrnAwhhEAEaDDEzNDA5NTA2NTg1NiIM9M9GT+c5UfV/8r7PKsQDUa9xE9Eprz5N+jgxbFSD2aJR2iyXCcP9Q1cOh4fdZhyw2WNmq9XnIa2tkzrreiQ5R2t+kzergJHO1KRZPfesarfJ879aWJCSocsEKh7xXwwzTsVXrNo5eWkpwTh64q+Ksz15eoaBhtrvnGvPx6SmXv7SToi/DTHFafJlT/T9jITACZvZXSE9zfLka26Rna3rI4g0ugowha//j1f/c1XuKloqshpZvMKc561om9Y5fqBv1fRiS2KhetGTcmz3wUqNQAk8Dq9oINS7cCtdIO0atqCK69UaKeJ9uKY8mzY9dFWw2IrkpOoXmA9r955iU0NOz/95jVJiPZ/8aE8vb0t67gQfzBUCfky+mGSGWAfPRXQlFa5AEulCTHPd7IcTVCtasG033oKEKgB8QnTxvM2LaPlwaaHo7MHGYXeUKbn9NRKd8m1ShwmAlr4oKp1vQp6cPHDTsdTfPTzh/ZAjUPs+ljQbAwqXbPQdUUPpOk0vltY8k6Im9EA0pf80iUNoqrixpmPsR2hzI/ybUwdh+QhvCSBx+J8KHqF6X92u4qAVYIxLy/LGZKT9YC6Kr9Gywn+Ro+EK/xl3axHPzNpbjRDJnbW3HrMw5LmmiwY6pgGWgmD6IOq4QYUtu1uhaLQZyoI5o5PWn+d3kqqxifu8D0ykldB3lQGdlJ2rjKJjCdx8fce1SoXao9cc4hiwn39hUPuTqzVwv2zbzCKmNggIpXP6gqyRtUCakf6tI7ZwqTb2S8KF3t4ElIP8i4cPdNoI0JHSC+sT4LDPpUcX1CjGxfvo55mBHJedW3LXve8TRj4UckFXT1gLuTnzqPMrC5AHz4TAt+uv",
                "AccountID" : "111122223333",
                "Expiration" : "2009-02-13T23:31:30Z"
            }
            """.trimIndent()
        val parsed = parseJsonCredentials(response).getOrThrow()
        assertIs<JsonCredentials.Refreshable>(parsed)
        assertEquals("ASIARTEST", parsed.credentials.accessKeyId)
        assertEquals("SECRETTEST", parsed.credentials.secretAccessKey)
        assertTrue(parsed.credentials.sessionToken.startsWith("token"))
        assertEquals("111122223333", parsed.credentials.accountId)
        assertEquals(Instant.fromEpochSeconds(1234567890), parsed.credentials.expiration)
    }

    @Test
    fun caseInsensitiveCodeParsing() {
        val response =
            """
            {
              "code" : "AssumeRoleUnauthorizedAccess",
              "message" : "EC2 cannot assume the role integration-test."
            }
            """.trimIndent()
        val parsed = parseJsonCredentials(response).getOrThrow()
        assertEquals(
            JsonCredentials.Error(
                code = "AssumeRoleUnauthorizedAccess",
                message = "EC2 cannot assume the role integration-test.",
            ),
            parsed,
        )
    }

    @Test
    fun aliasesAndCaseSensitivity() {
        val response =
            """
            {
              "accesskeyid": "MY_KEY",
              "secretaccesskey": "MY_SECRET",
              "sessiontoken": "MY_TOKEN",
              "accountid": "000111222333",
              "expiresat": "2024-01-01T00:00:00Z"
            }
            """.trimIndent()
        val parsed = parseJsonCredentials(response).getOrThrow()
        assertIs<JsonCredentials.Refreshable>(parsed)
        assertEquals("MY_KEY", parsed.credentials.accessKeyId)
        assertEquals("MY_SECRET", parsed.credentials.secretAccessKey)
        assertEquals("MY_TOKEN", parsed.credentials.sessionToken)
        assertEquals("000111222333", parsed.credentials.accountId)
        assertEquals(Instant.parse("2024-01-01T00:00:00Z"), parsed.credentials.expiration)
    }

    @Test
    fun unknownAndNestedValuesIgnored() {
        val response =
            """
            {
              "Code": "Success",
              "AccessKeyId": "AKIA123",
              "SecretAccessKey": "SECRET123",
              "Token": "TOKEN123",
              "Expiration": "2024-01-01T00:00:00Z",
              "NestedObject": {
                "key1": "val1",
                "nestedList": [1, 2, {"inner": "val"}]
              },
              "ArrayField": ["item1", "item2"]
            }
            """.trimIndent()
        val parsed = parseJsonCredentials(response).getOrThrow()
        assertIs<JsonCredentials.Refreshable>(parsed)
        assertEquals("AKIA123", parsed.credentials.accessKeyId)
    }

    @Test
    fun duplicateKeysOverwriteOrder() {
        val response =
            """
            {
              "AccessKeyId": "INITIAL_KEY",
              "AccessKeyId": "OVERWRITTEN_KEY",
              "SecretAccessKey": "SECRET",
              "Token": "TOKEN",
              "Expiration": "2024-01-01T00:00:00Z"
            }
            """.trimIndent()
        val parsed = parseJsonCredentials(response).getOrThrow()
        assertIs<JsonCredentials.Refreshable>(parsed)
        assertEquals("OVERWRITTEN_KEY", parsed.credentials.accessKeyId)
    }

    @Test
    fun trailingTokensRejected() {
        val response =
            """
            {
              "AccessKeyId": "KEY",
              "SecretAccessKey": "SECRET",
              "Token": "TOKEN",
              "Expiration": "2024-01-01T00:00:00Z"
            } trailing
            """.trimIndent()
        val error = parseJsonCredentials(response).exceptionOrNull()
        assertIs<InvalidJsonCredentials.Other>(error)
        assertTrue(error.message?.contains("found more JSON tokens after completing parsing") == true)
    }

    @Test
    fun invalidExpirationDateFormat() {
        val response =
            """
            {
              "AccessKeyId": "KEY",
              "SecretAccessKey": "SECRET",
              "Token": "TOKEN",
              "Expiration": "not-a-valid-date"
            }
            """.trimIndent()
        val error = parseJsonCredentials(response).exceptionOrNull()
        assertIs<InvalidJsonCredentials.InvalidField>(error)
        assertEquals("Expiration", error.field)
    }

    @Test
    fun debugRedactionContract() {
        val creds =
            RefreshableCredentials(
                accessKeyId = "AKIAEXAMPLE",
                secretAccessKey = "SUPER_SECRET_KEY_NOT_LEAKED",
                sessionToken = "SESSION_TOKEN_NOT_LEAKED",
                accountId = "123456789012",
                expiration = Instant.parse("2024-01-01T00:00:00Z"),
            )
        val debugString = creds.toString()
        assertTrue(debugString.contains("access_key_id: \"AKIAEXAMPLE\""))
        assertTrue(debugString.contains("secret_access_key: \"** redacted **\""))
        assertTrue(debugString.contains("session_token: \"** redacted **\""))
        assertTrue(!debugString.contains("SUPER_SECRET_KEY_NOT_LEAKED"))
        assertTrue(!debugString.contains("SESSION_TOKEN_NOT_LEAKED"))
        assertTrue(debugString.contains("account_id: \"123456789012\""))
    }

    @Test
    fun caseSensitiveCodeValue() {
        val response =
            """
            {
              "Code": "success",
              "Message": "lowercase success is not an ok status"
            }
            """.trimIndent()
        val parsed = parseJsonCredentials(response).getOrThrow()
        assertEquals(
            JsonCredentials.Error(
                code = "success",
                message = "lowercase success is not an ok status",
            ),
            parsed,
        )
    }

    @Test
    fun trailingCommaRejected() {
        val trailingCommaObject = """{"Code": "Failure",}"""
        val err1 = parseJsonCredentials(trailingCommaObject).exceptionOrNull()
        assertIs<InvalidJsonCredentials.JsonError>(err1)

        val trailingCommaAtEof = """{"Code": "Failure","""
        val err2 = parseJsonCredentials(trailingCommaAtEof).exceptionOrNull()
        assertIs<InvalidJsonCredentials.JsonError>(err2)

        val trailingCommaNested =
            """
            {
              "Code": "Success",
              "AccessKeyId": "K",
              "SecretAccessKey": "S",
              "Token": "T",
              "Expiration": "2024-01-01T00:00:00Z",
              "nested": {"key": "val",}
            }
            """.trimIndent()
        val err3 = parseJsonCredentials(trailingCommaNested).exceptionOrNull()
        assertIs<InvalidJsonCredentials.JsonError>(err3)

        val trailingCommaArray =
            """
            {
              "Code": "Success",
              "AccessKeyId": "K",
              "SecretAccessKey": "S",
              "Token": "T",
              "Expiration": "2024-01-01T00:00:00Z",
              "items": [1, 2,]
            }
            """.trimIndent()
        val err4 = parseJsonCredentials(trailingCommaArray).exceptionOrNull()
        assertIs<InvalidJsonCredentials.JsonError>(err4)
    }

    @Test
    fun invalidLiteralsAndNumbersRejected() {
        val invalidLiteral = """{"Code": "Failure", "extra": nonsense}"""
        assertIs<InvalidJsonCredentials.JsonError>(parseJsonCredentials(invalidLiteral).exceptionOrNull())

        val leadingZeroNumber = """{"Code": "Failure", "num": 01}"""
        assertIs<InvalidJsonCredentials.JsonError>(parseJsonCredentials(leadingZeroNumber).exceptionOrNull())

        val leadingPlusNumber = """{"Code": "Failure", "num": +1}"""
        assertIs<InvalidJsonCredentials.JsonError>(parseJsonCredentials(leadingPlusNumber).exceptionOrNull())

        val loneTrailingDotNumber = """{"Code": "Failure", "num": 1.}"""
        assertIs<InvalidJsonCredentials.JsonError>(parseJsonCredentials(loneTrailingDotNumber).exceptionOrNull())

        val noLeadingDigitFraction = """{"Code": "Failure", "num": .5}"""
        assertIs<InvalidJsonCredentials.JsonError>(parseJsonCredentials(noLeadingDigitFraction).exceptionOrNull())
    }

    @Test
    fun strictUtf8AndSurrogatesValidation() {
        val invalidUtf8Bytes = byteArrayOf('{'.code.toByte(), '"'.code.toByte(), 0xC2.toByte(), '}'.code.toByte())
        val utf8Err = jsonParseLoop(invalidUtf8Bytes) { _, _ -> Result.success(Unit) }.exceptionOrNull()
        assertIs<InvalidJsonCredentials.JsonError>(utf8Err)

        val unescapedControlChar = "{\"Code\": \"Fail\u0000ure\"}"
        val ctrlErr = parseJsonCredentials(unescapedControlChar).exceptionOrNull()
        assertIs<InvalidJsonCredentials.JsonError>(ctrlErr)

        val loneSurrogate = "{\"Code\": \"\\uD800\"}"
        val surrogateErr = parseJsonCredentials(loneSurrogate).exceptionOrNull()
        assertIs<InvalidJsonCredentials.JsonError>(surrogateErr)

        val validSurrogatePair = "{\"Code\": \"\\uD83D\\uDE00\", \"Message\": \"smile\"}"
        val parsed = parseJsonCredentials(validSurrogatePair).getOrThrow()
        assertIs<JsonCredentials.Error>(parsed)
        assertEquals("\uD83D\uDE00", parsed.code)
    }

    @Test
    fun unicodeDigitsInNumbersRejected() {
        val nonAsciiInteger = "{\"Code\": \"Failure\", \"extra\": 1\u0661}"
        assertIs<InvalidJsonCredentials.JsonError>(parseJsonCredentials(nonAsciiInteger).exceptionOrNull())

        val nonAsciiFraction = "{\"Code\": \"Failure\", \"extra\": 1.\u0661}"
        assertIs<InvalidJsonCredentials.JsonError>(parseJsonCredentials(nonAsciiFraction).exceptionOrNull())

        val nonAsciiExponent = "{\"Code\": \"Failure\", \"extra\": 1e\u0661}"
        assertIs<InvalidJsonCredentials.JsonError>(parseJsonCredentials(nonAsciiExponent).exceptionOrNull())
    }

    @Test
    fun unicodeEscapesSignedAndNonAsciiHexRejected() {
        val signedPlusEscape = "{\"Code\": \"\\u+041\"}"
        assertIs<InvalidJsonCredentials.JsonError>(parseJsonCredentials(signedPlusEscape).exceptionOrNull())

        val signedMinusEscape = "{\"Code\": \"\\u-001\"}"
        assertIs<InvalidJsonCredentials.JsonError>(parseJsonCredentials(signedMinusEscape).exceptionOrNull())

        val nonAsciiHexEscape = "{\"Code\": \"\\u\u0661000\"}"
        assertIs<InvalidJsonCredentials.JsonError>(parseJsonCredentials(nonAsciiHexEscape).exceptionOrNull())

        val invalidHexCharEscape = "{\"Code\": \"\\u004G\"}"
        assertIs<InvalidJsonCredentials.JsonError>(parseJsonCredentials(invalidHexCharEscape).exceptionOrNull())
    }

    @Test
    fun asciiOnlyKeyComparison() {
        val validAsciiLowercase =
            """
            {
              "accesskeyid": "AKIAVALID",
              "secretaccesskey": "SECRETDATA",
              "token": "TOKENDATA",
              "expiration": "2024-01-01T00:00:00Z"
            }
            """.trimIndent()
        val parsedValid = parseJsonCredentials(validAsciiLowercase).getOrThrow()
        assertIs<JsonCredentials.Refreshable>(parsedValid)
        assertEquals("AKIAVALID", parsedValid.credentials.accessKeyId)

        val kelvinSignKey =
            """
            {
              "Access\u212AeyId": "AKIAKELVIN",
              "SecretAccessKey": "SECRETDATA",
              "Token": "TOKENDATA",
              "Expiration": "2024-01-01T00:00:00Z"
            }
            """.trimIndent()
        val errKelvin = parseJsonCredentials(kelvinSignKey).exceptionOrNull()
        assertIs<InvalidJsonCredentials.MissingField>(errKelvin)
        assertEquals("AccessKeyId", errKelvin.field)
    }
}
