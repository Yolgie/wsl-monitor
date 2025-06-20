plugins {
    id("java")
    id("application")
}

group = "at.cnoize"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

application {
    mainClass.set("at.cnoize.wslmonitor.WslMonitor")
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "at.cnoize.wslmonitor.WslMonitor"
    }
}

tasks.test {
    useJUnitPlatform()
}
