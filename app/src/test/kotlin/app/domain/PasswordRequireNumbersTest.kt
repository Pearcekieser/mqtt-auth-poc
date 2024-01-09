package app.domain

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.security.InvalidParameterException

internal class PasswordRequireNumbersTest {
    val checker = PasswordRequireNumbers()

    @Test
    fun verifyPasses() {
        val valid = "abc1"
        assertDoesNotThrow { checker.verify(valid) }
    }

    @Test
    fun verifyFails() {
        val invalid = "abc"
        assertThrows<InvalidParameterException> { checker.verify(invalid) }
    }
}
