# WSL Monitor

A small Windows application that monitors updates in WSL2 Debian/Ubuntu distributions.

## Features

- Checks for available updates in WSL2 Debian/Ubuntu
- Runs `apt update` before checking for upgradable packages
- Counts and lists all upgradable packages
- Writes results to a file (`.wsl-monitor`) in the user's home directory
- Can be scheduled using Windows Task Scheduler

## Requirements

- Windows 10/11 with WSL2 installed
- Debian or Ubuntu distribution installed in WSL2
- JVM 21 or higher (for running Kotlin applications)
- Gradle (optional, wrapper included)
- Passwordless sudo configured in your WSL distribution (see below)

### Configuring Passwordless Sudo in WSL

The application uses sudo to run apt commands without requiring a password prompt. To configure passwordless sudo add this to your sudoers file in your WSL distro:
   ```
   yourusername ALL=(ALL) NOPASSWD: /usr/bin/apt update, /usr/bin/apt list --upgradable
   ```

## Building the Application

To build the application, run:

```
.\gradlew build
```

This will create a JAR file in the `build\libs` directory.

## Running the Application

You can run the application directly using:

```
java -jar build\libs\wsl-monitor-1.0-SNAPSHOT.jar [distribution-name]
```

Where `[distribution-name]` is optional and specifies the name of your WSL distribution. If not provided, the default WSL distribution will be used.

Examples:
```
# Use default WSL distribution
java -jar build\libs\wsl-monitor-1.0-SNAPSHOT.jar

# Specify Ubuntu distribution
java -jar build\libs\wsl-monitor-1.0-SNAPSHOT.jar Ubuntu

# Specify Ubuntu-20.04 distribution
java -jar build\libs\wsl-monitor-1.0-SNAPSHOT.jar Ubuntu-20.04
```

When run, the application will execute once and then exit. Use Windows Task Scheduler to run it periodically.

## Output

The application writes its output to a file named `.wsl-monitor` in your Windows user home directory. The file contains:
- Timestamp of the check
- Number of upgradable packages
- Details of upgradable packages (if any)

## Setting Up Scheduled Execution

### Using Windows Task Scheduler

1. Open Windows Task Scheduler
2. Create a new Basic Task
3. Name it "WSL Update Monitor" (or any name you prefer)
4. Set the trigger (e.g., Daily)
5. For the action, select "Start a program"
6. Browse to the location of your Java executable (e.g., `C:\Program Files\Java\bin\java.exe`)
7. Add arguments: `-jar "C:\path\to\wsl-monitor-1.0-SNAPSHOT.jar" [distribution-name]`
   - Replace `C:\path\to` with the actual path to your JAR file
   - Replace `[distribution-name]` with your WSL distribution name (e.g., `Ubuntu`) or omit it to use the default WSL distribution
8. Finish the wizard

## Customization

To modify the application:

- Specify your WSL distribution name as a command-line argument (as shown in the "Running the Application" section)
- Modify the output file location by changing the `OUTPUT_FILE` constant in `WslMonitor.kt`

## Development

This project is written in Kotlin and uses Gradle for build management. The main components are:

- `WslMonitor.kt`: The main class that handles checking for updates and writing results
- `WslUtils.kt`: Utility functions for formatting package lists and checking WSL availability

### Future Improvements

See the [tasks.md](docs/tasks.md) file for a detailed list of planned improvements to the project.

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Continuous Integration

This project uses GitHub Actions for continuous integration. The workflow automatically builds the project and runs all tests on every push to the main branch and on every pull request.

The CI workflow helps ensure that:
- The code builds successfully
- All tests pass
- New contributions don't break existing functionality

You can see the current build status in the GitHub repository.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request. All pull requests will be automatically tested by our CI workflow to ensure they don't break existing functionality.

## Disclaimer

This project was mostly created with AI-assisted coding using Junie, Jetbrains AI and ChatGPT. The AI tools were used to help with code generation, problem-solving, commit messages, and documentation.
