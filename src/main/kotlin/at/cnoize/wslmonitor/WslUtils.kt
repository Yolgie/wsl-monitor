package at.cnoize.wslmonitor

/**
 * Utility functions for WSL operations written in Kotlin.
 * This class demonstrates Kotlin integration with the Java-based WslMonitor.
 */
object WslUtils {

    /**
     * Formats a package list for better readability.
     * 
     * @param packageList The raw package list from apt
     * @return A formatted string with package information
     */
    @JvmStatic
    fun formatPackageList(packageList: String): String {
        if (packageList.isBlank()) {
            return "No packages found."
        }

        val formattedPackages = packageList.split("\n")
            .filter { it.contains("/") && it.contains("[upgradable from") }
            .map { line ->
                val parts = line.split("/")
                val packageName = parts[0].trim()
                val versionInfo = line.substringAfter("[upgradable from").substringBefore("]").trim()
                "• $packageName: $versionInfo"
            }
            .joinToString("\n")

        return if (formattedPackages.isBlank()) "No packages found." else formattedPackages
    }

    /**
     * Checks if WSL is installed and available.
     *
     * @return true if WSL is available, false otherwise
     */
    @JvmStatic
    fun isWslAvailable(): Boolean {
        return try {
            val process = ProcessBuilder("wsl", "--status").start()
            process.waitFor()
            process.exitValue() == 0
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Gets a list of available WSL distributions.
     *
     * @return A list of distribution names or empty list if none found
     */
    @JvmStatic
    fun getAvailableDistributions(): List<String> {
        return try {
            val process = ProcessBuilder("wsl", "--list", "--quiet").start()
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()

            output.split("\n")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
