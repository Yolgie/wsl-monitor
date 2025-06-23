package at.cnoize.wslmonitor

import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

typealias UpgradablePackage = Pair<String, String>

private const val OUTPUT_FILE_NAME = ".wsl-monitor"
private val OUTPUT_FILE = "${System.getProperty("user.home")}${File.separator}$OUTPUT_FILE_NAME"
private val FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

/**
 * Main entry point for the application.
 * Executes WSL commands to check for updates and writes the results to a file.
 */
fun main() {
    println("Starting WSL Monitor")
    println("Results will be written to: ${OUTPUT_FILE}")

    try {
        executeWslCommand("apt update")
        val upgradeOutput = executeWslCommand("apt list --upgradable")
        val upgradablePackages = extractUpgradablePackages(upgradeOutput)
        writeResults(upgradablePackages)
        println("WSL update check completed. Found ${upgradablePackages.size} upgradable packages.")
    } catch (e: IOException) {
        System.err.println("Error checking for updates: ${e.message}")
        e.printStackTrace()
    }
}

/**
 * Executes a command in WSL and returns the output.
 * Uses ProcessBuilder to run the command in WSL with sudo privileges.
 *
 * @param command The command to execute
 * @return The command output as a string
 */
private fun executeWslCommand(command: String): String {
    val commandList = mutableListOf("wsl", "-e", "sudo", "-n")
    commandList.addAll(command.split("\\s+".toRegex()).filterNot { it.isBlank() })
    println("Executing: ${commandList.joinToString(" ")}")
    val processBuilder = ProcessBuilder(commandList)
    processBuilder.redirectErrorStream(true)
    val process = processBuilder.start()
    val output = captureProcessOutput(process)
    try {
        val exitCode = process.waitFor()
        if (exitCode != 0) {
            System.err.println("Warning: Command '$command' exited with code $exitCode")
            System.err.println("Output: $output")
            when {
                output.contains("sudo: a password is required") -> {
                    throw IOException("Passwordless sudo is not configured. Please follow the instructions in the README.")
                }

                output.contains("WSL distribution name not found") -> {
                    throw IOException("WSL not found. Please make sure WSL is properly installed.")
                }
            }
        }
    } catch (e: InterruptedException) {
        Thread.currentThread().interrupt()
        throw IOException("Command execution was interrupted", e)
    }
    return output
}

/**
 * Captures the output from a process.
 *
 * @param process The process to capture output from
 * @return The captured output as a string
 */
private fun captureProcessOutput(process: Process): String {
    val output = StringBuilder()
    BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            output.append(line).append("\n")
        }
    }
    return output.toString()
}

/**
 * Writes the update check results to the output file.
 * Creates the output file if it doesn't exist and writes a formatted report
 * containing the number of upgradable packages and their details.
 *
 * @param upgradablePackages List of upgradable packages with their version information
 */
private fun writeResults(upgradablePackages: List<UpgradablePackage>) {
    val outputPath = Paths.get(OUTPUT_FILE)
    val upgradableCount = upgradablePackages.size

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
            writer.write(formatPackageList(upgradablePackages))
        } else {
            writer.write("Your system is up to date.\n")
        }
    }
}

/**
 * Extracts upgradable packages from apt output.
 * Parses the output of 'apt list --upgradable' command to identify packages
 * that can be upgraded and their version information.
 *
 * @param packageList The raw package list from apt list --upgradable command
 * @return A list of upgradable packages with their version information
 */
fun extractUpgradablePackages(packageList: String): List<UpgradablePackage> {
    return packageList.split("\n")
        .filter { it.contains("/") && it.contains("[upgradable from") }
        .map { line ->
            val packageName = line.split("/")[0].trim()
            val versionInfo = line.substringAfter("[upgradable from").substringBefore("]").trim()
            packageName to versionInfo
        }
}

/**
 * Formats a package list for better readability.
 * Converts the list of upgradable packages into a human-readable string format
 * with bullet points for each package.
 *
 * @param upgradablePackages List of upgradable packages with their version information
 * @return A formatted string with package information, one package per line with bullet points
 */
fun formatPackageList(upgradablePackages: List<UpgradablePackage>): String {
    return if (upgradablePackages.isEmpty()) {
        "No packages found."
    } else {
        upgradablePackages.joinToString("\n") { (name, version) ->
            "• $name: $version"
        }
    }
}
