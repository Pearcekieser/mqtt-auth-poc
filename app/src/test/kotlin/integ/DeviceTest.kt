package integ

import app.control.UserControl
import app.domain.Device
import app.domain.DeviceInfo
import app.domain.UserInfo
import app.gateway.CognitoGateway
import app.gateway.IotAuthControlGateway
import app.gateway.IotControlGateway
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
class DeviceTest {
    companion object : Logging
    val userControl = UserControl(CognitoGateway(), IotAuthControlGateway())
    val deviceControl = IotControlGateway()
    val adminMqttGateway = MqttGateway()

    val user1 = UserInfo(username = "user1", password = "User1234!")
    val device1 = DeviceInfo(user1, "device1")

    val testTopic = "${device1.username}/${device1.deviceName}/test"
    val invalidTopic = "invalidTopic"
    val payload = "test"

    lateinit var device: Device
    lateinit var deviceMqttGateway: MqttGateway

    @BeforeAll
    fun setup() {
        logger.info("setup")
        device = deviceControl.createDevice(device1)
    }

    @AfterAll
    fun teardown() {
        logger.info("teardown")
        deviceControl.deleteDevice(device)
    }

    @Test @Order(1)
    fun `GIVEN user1-device1 WHEN clientId has id THEN can connect`() {
        deviceMqttGateway = MqttGateway(device.certificate, device1.thingName)
    }

    @Test @Order(1)
    fun `GIVEN user1-device1 WHEN clientId not has id THEN can not connect`() {
        val invalidClientId = "invalidClientId"
        assertThrows<AWSIotTimeoutException> {
            deviceMqttGateway = MqttGateway(device.certificate, invalidClientId)
        }
    }

    @Test @Order(2)
    fun `GIVEN user1-device1 WHEN topic starts with id THEN can publish`() = runBlocking {
        val testSubscription = subscribeWithDelay(adminMqttGateway, testTopic)

        deviceMqttGateway.publish(payload, testTopic)

        val message = readSubscriptionWithTimeoutOrNull(testSubscription)
        assertThat(message).isEqualTo(payload)
    }

    @Test @Order(2)
    fun `GIVEN user1-device1 WHEN invalid topic THEN can not publish`() = runBlocking {
        val testSubscription = subscribeWithDelay(adminMqttGateway, invalidTopic)

        deviceMqttGateway.publish(payload, testTopic)

        val message = readSubscriptionWithTimeoutOrNull(testSubscription)
        assertThat(message).isNull()
    }

    @Test @Order(2)
    fun `GIVEN user1-device1 WHEN topic starts with id THEN can subscribe`() = runBlocking {
        val userSubscription = subscribeWithDelay(deviceMqttGateway, testTopic)

        adminMqttGateway.publish(payload, testTopic)

        val message = readSubscriptionWithTimeoutOrNull(userSubscription)
        assertThat(message).isEqualTo(payload)
    }

    @Test @Order(2)
    fun `GIVEN user1-device1 WHEN topic NOT starts with id THEN cannot subscribe`() = runBlocking {
        val userSubscription = subscribeWithDelay(deviceMqttGateway, invalidTopic)

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
