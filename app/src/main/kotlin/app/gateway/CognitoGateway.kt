package app.gateway

import app.domain.AuthenticatedUser
import app.domain.UserInfo
import org.apache.logging.log4j.kotlin.Logging
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.services.cognitoidentity.CognitoIdentityClient
import software.amazon.awssdk.services.cognitoidentity.model.DeleteIdentitiesRequest
import software.amazon.awssdk.services.cognitoidentity.model.GetCredentialsForIdentityRequest
import software.amazon.awssdk.services.cognitoidentity.model.GetCredentialsForIdentityResponse
import software.amazon.awssdk.services.cognitoidentity.model.GetIdRequest
import software.amazon.awssdk.services.cognitoidentity.model.ListIdentityPoolsRequest
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminCreateUserRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminDeleteUserRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminGetUserRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminInitiateAuthRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminRespondToAuthChallengeRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthenticationResultType
import software.amazon.awssdk.services.cognitoidentityprovider.model.ChallengeNameType
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUserPoolClientsRequest
import software.amazon.awssdk.services.cognitoidentityprovider.model.ListUserPoolsRequest

class CognitoGateway {
    // TODO split this into an CongitoUserPoolGateway, CongitoIdentityPoolGateway and IotGateway for low level operations
    // TODO move high level logic into a UserControler
    companion object : Logging

    val region = "us-west-2"
    private val credentialsProvider = DefaultCredentialsProvider.create()
    private val identityPoolClient = CognitoIdentityClient.builder()
        .credentialsProvider(credentialsProvider)
        .build()
    private val userPoolClient = CognitoIdentityProviderClient.builder()
        .credentialsProvider(credentialsProvider)
        .build()
    private val cognitoPoolId = getUserPoolId()
    private val identityPoolId = getIdentityPoolId()
    private val clientId = getClientId()

    private fun getUserPoolId(): String {
        val request = ListUserPoolsRequest.builder()
            .maxResults(10)
            .build()
        logger.info("request: $request")
        val response = userPoolClient.listUserPools(request)
        logger.info("response: $response")
        return response.userPools().first { it.name().contains("test42") }.id()
    }

    private fun getIdentityPoolId(): String {
        val request = ListIdentityPoolsRequest.builder()
            .maxResults(10)
            .build()
        val response = identityPoolClient.listIdentityPools(request)
        return response.identityPools().first { it.identityPoolName().contains("test42") }
            .identityPoolId()
    }

    private fun getClientId(): String {
        val request = ListUserPoolClientsRequest.builder()
            .userPoolId(cognitoPoolId)
            .maxResults(10)
            .build()
        val response = userPoolClient.listUserPoolClients(request)
        return response.userPoolClients().first { it.clientName().contains("test42") }.clientId()
    }

    fun createPoolUser(userInfo: UserInfo): Unit = userInfo.run {
        if (!userExists(username)) {
            createUser(username, password)
        }
    }

    fun signIn(userInfo: UserInfo): AuthenticatedUser = userInfo.run {
        val tokens = signIn(username, password)
        val creds = getCreds(tokens)

        return AuthenticatedUser(
            username,
            password,
            getIdentityId(tokens.idToken()),
            tokens.idToken(),
            creds.credentials(),
        )
    }

    fun deleteUser(user: AuthenticatedUser) {
        val identityId = getIdentityId(user.idToken)
        deleteIdentity(identityId)
        deletePoolUser(user.username)
    }

    private fun userExists(username: String): Boolean {
        val request = AdminGetUserRequest.builder()
            .userPoolId(cognitoPoolId)
            .username(username)
            .build()
        logger.info("request: $request")
        try {
            val response = userPoolClient.adminGetUser(request)
            logger.info("response: $response")
            return response.enabled()
        } catch (e: Exception) {
            logger.warn("Exception on getUser: ${e.message}")
            logger.warn(e.stackTrace)
            return false
        }
    }

