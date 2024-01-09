package app.domain.iot

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class PolicyTest {
    val objectMapper = jacksonObjectMapper()

    @Test
    fun testSerilization() {
        val policy = Policy(
            statements = listOf(
                Statement(
                    effect = Effect.ALLOW,
                    action = listOf(Action.CONNECT),
                    resource = listOf(Client("client1"))
                )
            )
        )
        val policyString = objectMapper.writeValueAsString(policy)

        val expectedPolicy = """
            {
                "Version": "2012-10-17",
                "Statement": [
                  {
                    "Effect": "Allow",
                    "Action": [
                      "iot:Connect"
                    ],
                    "Resource": [
                      "arn:aws:iot:us-west-2:454891305327:client/client1"
                    ]
                  }
                ]
            }
        """.trimIndent()

        val policyJson = objectMapper.readTree(policyString)
        val expectedJson = objectMapper.readTree(expectedPolicy)
        assertThat(policyJson).isEqualTo(expectedJson)
    }
}
