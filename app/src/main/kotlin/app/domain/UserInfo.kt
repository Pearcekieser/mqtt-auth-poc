package app.domain

import java.security.InvalidParameterException

data class UserInfo(
    val username: String,
    val password: String,
) {
    companion object {
        val PASSWORD_CHECKS = listOf(
            PasswordLengthCheck(8),
            PasswordRequireNumbers(),
            PasswordSpecialCharacter(),
            PasswordRequireUpper(),
            PasswordRequireLower(),
        )
    }
    init {
        PASSWORD_CHECKS.forEach { checker -> checker.verify(password) }
    }
}

interface StringCheck { fun verify(str: String) }

class PasswordLengthCheck(private val length: Int) : StringCheck {
    override fun verify(str: String) {
        if (str.length >= length) return

        throw InvalidParameterException("Password too short")
    }
}

class PasswordRequireNumbers : StringCheck {
    override fun verify(str: String) {
        for (c in '0'..'9') {
            if (str.contains(c)) return
        }
        throw InvalidParameterException("Password needs number")
    }
}

class PasswordSpecialCharacter : StringCheck {
    override fun verify(str: String) {
        if (str.contains('!')) return
        throw InvalidParameterException("Password needs '!'")
    }
}

class PasswordRequireUpper : StringCheck {
    override fun verify(str: String) {
        for (c in 'A'..'Z') {
            if (str.contains(c)) return
        }
        throw InvalidParameterException("Password needs upper")
    }
}

class PasswordRequireLower : StringCheck {
    override fun verify(str: String) {
        for (c in 'a'..'z') {
            if (str.contains(c)) return
        }
        throw InvalidParameterException("Password needs lower")
    }
}
