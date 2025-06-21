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
- JVM 11 or higher (for running Kotlin applications)
- Gradle (optional, wrapper included)
- Passwordless sudo configured in your WSL distribution (see below)

### Configuring Passwordless Sudo in WSL

The application uses sudo to run apt commands without requiring a password prompt. To configure passwordless sudo:

1. Open your WSL distribution (e.g., Debian or Ubuntu)
2. Edit the sudoers file:
   ```
   sudo visudo
   ```
3. Add the following line at the end of the file (replace `yourusername` with your actual WSL username):
   ```
   yourusername ALL=(ALL) NOPASSWD: /usr/bin/apt
   ```
4. Save and exit (in nano: Ctrl+O, Enter, Ctrl+X)

## Building the Application

To build the application, run:

```
./gradlew build
```

This will create a JAR file in the `build/libs` directory.

## Running the Application

You can run the application directly using:

```
kotlin -classpath build/libs/wsl-monitor-1.0-SNAPSHOT.jar at.cnoize.wslmonitor.WslMonitorKt [distribution-name]
```

Or using the JVM directly:

```
java -jar build/libs/wsl-monitor-1.0-SNAPSHOT.jar [distribution-name]
```

Where `[distribution-name]` is optional and specifies the name of your WSL distribution. If not provided, the default WSL distribution will be used.

Examples:
```
# Use default WSL distribution
kotlin -classpath build/libs/wsl-monitor-1.0-SNAPSHOT.jar at.cnoize.wslmonitor.WslMonitorKt

# Specify Ubuntu distribution
kotlin -classpath build/libs/wsl-monitor-1.0-SNAPSHOT.jar at.cnoize.wslmonitor.WslMonitorKt Ubuntu

# Specify Ubuntu-20.04 distribution
kotlin -classpath build/libs/wsl-monitor-1.0-SNAPSHOT.jar at.cnoize.wslmonitor.WslMonitorKt Ubuntu-20.04
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
6. Browse to the location of your Kotlin executable (e.g., `C:\Program Files\kotlin\bin\kotlin.bat`)
7. Add arguments: `-classpath "C:\path\to\wsl-monitor-1.0-SNAPSHOT.jar" at.cnoize.wslmonitor.WslMonitorKt [distribution-name]`
   - Replace `[distribution-name]` with your WSL distribution name (e.g., `Ubuntu`) or omit it to use the default WSL distribution
8. Finish the wizard

Alternatively, if you prefer using the JVM directly:
1. Follow steps 1-5 above
2. Browse to the location of your Java executable (e.g., `C:\Program Files\Java\bin\java.exe`)
3. Add arguments: `-jar "C:\path\to\wsl-monitor-1.0-SNAPSHOT.jar" [distribution-name]`
4. Finish the wizard


## Customization

To modify the application:

- Specify your WSL distribution name as a command-line argument (as shown in the "Running the Application" section)
- Modify the output file location by changing the `OUTPUT_FILE` constant in `WslMonitor.kt`
