# 0001 — Monorepo com Gradle multi-módulo (Kotlin DSL)

## Status
Aceita

## Contexto
O projeto é composto por 9 serviços backend (Kotlin/Spring Boot), um dashboard Angular, infraestrutura como código (Terraform, Kubernetes) e documentação, todos construídos por uma única pessoa como peça de portfólio. Múltiplos repositórios trariam overhead de sincronização (versionamento cruzado, PRs espalhados) sem o benefício real de multi-repo, que é permitir times independentes com ciclos de deploy próprios — inexistente aqui.

## Decisão
Monorepo único (`caas-consignado-leilao`), com build Gradle multi-módulo em Kotlin DSL na raiz. Cada serviço permanece um módulo Gradle independente (sem dependências de compilação entre módulos de serviço — só via `libs/`), preservando autonomia real de deploy apesar de estarem no mesmo repositório.

Versões de plugin fixadas no `build.gradle.kts` raiz e aplicadas sem versão pelos módulos:
- Gradle **9.8.0** (wrapper), Kotlin **2.4.20**, Spring Boot **3.5.16**, Spring Dependency Management **1.1.7**.
- Spring Boot foi fixado na série **3.5.x**, não na 4.x mais recente (4.1.1 no momento desta decisão): o Spring Cloud 2025.1.x — necessário para o `api-gateway` (Spring Cloud Gateway) — ainda não tem um train estável compatível com Spring Boot 4 (o 2026.0.0 está em milestone). Reavaliar quando o Spring Cloud publicar uma versão GA para Spring Boot 4.

## Consequências
- Um único `./gradlew build` roda todos os módulos; CI e revisão de PR ficam mais simples.
- Ao migrar para Spring Boot 4 no futuro, isso vira um upgrade coordenado de todos os módulos (não há versão mista intencional).
- O dashboard Angular fica fora do build Gradle (gerenciado por `npm`/Angular CLI dentro de `dashboard/`), sem integração de build cruzada — mantém as duas toolchains desacopladas.
