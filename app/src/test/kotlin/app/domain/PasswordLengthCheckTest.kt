package app.domain

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.security.InvalidParameterException

class PasswordLengthCheckTest {
    val len = 5
    val checker = PasswordLengthCheck(len)
    @Test
    fun verifyPasses() {
        val valid = "12345"
        assertDoesNotThrow { checker.verify(valid) }
    }

    @Test
    fun verifyFails() {
        val invalid = "1234"
        assertThrows<InvalidParameterException> { checker.verify(invalid) }
    }
}
