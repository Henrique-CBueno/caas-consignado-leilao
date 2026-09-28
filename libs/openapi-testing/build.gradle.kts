plugins {
    `java-library`
    id("io.spring.dependency-management")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:3.5.16")
    }
}

// Só para testes de contrato de documentação (ADR de docs): quem consome traz servlet-api e o springdoc.
dependencies {
    api("org.springframework:spring-test")
    api("com.fasterxml.jackson.core:jackson-databind")
    api("org.assertj:assertj-core")
    compileOnly("jakarta.servlet:jakarta.servlet-api")
    compileOnly("org.springdoc:springdoc-openapi-starter-webmvc-api:2.8.17")
    compileOnly("org.springframework.boot:spring-boot-test-autoconfigure")
}
