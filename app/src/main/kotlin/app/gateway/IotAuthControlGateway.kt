package app.gateway

import app.domain.AuthenticatedUser
import app.domain.UserInfo
import org.apache.logging.log4j.kotlin.Logging
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.iot.IotClient
import software.amazon.awssdk.services.iot.model.AttachPolicyRequest
import software.amazon.awssdk.services.iot.model.CreatePolicyRequest
import software.amazon.awssdk.services.iot.model.DeletePolicyRequest
import software.amazon.awssdk.services.iot.model.DetachPolicyRequest
import software.amazon.awssdk.services.iot.model.ListAttachedPoliciesRequest
import software.amazon.awssdk.services.iot.model.ResourceAlreadyExistsException

class IotAuthControlGateway {
    companion object : Logging
    val region = "us-west-2"
    private val credentialsProvider = DefaultCredentialsProvider.create()

    val iotClient = IotClient.builder()
        .credentialsProvider(credentialsProvider)
        .region(Region.of(region))
        .build()

    fun createIotPolicy(userInfo: UserInfo) {
        val request = CreatePolicyRequest.builder()
            .policyName(userInfo.username)
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
                                "arn:aws:iot:us-west-2:454891305327:client/${userInfo.username}"
                            ]
                        },
                        {
                            "Effect": "Allow",
                            "Action": [
                                "iot:Publish"
                            ],
                            "Resource": [
                                "arn:aws:iot:us-west-2:454891305327:topic/${userInfo.username}/*"
                            ]
                        },
                        {
                            "Effect": "Allow",
                            "Action": [
                                "iot:Subscribe"
                            ],
                            "Resource": [
                                "arn:aws:iot:us-west-2:454891305327:topicfilter/${userInfo.username}/*"
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
    }

    fun deleteIotPolicy(authenticatedUser: AuthenticatedUser) {
        val request = DeletePolicyRequest.builder()
            .policyName(authenticatedUser.username)
            .build()
        logger.info(request)
        val response = iotClient.deletePolicy(request)
        logger.info(response)
    }

    fun attachIotPolicy(authenticatedUser: AuthenticatedUser) {
        val request = AttachPolicyRequest.builder()
            .target(authenticatedUser.identityId)
            .policyName(authenticatedUser.username)
            .build()
        logger.info(request)
        val response = iotClient.attachPolicy(request)
        logger.info(response)
    }

    fun detachAllPolicies(authenticatedUser: AuthenticatedUser) {
        val request = ListAttachedPoliciesRequest.builder()
            .target(authenticatedUser.identityId)
            .build()
        val result = iotClient.listAttachedPolicies(request)
        result.policies().forEach { policy ->
            val request = DetachPolicyRequest.builder()
                .policyName(policy.policyName())
                .target(authenticatedUser.identityId)
                .build()
            val result = iotClient.detachPolicy(request)
        }
    }
}
