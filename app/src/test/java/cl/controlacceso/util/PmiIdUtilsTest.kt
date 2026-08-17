package cl.controlacceso.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PmiIdUtilsTest {

    @Test
    fun acceptsOnlyDigits() {
        assertEquals("1234567", PmiIdUtils.normalize("1234567"))
        assertTrue(PmiIdUtils.isValid("1234567"))
    }

    @Test
    fun rejectsLetters() {
        assertNull(PmiIdUtils.normalize("11260A87"))
        assertFalse(PmiIdUtils.isValid("11260A87"))
    }

    @Test
    fun rejectsEmptyAndBlank() {
        assertNull(PmiIdUtils.normalize(""))
        assertNull(PmiIdUtils.normalize("   "))
        assertNull(PmiIdUtils.normalize(null))
    }

    @Test
    fun rejectsHyphenAndDots() {
        assertNull(PmiIdUtils.normalize("11.260.187"))
        assertNull(PmiIdUtils.normalize("1234567-5"))
    }

    @Test
    fun preservesLeadingZerosAsText() {
        assertEquals("00123", PmiIdUtils.normalize("00123"))
    }
}
