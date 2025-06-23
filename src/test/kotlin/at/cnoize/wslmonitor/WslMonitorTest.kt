package at.cnoize.wslmonitor

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
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
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class WslMonitorTest {
    // Shared test data
    private lateinit var consoleOutput: String
    private lateinit var outputFile: Path
    private lateinit var fileContent: String
    private lateinit var todayDate: String

    /**
     * Captures the standard output during the execution of the provided code block.
     * This is a utility function for testing console output.
     *
     * @param block The code block to execute while capturing standard output
     * @return The captured standard output as a string
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
     * Setup method that runs once before all tests.
     * This initializes the shared test data by running WslMonitor once
     * and reading the actual output file created by WslMonitor.
     */
    @BeforeAll
    fun setup(@TempDir tempDir: Path) {
        // Overwrite "user.home" property which is used to determine the output dir of the WslMonitor
        System.setProperty("user.home", tempDir.toString())

        consoleOutput = captureSystemOut {
            main()
        }

        outputFile = tempDir.resolve(".wsl-monitor")

        todayDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

        fileContent = outputFile.toFile().readText()
    }

    @Test
    fun `should execute apt update command`() {
        Assertions.assertTrue(
            consoleOutput.contains("Executing: wsl -e sudo -n apt update"),
            "Console output should contain apt update command"
        )
    }

    @Test
    fun `should execute apt list upgradable command`() {
        Assertions.assertTrue(
            consoleOutput.contains("Executing: wsl -e sudo -n apt list --upgradable"),
            "Console output should contain apt list upgradable command"
        )
    }

    @Test
    fun `should display completion message`() {
        Assertions.assertTrue(
            consoleOutput.contains("WSL update check completed."),
            "Console output should contain completion message"
        )
    }

    @Test
    fun `should create output file`() {
        println("[DEBUG_LOG] Output file path: ${outputFile.toAbsolutePath()}")
        println("[DEBUG_LOG] Output file content: $fileContent")

        Assertions.assertTrue(
            Files.exists(outputFile),
            "Output file should exist"
        )
    }

    @Test
    fun `should include WSL Update Check header in output file`() {
        Assertions.assertTrue(
            fileContent.contains("WSL Update Check"),
            "Output file should contain 'WSL Update Check'"
        )
    }

    @Test
    fun `should include today's date in output file`() {
        Assertions.assertTrue(
            fileContent.contains(todayDate),
            "Output file should contain today's date in ISO format: $todayDate"
        )
    }
}
