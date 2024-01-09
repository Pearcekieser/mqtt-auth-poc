package integ

import app.control.UserControl
import app.domain.AuthenticatedUser
import app.domain.UserInfo
import app.gateway.CognitoGateway
import app.gateway.IotAuthControlGateway
import app.gateway.MqttGateway
import com.amazonaws.services.iot.client.AWSIotTimeoutException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.time.delay
import kotlinx.coroutines.time.withTimeoutOrNull
import org.apache.logging.log4j.kotlin.Logging
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.assertThrows
import java.time.Duration

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class UserActionTest {
    companion object : Logging
    val userControl = UserControl(CognitoGateway(), IotAuthControlGateway())

    val user1 = UserInfo(
        username = "user1",
        password = "User1234!"
    )
    lateinit var user1Auth: AuthenticatedUser
    lateinit var userMqttGateway: MqttGateway

    @BeforeAll
    fun setup() {
        logger.info("Setup")
        userControl.createUser(user1)
        user1Auth = userControl.signIn(user1)
    }

    @AfterAll
    fun teardown() {
        logger.info("Teardown")
        userControl.deleteUser(user1Auth)
    }

    val adminMqttGateway = MqttGateway()
    val testTopic = "${user1.username}/test"
    val invalidTopic = "invalidTopic"
    val payload = "test"

    @Test @Order(1)
    fun `GIVEN user1 WHEN clientId has id THEN can connect`() {
        val auth = userControl.signIn(user1)
        val clientId = auth.username
        userMqttGateway = MqttGateway(auth.credentials, clientId)
    }

    @Test @Order(1)
    fun `GIVEN user1 WHEN clientId not has id THEN cannot connect`() {
        val auth = userControl.signIn(user1)
        val clientId = "invalidClientId"
        assertThrows<AWSIotTimeoutException> {
            // assert MqttException (0) - java.io.IOException: Already connected
            userMqttGateway = MqttGateway(auth.credentials, clientId)
        }
    }

    @Test @Order(2)
    fun `GIVEN user1 WHEN topic starts with id THEN can publish`() = runBlocking {
        val testSubscription = subscribeWithDelay(adminMqttGateway, testTopic)

        userMqttGateway.publish(payload, testTopic)

        val message = readSubscriptionWithTimeoutOrNull(testSubscription)
        assertThat(message).isEqualTo(payload)
    }

    @Test @Order(2)
    fun `GIVEN user1 WHEN topic NOT starts with id THEN cannot publish`() = runBlocking {
        val testSubscription = subscribeWithDelay(adminMqttGateway, invalidTopic)

        userMqttGateway.publish(payload, invalidTopic)

        val message = readSubscriptionWithTimeoutOrNull(testSubscription)
        assertThat(message).isNull()
    }

    @Test @Order(2)
    fun `GIVEN user1 WHEN topic starts with id THEN can subscribe`() = runBlocking {
        val userSubscription = subscribeWithDelay(userMqttGateway, testTopic)

        adminMqttGateway.publish(payload, testTopic)

        val message = readSubscriptionWithTimeoutOrNull(userSubscription)
        assertThat(message).isEqualTo(payload)
    }

    @Test @Order(2)
    fun `GIVEN user1 WHEN topic NOT starts with id THEN cannot subscribe`() = runBlocking {
        val userSubscription = subscribeWithDelay(userMqttGateway, invalidTopic)

        adminMqttGateway.publish(payload, invalidTopic)

        val message = readSubscriptionWithTimeoutOrNull(userSubscription)
        assertThat(message).isNull()
    }

    private suspend fun subscribeWithDelay(
        mqttGateway: MqttGateway,
        topic: String,
        delay: Duration = Duration.ofMillis(500L)
    ): Flow<String> {
        val userSubscription = mqttGateway.subscribe(topic)
        delay(delay)
        return userSubscription
    }

    private suspend fun readSubscriptionWithTimeoutOrNull(
        subscription: Flow<String>,
        timeout: Duration = Duration.ofSeconds(5L)
    ): String? {
        return withTimeoutOrNull(timeout) { subscription.first() }
    }
}
