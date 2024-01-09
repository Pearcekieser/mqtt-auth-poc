package app.domain

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.security.InvalidParameterException

internal class PasswordRequireLowerTest {
    val checker = PasswordRequireLower()

    @Test
    fun verifyPasses() {
        val valid = listOf("a", "c", "z")

        assertDoesNotThrow { valid.forEach { checker.verify(it) } }
    }

    @Test
    fun verifyFails() {
        val invalid = listOf("A", "C", "Z")
        invalid.forEach {
            assertThrows<InvalidParameterException> { checker.verify(it) }
        }
    }
}
