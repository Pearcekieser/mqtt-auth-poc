package app.domain

import software.amazon.awssdk.services.cognitoidentity.model.Credentials

data class AuthenticatedUser(
    val username: String,
    val password: String,
    val identityId: String,
    val idToken: String,
    val credentials: Credentials
)
