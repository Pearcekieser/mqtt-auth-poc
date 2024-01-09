package app.domain

data class DeviceInfo(
    val username: String,
    val deviceName: String,
) {
    val thingName = "$username-$deviceName"

    constructor(userInfo: UserInfo, deviceName: String) : this(userInfo.username, deviceName)
}
