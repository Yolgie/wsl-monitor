package at.cnoize.wslmonitor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for the WslMonitor class.
 * <p>
 * This test verifies that the WslMonitor can properly execute WSL commands
 * and handle the output, including commands with spaces.
 */
class WslMonitorTest {

    @TempDir
    Path tempDir;

    /**
     * Helper method to capture console output.
     */
    private String captureSystemOut(Runnable runnable) {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));

        try {
            runnable.run();
            return outContent.toString();
        } finally {
            System.setOut(originalOut);
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
    void testWslMonitorIntegration() throws IOException {
        // Override the output file location to use our temp directory
        System.setProperty("user.home", tempDir.toString());

        // Capture the console output to verify it contains the expected messages
        String consoleOutput = captureSystemOut(() -> {
            // Run the main method with no arguments (uses default distribution)
            WslMonitor.main(new String[0]);

        });

        // Verify the console output contains the expected command execution messages
        assertTrue(consoleOutput.contains("Executing: wsl -e sudo -n apt update"));
        assertTrue(consoleOutput.contains("Executing: wsl -e sudo -n apt list --upgradable"));

        // Verify the console output contains the completion message
        assertTrue(consoleOutput.contains("WSL update check completed."));

        // Check the content of the output file
        Path outputFile = tempDir.resolve(".wsl-monitor");
        assertTrue(Files.exists(outputFile), "Output file should exist");

        // Read the content of the file
        String fileContent = Files.readString(outputFile);

        // Check if it contains "WSL Update Check"
        assertTrue(fileContent.contains("WSL Update Check"), "Output file should contain 'WSL Update Check'");

        // Check if it contains today's date in ISO format (yyyy-MM-dd)
        String todayDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        assertTrue(fileContent.contains(todayDate), "Output file should contain today's date in ISO format: " + todayDate);
    }
}
