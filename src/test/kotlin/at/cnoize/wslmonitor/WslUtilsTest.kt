package at.cnoize.wslmonitor

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class WslUtilsTest {

    @Test
    fun testFormatPackageList_withEmptyInput() {
        val result = WslUtils.formatPackageList("")
        assertEquals("No packages found.", result)
    }

    @Test
    fun testFormatPackageList_withValidInput() {
        val input = """
            Listing...
            libc6/stable-security [upgradable from 2.31-13+deb11u5 to 2.31-13+deb11u6]
            libssl1.1/stable-security [upgradable from 1.1.1n-0+deb11u4 to 1.1.1n-0+deb11u5]
        """.trimIndent()
        
        val expected = """
            • libc6: 2.31-13+deb11u5 to 2.31-13+deb11u6
            • libssl1.1: 1.1.1n-0+deb11u4 to 1.1.1n-0+deb11u5
        """.trimIndent()
        
        val result = WslUtils.formatPackageList(input)
        assertEquals(expected, result)
    }

    @Test
    fun testFormatPackageList_withNoMatchingPackages() {
        val input = """
            Listing...
            All packages are up to date.
        """.trimIndent()
        
        val result = WslUtils.formatPackageList(input)
        assertEquals("No packages found.", result)
    }
}