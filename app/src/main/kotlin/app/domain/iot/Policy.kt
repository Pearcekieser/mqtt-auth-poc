package app.domain.iot

import app.domain.iot.Version.V2012_10_17
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonValue
import java.security.InvalidParameterException

// TODO add data sturcture to ser/des the Iot Policies
// Allows us to diff existing policy against our intended one to
// determine if we need to update.

enum class Version {
    @JsonProperty("2012-10-17")
    V2012_10_17
}

data class Policy(
    @JsonProperty("Version")
    val version: Version = V2012_10_17,
    @JsonProperty("Statement")
    val statements: List<Statement>,
)

data class Statement(
    @JsonProperty("Effect") val effect: Effect,
    @JsonProperty("Action") val action: List<Action>,
    @JsonProperty("Resource") val resource: List<Client>
)

enum class Effect {
    @JsonProperty("Allow") ALLOW,
    @JsonProperty("Deny") DENY
}

enum class Action {
    @JsonProperty("iot:Publish")
    PUBLISH,
    @JsonProperty("iot:Connect")
    CONNECT,
}

interface Resource

data class Client constructor(
    val clientExpression: String,
    val region: String = DEFAULT_REGION,
    val account: String = DEFUALT_ACCOUNT,
) : Resource {

    @JsonValue
    fun toArn() = "arn:aws:iot:$region:$account:client/$clientExpression"

    companion object {
        val DEFUALT_ACCOUNT = "454891305327"
        val DEFAULT_REGION = "us-west-2"
        fun parseArn(arn: String): Client {
            val regex = Regex("arn:aws:iot:(?<region>.*?):(?<account>\\d*?):client/(?<clientId>.*)")
            val matchResult = regex.matchEntire(arn) ?: throw InvalidParameterException("Unable to parse arn: $arn")
            val groups = matchResult.groups
            return Client(
                region = groups["region"]!!.value,
                account = groups["account"]!!.value,
                clientExpression = groups["clientId"]!!.value,
            )
        }
    }
}
