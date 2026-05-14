plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    application
}

val ktorVersion = "3.3.1"
val testcontainersVersion = "1.20.6"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

kotlin {
    jvmToolchain(17)
}

tasks.withType<Test>().configureEach {
    maxParallelForks = 1

    // Force test executors to use JDK 17 (some machines default to newer JDKs like 25)
    javaLauncher.set(
        javaToolchains.launcherFor {
            languageVersion.set(JavaLanguageVersion.of(17))
        }
    )

    // Windows + Docker Desktop: ensure Testcontainers talks to the correct Docker Engine pipe.
    // Some environments end up with DOCKER_HOST=npipe://localhost:2375 which returns HTTP 400.
    environment("DOCKER_HOST", "npipe:////./pipe/docker_engine")
    environment("TESTCONTAINERS_HOST_OVERRIDE", "localhost")
}

application {
    mainClass.set("openqwoutt.textprocessor.backend.bootstrap.ApplicationKt")
    applicationName = "backend"
}

dependencies {
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-server-call-logging:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("ch.qos.logback:logback-classic:1.5.13")
    implementation("io.micrometer:micrometer-core:1.14.5")
    implementation("io.micrometer:micrometer-registry-prometheus:1.14.5")

    // Repo indexer (feature-flagged): Postgres + migrations
    implementation("org.postgresql:postgresql:42.7.6")
    implementation("com.zaxxer:HikariCP:6.3.0")
    implementation("org.flywaydb:flyway-core:11.8.2")
    implementation("org.flywaydb:flyway-database-postgresql:11.8.2")

    testImplementation(kotlin("test"))
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("io.ktor:ktor-client-mock:$ktorVersion")
    testImplementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    testImplementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testImplementation("org.testcontainers:junit-jupiter:$testcontainersVersion")
    testImplementation("org.testcontainers:postgresql:$testcontainersVersion")
}

tasks.test { useJUnitPlatform() }
