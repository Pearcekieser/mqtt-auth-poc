package app.domain

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.security.InvalidParameterException

internal class PasswordRequireUpperTest {
    val checker = PasswordRequireUpper()

    @Test
    fun verifyPasses() {
        val valid = listOf("A", "C", "Z")
        assertDoesNotThrow { valid.forEach { checker.verify(it) } }
    }

    @Test
    fun verifyFails() {
        val invalid = listOf("a", "c", "z")
        invalid.forEach {
            assertThrows<InvalidParameterException> { checker.verify(it) }
        }
    }
}
