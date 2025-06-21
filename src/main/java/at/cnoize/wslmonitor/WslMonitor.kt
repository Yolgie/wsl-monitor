package at.cnoize.wslmonitor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * WslMonitor - A small application that checks for updates in WSL2 Debian/Ubuntu
 * and writes the results to a file. It can work with a specific WSL distribution
 * or use the default WSL distribution if none is specified.
 */
public class WslMonitor {
    private static final String OUTPUT_FILE = System.getProperty("user.home") + File.separator + ".wsl-monitor";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final String distribution;

    /**
     * Creates a new WslMonitor with the specified WSL distribution.
     * 
     * @param distribution The name of the WSL distribution to monitor, or null/empty to use the default WSL distribution
     */
    public WslMonitor(String distribution) {
        this.distribution = distribution;
    }

    /**
     * Creates a new WslMonitor with the default WSL distribution.
     * This is equivalent to calling WslMonitor(null).
     */
    public WslMonitor() {
        this(null);
    }

    public static void main(String[] args) {
        // Use the first command-line argument as the distribution name, or use default WSL distribution
        String distribution = args.length > 0 ? args[0] : null;
        WslMonitor monitor = new WslMonitor(distribution);

        System.out.println("Starting WSL Monitor for distribution: " + 
            (distribution != null ? distribution : "default"));
        System.out.println("Results will be written to: " + OUTPUT_FILE);

        // Run once
        monitor.checkForUpdates();
    }

    /**
     * Checks for updates in WSL2 and writes the results to the output file.
     */
    public void checkForUpdates() {
        try {
            // First run apt update
            executeWslCommand("apt update");

            // Then check for upgradable packages
            String upgradeOutput = executeWslCommand("apt list --upgradable");

            // Parse the output to count upgradable packages
            int upgradableCount = countUpgradablePackages(upgradeOutput);

            // Write the results to the output file
            writeResults(upgradableCount, upgradeOutput);

            System.out.println("WSL update check completed. Found " + upgradableCount + " upgradable packages.");
        } catch (IOException e) {
            System.err.println("Error checking for updates: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Executes a command in WSL and returns the output.
     * 
     * @param command The command to execute
     * @return The command output
     * @throws IOException If an I/O error occurs
     */
    private String executeWslCommand(String command) throws IOException {
        // Use sudo with -n flag to prevent password prompt
        // If this fails, the user will need to configure passwordless sudo in WSL
        ProcessBuilder processBuilder;

        // Split the command string into separate arguments
        String[] commandArgs = command.split("\\s+");

        // Make distribution optional - if it's null or empty, don't specify it
        if (distribution == null || distribution.trim().isEmpty()) {
            // Use default WSL distribution
            processBuilder = new ProcessBuilder();
            processBuilder.command().add("wsl");
            processBuilder.command().add("-e");
            processBuilder.command().add("sudo");
            processBuilder.command().add("-n");
            // Add each command argument separately
            for (String arg : commandArgs) {
                processBuilder.command().add(arg);
            }
        } else {
            // Use specified distribution
            processBuilder = new ProcessBuilder();
            processBuilder.command().add("wsl");
            processBuilder.command().add("-d");
            processBuilder.command().add(distribution);
            processBuilder.command().add("-e");
            processBuilder.command().add("sudo");
            processBuilder.command().add("-n");
            // Add each command argument separately
            for (String arg : commandArgs) {
                processBuilder.command().add(arg);
            }
        }

        // Log the full command being executed
        StringBuilder fullCommand = new StringBuilder("Executing: wsl");
        for (String arg : processBuilder.command().subList(1, processBuilder.command().size())) {
            fullCommand.append(" ").append(arg);
        }
        System.out.println(fullCommand.toString());

        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
//        process.getInputStream().transferTo(System.out);
        process.getErrorStream().transferTo(System.err);

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        int exitCode;
        try {
            exitCode = process.waitFor();
            if (exitCode != 0) {
                System.err.println("Warning: Command '" + command + "' exited with code " + exitCode);
                System.err.println("Output: " + output.toString());

                // Check for common issues
                if (output.toString().contains("sudo: a password is required")) {
                    throw new IOException("Passwordless sudo is not configured. Please follow the instructions in the README.");
                } else if (output.toString().contains("WSL distribution name not found")) {
                    if (distribution == null || distribution.trim().isEmpty()) {
                        throw new IOException("Default WSL distribution not found. Please make sure WSL is properly installed.");
                    } else {
                        throw new IOException("WSL distribution '" + distribution + "' not found. Please check the distribution name.");
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Command execution was interrupted", e);
        }

        return output.toString();
    }

    /**
     * Counts the number of upgradable packages from the apt list output.
     * 
     * @param output The output from apt list --upgradable
     * @return The number of upgradable packages
     */
    private int countUpgradablePackages(String output) {
        // Split the output by lines and count lines that contain package information
        // The first line is usually a header, so we subtract 1 if there are any packages
        String[] lines = output.split("\n");
        int count = 0;

        for (String line : lines) {
            if (line.contains("/") && line.contains("[upgradable from")) {
                count++;
            }
        }

        return count;
    }

    /**
     * Writes the update check results to the output file.
     * 
     * @param upgradableCount The number of upgradable packages
     * @param fullOutput The full output from apt list --upgradable
     * @throws IOException If an I/O error occurs
     */
    private void writeResults(int upgradableCount, String fullOutput) throws IOException {
        Path outputPath = Paths.get(OUTPUT_FILE);

        // Ensure parent directory exists
        Path parent = outputPath.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }

        try (BufferedWriter writer = Files.newBufferedWriter(outputPath)) {
            LocalDateTime now = LocalDateTime.now();
            writer.write("WSL Update Check - " + now.format(FORMATTER) + "\n");
            writer.write("----------------------------------------\n");
            writer.write("Upgradable packages: " + upgradableCount + "\n\n");

            if (upgradableCount > 0) {
                writer.write("Details:\n");
                writer.write(fullOutput);
            } else {
                writer.write("Your system is up to date.\n");
            }
        }
    }
}
