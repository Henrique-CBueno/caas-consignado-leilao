// Versões de plugin fixadas no root e aplicadas (sem versão) pelos módulos de serviço a partir da Milestone 1.
// Spring Boot fixado em 3.5.x (não 4.x): Spring Cloud 2025.1.x, usado pelo api-gateway, ainda não tem
// train estável para Spring Boot 4 (2026.0.0 está em milestone) — ver ADR-015.
// Java (não Kotlin) desde a troca de linguagem pós-Milestone 2 — ver ADR-0004. O plugin
// org.springframework.boot aplica o plugin "java" automaticamente, não precisa declarar.
plugins {
    id("org.springframework.boot") version "3.5.16" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
}

allprojects {
    repositories {
        mavenCentral()
    }
}
