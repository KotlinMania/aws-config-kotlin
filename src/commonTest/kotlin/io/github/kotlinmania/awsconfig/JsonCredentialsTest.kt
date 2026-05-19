// port-lint: source json_credentials.rs
package io.github.kotlinmania.awsconfig

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Instant

class JsonCredentialsTest {
    @Test
    fun jsonCredentialsSuccessResponse() {
        val response = """
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
            JsonCredentials.RefreshableCredentials(
                RefreshableCredentials(
                    accessKeyId = "ASIARTEST",
                    secretAccessKey = "xjtest",
                    sessionToken = "IQote///test",
                    accountId = "111122223333",
                    expiration = Instant.fromEpochSeconds(1_631_935_916),
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
        val response = """
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
        val parsed = parseJsonCredentials(response).getOrThrow()
        assertEquals(
            JsonCredentials.RefreshableCredentials(
                RefreshableCredentials(
                    accessKeyId = "ASIARTEST",
                    secretAccessKey = "xjtest",
                    sessionToken = "IQote///test",
                    accountId = "111122223333",
                    expiration = Instant.fromEpochSeconds(1_631_935_916),
                ),
            ),
            parsed,
        )
    }

    @Test
    fun jsonCredentialsRequiredSessionToken() {
        val response = """
        {
            "LastUpdated" : "2021-09-17T20:57:08Z",
            "Type" : "AWS-HMAC",
            "AccessKeyId" : "ASIARTEST",
            "SecretAccessKey" : "xjtest",
            "AccountID" : "111122223333",
            "Expiration" : "2021-09-18T03:31:56Z"
        }
        """.trimIndent()
        val parsed = parseJsonCredentials(response).exceptionOrNull()
        assertEquals("Expected field `Token` in response but it was missing", parsed?.message)
    }

    @Test
    fun jsonCredentialsMissingAkid() {
        val response = """
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
        val error = parseJsonCredentials(response).exceptionOrNull()
        assertIs<InvalidJsonCredentials.MissingField>(error)
    }

    @Test
    fun jsonCredentialsErrorResponse() {
        val response = """
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

    /** Validate the specific JSON response format sent by ECS. */
    @Test
    fun jsonCredentialsEcs() {
        // Identical, but extra `RoleArn` field is present.
        val response = """
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
        assertIs<JsonCredentials.RefreshableCredentials>(parsed)
        assertTrue(parsed.credentials.sessionToken.startsWith("token"))
        assertEquals("111122223333", parsed.credentials.accountId)
        assertEquals(Instant.fromEpochSeconds(1_234_567_890), parsed.credentials.expiration)
    }

    @Test
    fun caseInsensitiveCodeParsing() {
        val response = """
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
}
