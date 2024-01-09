package app.gateway

import app.gateway.util.MqttMessageConsumer
import app.gateway.util.SampleUtil
import com.amazonaws.services.iot.client.AWSIotMessage
import com.amazonaws.services.iot.client.AWSIotMqttClient
import com.amazonaws.services.iot.client.AWSIotQos
import com.amazonaws.services.iot.client.auth.Credentials
import com.amazonaws.services.iot.client.auth.CredentialsProvider
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.onEach
import org.apache.logging.log4j.kotlin.Logging
import software.amazon.awssdk.services.iot.model.CreateKeysAndCertificateResponse
import software.amazon.awssdk.services.sts.StsClient
import software.amazon.awssdk.services.sts.model.GetSessionTokenRequest
import java.time.Duration
import kotlin.random.Random.Default.nextInt

class MqttGateway private constructor(val iotClient: AWSIotMqttClient) {
    companion object : Logging {
        const val ENDPOINT = "a40t4sa3r5q82-ats.iot.us-west-2.amazonaws.com"
        const val REGION = "us-west-2"

        fun getMqttClientFromCert(cert: CreateKeysAndCertificateResponse, clientId: String): AWSIotMqttClient {
            val certPemStream = cert.certificatePem().byteInputStream(Charsets.UTF_8)
            val privateKeyStream = cert.keyPair().privateKey().byteInputStream(Charsets.UTF_8)
            val kpp = SampleUtil.getKeyStorePasswordPair(certPemStream, privateKeyStream, null)

            return AWSIotMqttClient(
                ENDPOINT,
                clientId,
                kpp.keyStore,
                kpp.keyPassword
            )
        }

        private fun getAdminCreds(): software.amazon.awssdk.services.sts.model.Credentials {
            val stsClient = StsClient.create()
            val adminCredRequest = GetSessionTokenRequest.builder()
                .durationSeconds(900)
                .build()
            return stsClient.getSessionToken(adminCredRequest).credentials()
        }
    }

    constructor(credentialsProvider: CredentialsProvider, clientId: String) : this(
        AWSIotMqttClient(
            ENDPOINT,
            clientId,
            credentialsProvider,
            REGION
        )
    )

    constructor(creds: software.amazon.awssdk.services.sts.model.Credentials, clientId: String) : this(
        CredentialsProvider { Credentials(creds.accessKeyId(), creds.secretAccessKey(), creds.sessionToken()) },
        clientId
    )
    constructor(creds: software.amazon.awssdk.services.cognitoidentity.model.Credentials, clientId: String) : this(
        CredentialsProvider { Credentials(creds.accessKeyId(), creds.secretKey(), creds.sessionToken()) },
        clientId
    )

    constructor(certificate: CreateKeysAndCertificateResponse, clientId: String) : this(getMqttClientFromCert(certificate, clientId))
    constructor() : this(getAdminCreds(), "admin-${nextInt(0, 1000)}")

    val objectMapper: ObjectMapper = jacksonObjectMapper()

    init {
        iotClient.isCleanSession = true
        iotClient.maxConnectionRetries = 100
        iotClient.keepAliveInterval = Math.toIntExact(Duration.ofMinutes(5).toMillis())
        iotClient.numOfClientThreads = 10

        iotClient.connect(5000L)
    }

    fun publish(payload: String, topic: String) {
        logger.info("Publishing $payload topic: $topic")
        val message = AWSIotMessage(
            topic,
            AWSIotQos.QOS1,
            payload,
        )
        iotClient.publish(message)
    }

    fun subscribe(
        topic: String
    ): Flow<String> {
        logger.info("Subscribing to $topic")
        val iotTopic = MqttMessageConsumer(topic)
        iotClient.subscribe(iotTopic, 1000L)

        return iotTopic.subscriptionResults.consumeAsFlow()
            .onEach { logger.info("Received $it on $topic") }
    }
}
