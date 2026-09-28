plugins {
    id("java")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencyManagement {
    imports {
        mavenBom("software.amazon.awssdk:bom:2.55.6")
    }
}

dependencies {
    implementation(project(":libs:event-schemas"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-json")
    implementation("org.springframework.kafka:spring-kafka")
    implementation("software.amazon.awssdk:dynamodb")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:kafka")
    testImplementation("org.testcontainers:localstack")
    testImplementation("au.com.dius.pact.provider:spring6:4.7.5")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// O pact é gerado pelo consumidor (funder-bot-service); nunca verificar um contrato desatualizado.
tasks.named<Test>("contractTest") {
    dependsOn(":services:funder-bot-service:contractTest")
}
