import com.github.gradle.node.npm.task.NpmTask

plugins {
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.spring") version "2.1.0"
    kotlin("kapt") version "2.1.0"
    id("org.springframework.boot") version "3.5.7"
    id("io.spring.dependency-management") version "1.1.7"
    id("com.github.node-gradle.node") version "7.1.0"
}

node {
    version.set("22.14.0")
    npmVersion.set("10.9.2")
    download.set(true)
    nodeProjectDir.set(layout.projectDirectory.dir("openapi"))
    npmInstallCommand.set("ci")
}

group = "org.turter"
version = "0.0.1-SNAPSHOT"
description = "wage-app"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:3.5.7")
    }
}

dependencies {
    // security + oauth
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")

    // reactor
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    // metrics
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-registry-prometheus")

    // Kotlin
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // Kotlin Coroutines + WebFlux
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactive")

    // validation
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Redisson
//    implementation("org.redisson:redisson-spring-boot-starter") {
//        exclude(group = "org.springframework.boot", module = "spring-boot-starter-web")
//        exclude(group = "org.springframework.boot", module = "spring-boot-starter-aop")
//    }

    // Liquibase / Preliquibase
    implementation("org.liquibase:liquibase-core")
    implementation("net.lbruun.springboot:preliquibase-spring-boot-starter:1.6.0")

    // R2DBC / PostgreSQL
    implementation("org.springframework.boot:spring-boot-starter-data-r2dbc")
    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("org.postgresql:r2dbc-postgresql")

    // utils
    developmentOnly("org.springframework.boot:spring-boot-devtools")

    // MapStruct
    implementation("org.mapstruct:mapstruct:1.5.5.Final")
    kapt("org.mapstruct:mapstruct-processor:1.5.5.Final")

    // Apache POI
    implementation("org.apache.poi:poi:5.5.1")
    implementation("org.apache.poi:poi-ooxml:5.5.1")

    // TESTS
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")

    testImplementation(platform("org.testcontainers:testcontainers-bom:1.20.3"))
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("com.redis.testcontainers:testcontainers-redis:1.6.4")

    // WireMock
    testImplementation("org.wiremock:wiremock-standalone:3.13.2")
    testImplementation("org.wiremock.integrations:wiremock-spring-boot:3.10.0")

    // Temporary baseline extraction support. This dependency is absent from production artifacts.
    testImplementation("org.springdoc:springdoc-openapi-starter-webflux-api:2.8.14")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.test {
    useJUnitPlatform {
        excludeTags("openapi-extraction")
    }
}

val extractCompanyOpenApi by tasks.registering(Test::class) {
    group = "openapi"
    description = "Extracts the current Company operations with the openapi-docs test profile."
    dependsOn(tasks.testClasses)
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    useJUnitPlatform {
        includeTags("openapi-extraction")
    }
    systemProperty(
        "openapi.extraction.output",
        layout.buildDirectory.file("openapi/extracted-company.json").get().asFile.absolutePath,
    )
    outputs.file(layout.buildDirectory.file("openapi/extracted-company.json"))
}

val openApiSources = fileTree("openapi") {
    include("**/*.yaml")
    exclude("bundled/**")
}

val openApiValidate by tasks.registering(NpmTask::class) {
    group = "verification"
    description = "Validates the modular OpenAPI document and its references."
    dependsOn(tasks.npmInstall)
    args.set(listOf("run", "validate"))
    inputs.files(openApiSources)
}

val openApiLint by tasks.registering(NpmTask::class) {
    group = "verification"
    description = "Lints the modular OpenAPI document."
    dependsOn(tasks.npmInstall)
    args.set(listOf("run", "lint"))
    inputs.files(openApiSources)
}

val openApiBundle by tasks.registering(NpmTask::class) {
    group = "openapi"
    description = "Builds a deterministic, self-contained OpenAPI document."
    dependsOn(tasks.npmInstall)
    args.set(listOf("run", "bundle"))
    inputs.files(openApiSources)
    outputs.file(layout.buildDirectory.file("openapi/openapi.yaml"))
    doFirst {
        layout.buildDirectory.dir("openapi").get().asFile.mkdirs()
    }
}

val openApiVerifyBundle by tasks.registering {
    group = "verification"
    description = "Fails when the committed OpenAPI bundle differs from its modular sources."
    dependsOn(openApiBundle)
    mustRunAfter("openApiUpdateBundle")
    inputs.file(layout.projectDirectory.file("openapi/bundled/openapi.yaml"))
    inputs.file(layout.buildDirectory.file("openapi/openapi.yaml"))
    doLast {
        val committed = layout.projectDirectory.file("openapi/bundled/openapi.yaml").asFile
        val generated = layout.buildDirectory.file("openapi/openapi.yaml").get().asFile
        check(committed.readBytes().contentEquals(generated.readBytes())) {
            "The committed OpenAPI bundle is stale. Run ./gradlew openApiUpdateBundle."
        }
    }
}

val openApiContractTest by tasks.registering(NpmTask::class) {
    group = "verification"
    description = "Checks the public guarantees of the bundled Company contract."
    dependsOn(tasks.npmInstall, openApiBundle)
    args.set(listOf("run", "contract-test"))
    inputs.file(layout.buildDirectory.file("openapi/openapi.yaml"))
    inputs.file(layout.projectDirectory.file("openapi/tests/company-contract.mjs"))
}

tasks.register<Copy>("openApiUpdateBundle") {
    group = "openapi"
    description = "Rebuilds the committed self-contained OpenAPI bundle."
    dependsOn(openApiBundle)
    from(layout.buildDirectory.file("openapi/openapi.yaml"))
    into(layout.projectDirectory.dir("openapi/bundled"))
}

val openApiCheck by tasks.registering {
    group = "verification"
    description = "Validates, lints, and checks the deterministic OpenAPI bundle."
    dependsOn(openApiValidate, openApiLint, openApiVerifyBundle, openApiContractTest)
}

tasks.check {
    dependsOn(openApiCheck)
}
