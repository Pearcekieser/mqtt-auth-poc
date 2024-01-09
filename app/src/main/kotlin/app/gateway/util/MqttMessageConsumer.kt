package app.gateway.util

import com.amazonaws.services.iot.client.AWSIotMessage
import com.amazonaws.services.iot.client.AWSIotQos
import com.amazonaws.services.iot.client.AWSIotTopic
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.trySendBlocking
import org.apache.logging.log4j.kotlin.Logging

class MqttMessageConsumer(topic: String, qos: AWSIotQos = AWSIotQos.QOS1) : AWSIotTopic(topic, qos) {
    companion object : Logging

    val subscriptionResults = Channel<String>()

    override fun onMessage(message: AWSIotMessage) {
        val messagePayload = message.payload.toString(Charsets.UTF_8)
        logger.info("Received message $messagePayload on ${message.topic}")
        subscriptionResults.trySendBlocking(messagePayload)
    }
}
