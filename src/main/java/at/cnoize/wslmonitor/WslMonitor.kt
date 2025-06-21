package at.cnoize.wslmonitor

import at.cnoize.wslmonitor.WslUtils.formatPackageList
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * WslMonitor - A small application that checks for updates in WSL2 Debian/Ubuntu
 * and writes the results to a file. It can work with a specific WSL distribution
 * or use the default WSL distribution if none is specified.
 */
class WslMonitor(
    /**
     * The name of the WSL distribution to monitor, or null/empty to use the default WSL distribution
     */
    private val distribution: String? = null
) {

    /**
     * Checks for updates in WSL2 and writes the results to the output file.
     */
    fun checkForUpdates() {
        try {
            // First update the package list
            executeWslCommand("apt update")

            // Then check for upgradable packages
            val upgradeOutput = executeWslCommand("apt list --upgradable")

            // Parse the output to count upgradable packages
            val upgradableCount = countUpgradablePackages(upgradeOutput)

            // Write the results to the output file
            writeResults(upgradableCount, upgradeOutput)

            println("WSL update check completed. Found $upgradableCount upgradable packages.")
        } catch (e: IOException) {
            System.err.println("Error checking for updates: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * Executes a command in WSL and returns the output.
     *
     * @param command The command to execute
     * @return The command output
     * @throws IOException If an I/O error occurs
     */
    @Throws(IOException::class)
    private fun executeWslCommand(command: String): String {
        // Use sudo with -n flag to prevent password prompt
        // If this fails, the user will need to configure passwordless sudo in WSL

        val commandList = mutableListOf("wsl")

        if (!distribution.isNullOrBlank()) {
            commandList.add("-d")
            commandList.add(distribution)
        }

        // Add common arguments
        commandList.addAll(listOf("-e", "sudo", "-n"))

        // Split the command string into separate arguments and add them
        val commandArgs = command.split("\\s+".toRegex()).filterNot { it.isBlank() }
        commandList.addAll(commandArgs)

        // Create the process builder with all arguments
        val processBuilder = ProcessBuilder(commandList)

        // Log the full command being executed
        println("Executing: ${commandList.joinToString(" ")}")

        processBuilder.redirectErrorStream(true)
        val process = processBuilder.start()

        val output = StringBuilder()
        BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }
        }

        try {
            val exitCode = process.waitFor()
            if (exitCode != 0) {
                val outputStr = output.toString()
                System.err.println("Warning: Command '$command' exited with code $exitCode")
                System.err.println("Output: $outputStr")

                // Check for common issues
                when {
                    outputStr.contains("sudo: a password is required") -> {
                        throw IOException("Passwordless sudo is not configured. Please follow the instructions in the README.")
                    }

                    outputStr.contains("WSL distribution name not found") -> {
                        if (distribution.isNullOrBlank()) {
                            throw IOException("Default WSL distribution not found. Please make sure WSL is properly installed.")
                        } else {
                            throw IOException("WSL distribution '$distribution' not found. Please check the distribution name.")
                        }
                    }
                }
            }
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw IOException("Command execution was interrupted", e)
        }

        return output.toString()
    }

    /**
     * Counts the number of upgradable packages from the apt list output.
     *
     * @param output The output from apt list --upgradable
     * @return The number of upgradable packages
     */
    private fun countUpgradablePackages(output: String): Int {
        // Split the output by lines and count lines that contain package information
        // The first line is usually a header
        return output
            .split("\n")
            .count { it.isNotEmpty() && it.contains("/") && it.contains("[upgradable from") }
    }

    /**
     * Writes the update check results to the output file.
     *
     * @param upgradableCount The number of upgradable packages
     * @param fullOutput The full output from apt list --upgradable
     * @throws IOException If an I/O error occurs
     */
    @Throws(IOException::class)
    private fun writeResults(upgradableCount: Int, fullOutput: String) {
        val outputPath = Paths.get(OUTPUT_FILE)

        // Ensure parent directory exists
        outputPath.parent?.let { parent ->
            if (!Files.exists(parent)) {
                Files.createDirectories(parent)
            }
        }

        Files.newBufferedWriter(outputPath).use { writer ->
            val now = LocalDateTime.now()
            writer.write("WSL Update Check - ${now.format(FORMATTER)}\n")
            writer.write("----------------------------------------\n")
            writer.write("Upgradable packages: $upgradableCount\n\n")
            if (upgradableCount > 0) {
                writer.write("Details:\n")
                // Use the Kotlin utility class to format the package list
                val formattedOutput = formatPackageList(fullOutput)
                writer.write(formattedOutput)
            } else {
                writer.write("Your system is up to date.\n")
            }
        }
    }

    companion object {
        private const val OUTPUT_FILE_NAME = ".wsl-monitor"
        private val OUTPUT_FILE = "${System.getProperty("user.home")}${File.separator}$OUTPUT_FILE_NAME"
        private val FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

        @JvmStatic
        fun main(args: Array<String>) {
            // Use the first command-line argument as the distribution name, or use default WSL distribution
            val distribution = args.firstOrNull()
            val monitor = WslMonitor(distribution)

            println("Starting WSL Monitor for distribution: ${distribution ?: "default"}")
            println("Results will be written to: $OUTPUT_FILE")

            // Run once
            monitor.checkForUpdates()
        }
    }
}
