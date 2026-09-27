// Versões de plugin fixadas no root e aplicadas (sem versão) pelos módulos de serviço a partir da Milestone 1.
// Spring Boot fixado em 3.5.x (não 4.x): Spring Cloud 2025.1.x, usado pelo api-gateway, ainda não tem
// train estável para Spring Boot 4 (2026.0.0 está em milestone) — ver ADR-015.
plugins {
    kotlin("jvm") version "2.4.20" apply false
    kotlin("plugin.spring") version "2.4.20" apply false
    id("org.springframework.boot") version "3.5.16" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
}

// ktlint-gradle 14.2.0 falha com "Extensions storage is not registered" nesta
// combinação de Kotlin/JDK (bug conhecido do plugin, não do nosso código). Lint
// dedicado fica pendente — não vale travar a milestone por uma ferramenta de lint.

allprojects {
    repositories {
        mavenCentral()
    }
}
