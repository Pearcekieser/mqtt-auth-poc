package app.control

import app.domain.AuthenticatedUser
import app.domain.UserInfo
import app.gateway.CognitoGateway
import app.gateway.IotAuthControlGateway
import javax.inject.Inject

class UserControl @Inject constructor(
    val cognitoGateway: CognitoGateway,
    val iotAuth: IotAuthControlGateway,
) {
    fun createUser(userInfo: UserInfo) {
        cognitoGateway.createPoolUser(userInfo)
        iotAuth.createIotPolicy(userInfo)
    }

    fun signIn(userInfo: UserInfo): AuthenticatedUser {
        val authenticatedUser = cognitoGateway.signIn(userInfo)
        iotAuth.attachIotPolicy(authenticatedUser)
        return authenticatedUser
    }

    fun deleteUser(userInfo: UserInfo) = deleteUser(signIn(userInfo))
    fun deleteUser(auth: AuthenticatedUser) {
        iotAuth.detachAllPolicies(auth)
        iotAuth.deleteIotPolicy(auth)
        cognitoGateway.deleteUser(auth)
    }
}
