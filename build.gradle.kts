import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.bmuschko.gradle.docker.tasks.container.DockerCreateContainer
import com.bmuschko.gradle.docker.tasks.container.DockerRemoveContainer
import com.bmuschko.gradle.docker.tasks.container.DockerStartContainer
import org.jooq.codegen.GenerationTool
import org.jooq.meta.jaxb.Configuration
import org.jooq.meta.jaxb.Database
import org.jooq.meta.jaxb.Generate
import org.jooq.meta.jaxb.Generator
import org.jooq.meta.jaxb.Jdbc
import org.jooq.meta.jaxb.Target


plugins {
    id("org.springframework.boot") version "3.2.5"
    id("io.spring.dependency-management") version "1.1.4"
    kotlin("jvm") version "1.9.23"
    kotlin("plugin.spring") version "1.9.23"

    // Plugins required for code generation of JOOQ
    id("org.jooq.jooq-codegen-gradle") version "3.19.8"
    id("org.flywaydb.flyway") version "9.16.0"
    id("com.bmuschko.docker-remote-api") version "7.4.0"
}

buildscript {
    dependencies {
        classpath("org.postgresql:postgresql:42.6.2")
    }
}

group = "net.intergamma"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-jooq") {
        exclude(group = "org.jooq", module = "jooq")
    }
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.flywaydb:flyway-core")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    implementation("org.postgresql:postgresql")
    implementation("org.jooq:jooq:3.19.8")
    implementation("org.jooq:jooq-codegen:3.19.8")
    implementation("org.jooq:jooq-meta:3.19.8")
    implementation("org.jooq:jooq-kotlin:3.19.8")
    implementation("org.jooq:jooq-kotlin-coroutines:3.19.8")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.5.0")

    jooqCodegen("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("io.kotest:kotest-assertions-core-jvm:5.9.0")
    testImplementation("org.testcontainers:testcontainers:1.19.7")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// This is configuration for the flywayMigrate task.
// This is only used for codegen when invoking 'gradle codegen', not during application runtime.
flyway {
    driver = "org.postgresql.Driver"
    url = "jdbc:postgresql://localhost:9199/stock"
    user = "stock"
    password = "stock"
    locations = arrayOf("filesystem:src/main/resources/db/migration")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
        jvmTarget = "21"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

val jooqCodegenPostgresContainer by tasks.creating(DockerCreateContainer::class) {
    targetImageId("postgres:16.3")
    containerName.set("gradle-jooq-postgres-codegen")
    hostConfig.portBindings.set(listOf("9199:5432"))
    envVars.put("POSTGRES_DB", "stock")
    envVars.put("POSTGRES_USER", "stock")
    envVars.put("POSTGRES_PASSWORD", "stock")
}
val startMyAppContainer by tasks.creating(DockerStartContainer::class) {
    dependsOn(jooqCodegenPostgresContainer)
    targetContainerId(jooqCodegenPostgresContainer.containerId)
    doLast {
        Thread.sleep(5000) // allow some time for PG to start, not ideal but works for now
    }
}

val deleteMyAppContainer by tasks.creating(DockerRemoveContainer::class) {
    targetContainerId(jooqCodegenPostgresContainer.containerId)
    force.set(true)
}

val flywayMigrate = tasks.named("flywayMigrate")


/***
 * JOOQ generates code based on a database schema and the generated code fits exactly to that given dialect.
 * However, running the code generation manually each time is a bit of a hassle.
 * So I created this task that creates a docker container with PostgreSQL,
 * Then runs flyway to apply the migrations,
 * and then run the JOOQ code generation, specifically configured for Kotlin.
 */
tasks.create("codegen") {
    description = "Generates code based on the db/migrations directory in the src/main/resources folder by creating a postgresql container, running flyway and executing the codegen."
    dependsOn(startMyAppContainer)
    dependsOn(flywayMigrate)
    finalizedBy(deleteMyAppContainer)

    flywayMigrate.configure {
        mustRunAfter(startMyAppContainer)
    }
    mustRunAfter(flywayMigrate)
    doLast {
        Thread.sleep(2000)
        GenerationTool.generate(
            Configuration()
                .withJdbc(
                    Jdbc()
                        .withDriver("org.postgresql.Driver")
                        .withUrl("jdbc:postgresql://localhost:9199/stock")
                        .withUser("stock")
                        .withPassword("stock")
                )
                .withGenerator(
                    Generator()
                        .withName("org.jooq.codegen.KotlinGenerator")
                        .withGenerate(
                            Generate()
                                .withPojosAsKotlinDataClasses(true)
                                .withKotlinNotNullPojoAttributes(true)
                                .withKotlinSetterJvmNameAnnotationsOnIsPrefix(true)
                                .withImplicitJoinPathsAsKotlinProperties(true)
                        )
                        .withDatabase(
                            Database()
                                .withName("org.jooq.meta.postgres.PostgresDatabase")
                                .withInputSchema("public")
                                .withIncludes(".*")
                                .withExcludes("flyway_schema_history")
                        )
                        .withTarget(
                            Target()
                                .withPackageName("net.intergamma.stock.db")
                                .withDirectory(layout.projectDirectory.dir("src/main/kotlin").asFile.absolutePath)
                        )
                )
        )
    }
}