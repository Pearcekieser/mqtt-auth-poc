package app.gateway

import app.domain.Device
import app.domain.DeviceInfo
import org.apache.logging.log4j.kotlin.Logging
import software.amazon.awssdk.services.iot.IotClient
import software.amazon.awssdk.services.iot.model.AttachPolicyRequest
import software.amazon.awssdk.services.iot.model.CertificateStatus
import software.amazon.awssdk.services.iot.model.CreatePolicyRequest
import software.amazon.awssdk.services.iot.model.DeleteCertificateRequest
import software.amazon.awssdk.services.iot.model.DetachPolicyRequest
import software.amazon.awssdk.services.iot.model.ResourceAlreadyExistsException
import software.amazon.awssdk.services.iot.model.UpdateCertificateRequest

class IotControlGateway {
    val region = "us-west-2"
    val awsAccount = "454891305327"
    val iotClient = IotClient.create()

    companion object : Logging

    fun createDevice(deviceInfo: DeviceInfo): Device {
        // register cert to device from: https://stackoverflow.com/questions/47202668/creating-aws-iot-things-with-policies-and-certificates-on-the-fly

        // create certificate
        val certificateResponse = iotClient.createKeysAndCertificate()

        // create policy
        val request = CreatePolicyRequest.builder()
            .policyName(deviceInfo.thingName)
            .policyDocument(
                """
                {
                    "Version": "2012-10-17",
                    "Statement": [
                        {
                            "Effect": "Allow",
                            "Action": [
                                "iot:Connect"
                            ],
                            "Resource": [
                                "arn:aws:iot:$region:$awsAccount:client/${deviceInfo.thingName}"
                            ]
                        },
                        {
                            "Effect": "Allow",
                            "Action": [
                                "iot:Publish"
                            ],
                            "Resource": [
                                "arn:aws:iot:$region:$awsAccount:topic/${deviceInfo.username}/${deviceInfo.deviceName}/*"
                            ]
                        },
                        {
                            "Effect": "Allow",
                            "Action": [
                                "iot:Subscribe"
                            ],
                            "Resource": [
                                "arn:aws:iot:$region:$awsAccount:topicfilter/${deviceInfo.username}/${deviceInfo.deviceName}/*"
                            ]
                        }
                    ]
                }
                """.trimIndent()
            )
            .build()
        logger.info(request)
        try {
            val response = iotClient.createPolicy(request)
            logger.info(response)
        } catch (e: ResourceAlreadyExistsException) {
            // TODO compare intended vs already existing policy and update if needed
            logger.info("Policy already exists: ${request.policyName()}")
        }

        // Assign IoT Policy to certificate
        val iotPolicyToCertificateRequest = AttachPolicyRequest.builder()
            .policyName(deviceInfo.thingName)
            .target(certificateResponse.certificateArn())
            .build()
        iotClient.attachPolicy(iotPolicyToCertificateRequest)

        // activate the cert
        val activateCertRequest = UpdateCertificateRequest.builder()
            .certificateId(certificateResponse.certificateId())
            .newStatus(CertificateStatus.ACTIVE)
            .build()
        iotClient.updateCertificate(activateCertRequest)

//        // create the iot thing
//        val createThingRequest = CreateThingRequest.builder()
//            .thingName(deviceInfo.thingName)
//            .build()
//        val createThingResponse = iotClient.createThing(createThingRequest)
//
//        // attach cert to thing
//        val attachCertToThingRequest = AttachThingPrincipalRequest.builder()
//            .thingName(createThingResponse.thingName())
//            .principal(certificateResponse.certificateArn())
//            .build()
//        iotClient.attachThingPrincipal(attachCertToThingRequest)

        return Device(
            certificateResponse,
            deviceInfo.thingName
        )
    }

    fun deleteDevice(device: Device): Unit = device.run {
        // https://stackoverflow.com/questions/36003491/how-to-delete-aws-iot-things-and-policies
        val certificateId = device.certificate.certificateId()

        val detachPolicyRequest = DetachPolicyRequest.builder()
            .policyName(thingName)
            .target(certificate.certificateArn())
            .build()
        iotClient.detachPolicy(detachPolicyRequest)

//        val detachThingPrincipalRequest = DetachThingPrincipalRequest.builder()
//            .thingName(thingName)
//            .principal(certificate.certificateArn())
//            .build()
//        iotClient.detachThingPrincipal(detachThingPrincipalRequest)
//
//        val deleteThingRequest = DeleteThingRequest.builder()
//            .thingName(device.thingName)
//            .build()
//        iotClient.deleteThing(deleteThingRequest)

        val activateCertRequest = UpdateCertificateRequest.builder()
            .certificateId(certificateId)
            .newStatus(CertificateStatus.REVOKED)
            .build()
        iotClient.updateCertificate(activateCertRequest)

        val deleteCertificateRequest = DeleteCertificateRequest.builder()
            .certificateId(certificateId)
            .build()
        iotClient.deleteCertificate(deleteCertificateRequest)
    }
}