    private fun createUser(username: String, password: String) {
        // create user in pool
        val request = AdminCreateUserRequest.builder()
            .userPoolId(cognitoPoolId)
            .username(username)
            .temporaryPassword(password)
            .build()
        logger.info("request: $request")
        val response = userPoolClient.adminCreateUser(request)
        logger.info("response: $response")
    }

    private fun deleteIdentity(identityId: String) {
        val request = DeleteIdentitiesRequest.builder()
            .identityIdsToDelete(identityId)
            .build()
        logger.info("request: $request")
        val response = identityPoolClient.deleteIdentities(request)
        logger.info("response: $response")
    }

    private fun deletePoolUser(username: String) {
        // todo delete unused identities
        val request = AdminDeleteUserRequest.builder()
            .userPoolId(cognitoPoolId)
            .username(username)
            .build()
        logger.info("request: $request")
        val response = userPoolClient.adminDeleteUser(request)
        logger.info("response: $response")
    }

    private fun signIn(username: String, password: String): AuthenticationResultType {
        // auth docs https://sdk.amazonaws.com/java/api/latest/software/amazon/awssdk/services/cognitoidentityprovider/model/AdminInitiateAuthRequest.html
        // https://docs.aws.amazon.com/cognito/latest/developerguide/amazon-cognito-user-pools-authentication-flow.html#amazon-cognito-user-pools-custom-authentication-flow

        val request = AdminInitiateAuthRequest.builder()
            .userPoolId(cognitoPoolId)
            .clientId(clientId)
            .authFlow("ADMIN_USER_PASSWORD_AUTH")
            .authParameters(
                mapOf(
                    "USERNAME" to username,
                    "PASSWORD" to password,
                    "DEVICE_KEY" to "cliPoc",
                )
            )
            .build()
        logger.info("request: $request")
        val response = userPoolClient.adminInitiateAuth(request)
        logger.info("response: $response")

        if (response.authenticationResult() != null) {
            return response.authenticationResult()
        }

        if (response.challengeName() == ChallengeNameType.NEW_PASSWORD_REQUIRED) {
            val request = AdminRespondToAuthChallengeRequest.builder()
                .userPoolId(cognitoPoolId)
                .clientId(clientId)
                .challengeName(ChallengeNameType.NEW_PASSWORD_REQUIRED)
                .challengeResponses(
                    mapOf(
                        "USERNAME" to username,
                        "NEW_PASSWORD" to password,
                    )
                )
                .session(response.session())
                .build()
            logger.info("request: $request")
            val response = userPoolClient.adminRespondToAuthChallenge(request)
            logger.info("response: $response")
            return response.authenticationResult()
        }

        throw NotImplementedError()
    }

    private fun getIdentityId(idToken: String): String {
        val response = identityPoolClient.getId(
            GetIdRequest.builder()
                .identityPoolId(identityPoolId)
                .accountId("454891305327") // TODO replace with NEW id
                .logins(
                    mapOf(
                        "cognito-idp.$region.amazonaws.com/${getUserPoolId()}" to idToken,
                    )
                )
                .build()
        )
        logger.info("response: $response")
        return response.identityId()
    }

    private fun getCreds(auth: AuthenticationResultType): GetCredentialsForIdentityResponse {
        // https://stackoverflow.com/questions/44244375/aws-iot-android-application-over-mqtt-throws-mqttexception-0-java-io-ioexcep/54898307#54898307
        val identId = getIdentityId(auth.idToken())
        val request = GetCredentialsForIdentityRequest.builder()
            .identityId(identId)
            .logins(
                mapOf(
                    "cognito-idp.$region.amazonaws.com/${getUserPoolId()}" to auth.idToken(),
                )
            )
            .build()
        logger.info("request: $request")
        val response = identityPoolClient.getCredentialsForIdentity(request)
        logger.info("response: $response")
        return response
    }
}
