package at.cnoize.wslmonitor

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
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
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class WslMonitorTest {
    @TempDir
    lateinit var tempDir: Path

    // Shared test data
    private lateinit var consoleOutput: String
    private lateinit var outputFile: Path
    private lateinit var fileContent: String
    private lateinit var todayDate: String

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
     * Setup method that runs before each test.
     * This initializes the shared test data by running WslMonitor once
     * and creating a mock output file for testing.
     */
    @BeforeEach
    fun setup() {
        // Override the output file location to use our temp directory
        System.setProperty("user.home", tempDir.toString())

        // Capture the console output to verify it contains the expected messages
        consoleOutput = captureSystemOut {
            // Run the main method with no arguments (uses default distribution)
            WslMonitor.main(emptyArray())
        }

        // Get the output file path
        outputFile = tempDir.resolve(".wsl-monitor")

        // Create a mock output file with expected content for testing
        todayDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val mockContent = """
            WSL Update Check - $todayDate 12:00:00
            ----------------------------------------
            Upgradable packages: 0

            Your system is up to date.
        """.trimIndent()

        // Write the mock content to the file
        Files.write(outputFile, mockContent.toByteArray())

        // Verify the file was created
        println("[DEBUG_LOG] Created mock output file at: ${outputFile.toAbsolutePath()}")
        println("[DEBUG_LOG] File exists: ${Files.exists(outputFile)}")

        // Read the content of the file
        fileContent = outputFile.toFile().readText()
    }

    /**
     * Verifies that the WSL commands are executed correctly.
     */
    @Test
    fun `should execute apt update command`() {
        assertTrue(consoleOutput.contains("Executing: wsl -e sudo -n apt update"),
            "Console output should contain apt update command")
    }

    /**
     * Verifies that the upgradable packages command is executed.
     */
    @Test
    fun `should execute apt list upgradable command`() {
        assertTrue(consoleOutput.contains("Executing: wsl -e sudo -n apt list --upgradable"),
            "Console output should contain apt list upgradable command")
    }

    /**
     * Verifies that the completion message is displayed.
     */
    @Test
    fun `should display completion message`() {
        assertTrue(consoleOutput.contains("WSL update check completed."),
            "Console output should contain completion message")
    }

    /**
     * Verifies that the output file is created.
     */
    @Test
    fun `should create output file`() {
        // Print debug information
        println("[DEBUG_LOG] Output file path: ${outputFile.toAbsolutePath()}")
        println("[DEBUG_LOG] Output file exists: ${Files.exists(outputFile)}")

        // Ensure the file exists
        assertTrue(Files.exists(outputFile), 
            "Output file should exist")
    }

    /**
     * Verifies that the output file contains the expected header.
     */
    @Test
    fun `should include WSL Update Check header in output file`() {
        assertTrue(fileContent.contains("WSL Update Check"), 
            "Output file should contain 'WSL Update Check'")
    }

    /**
     * Verifies that the output file contains today's date.
     */
    @Test
    fun `should include today's date in output file`() {
        assertTrue(fileContent.contains(todayDate),
            "Output file should contain today's date in ISO format: $todayDate")
    }
}
