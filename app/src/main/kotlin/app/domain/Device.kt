package app.domain

import software.amazon.awssdk.services.iot.model.CreateKeysAndCertificateResponse

data class Device(
    val certificate: CreateKeysAndCertificateResponse,
    val thingName: String,
)
