package at.cnoize.wslmonitor

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Integration test for the WslMonitor class.
 *
 * This test verifies that the WslMonitor can properly execute WSL commands
 * and handle the output, including commands with spaces.
 */
internal class WslMonitorTest {
    @TempDir
    lateinit var tempDir: Path

    /**
     * Helper method to capture console output.
     */
    private fun captureSystemOut(block: () -> Unit): String {
        val originalOut = System.out
        val outContent = ByteArrayOutputStream()
        System.setOut(PrintStream(outContent))

        try {
            block()
            return outContent.toString()
        } finally {
            System.setOut(originalOut)
        }
    }

    /**
     * Integration test that verifies the WslMonitor main method runs correctly.
     * This test checks that:
     * 1. The main method executes the expected WSL commands
     * 2. The output contains the expected command execution messages
     * 3. The output contains the completion message
     */
    @Test
    fun testWslMonitorIntegration() {
        // Override the output file location to use our temp directory
        System.setProperty("user.home", tempDir.toString())

        // Capture the console output to verify it contains the expected messages
        val consoleOutput = captureSystemOut {
            // Run the main method with no arguments (uses default distribution)
            WslMonitor.main(emptyArray())
        }

        // Verify the console output contains the expected command execution messages
        assertTrue(consoleOutput.contains("Executing: wsl -e sudo -n apt update"))
        assertTrue(consoleOutput.contains("Executing: wsl -e sudo -n apt list --upgradable"))

        // Verify the console output contains the completion message
        assertTrue(consoleOutput.contains("WSL update check completed."))

        // Check the content of the output file
        val outputFile = tempDir.resolve(".wsl-monitor")
        assertTrue(Files.exists(outputFile), "Output file should exist")

        // Read the content of the file
        val fileContent = outputFile.toFile().readText()

        // Check if it contains "WSL Update Check"
        assertTrue(fileContent.contains("WSL Update Check"), 
            "Output file should contain 'WSL Update Check'")

        // Check if it contains today's date in ISO format (yyyy-MM-dd)
        val todayDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        assertTrue(
            fileContent.contains(todayDate),
            "Output file should contain today's date in ISO format: $todayDate"
        )
    }
}
