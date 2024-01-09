package app.domain

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.security.InvalidParameterException

internal class PasswordSpecialCharacterTest {
    val checker = PasswordSpecialCharacter()

    @Test
    fun verifyPasses() {
        val valid = "!"
        assertDoesNotThrow { checker.verify(valid) }
    }

    @Test
    fun verifyFails() {
        val invalid = "a"
        assertThrows<InvalidParameterException> { checker.verify(invalid) }
    }
}
