# 0004 — Java em vez de Kotlin como linguagem de aplicação

## Status
Aceita

## Contexto
As Milestones 0-2 foram construídas em Kotlin (decisão original da entrevista de design, que agrupava "Java/Kotlin (Spring Boot)" como uma única opção de stack). O usuário pediu explicitamente a troca para Java depois de ver o resultado em Kotlin — preferência de linguagem, não uma limitação técnica encontrada.

## Decisão
Todos os serviços (`tenant-service`, `api-gateway`, `proposal-service`) e o módulo `libs/event-schemas` foram reescritos de Kotlin para Java 21, mantendo exatamente a mesma arquitetura, comportamento e cobertura de teste — nenhuma decisão do plano foi reaberta, só a linguagem de implementação mudou. Equivalências usadas na tradução:

- `data class` imutável (`Tenant`, `Proposal`, DTOs, eventos) → **Java record**.
- `@JvmInline value class` (wrappers como `TenantId`, `ProposalId`) → **Java record** de um campo (não há inline/value classes estáveis em Java ainda — Project Valhalla segue em preview).
- `object` (singleton, ex.: `TenantContextHolder`) → classe final com construtor privado e membros estáticos.
- Entidades JPA (mutáveis, exigem construtor sem argumentos) → classes comuns com **Lombok** (`@Getter`, `@Setter` onde necessário, `@NoArgsConstructor(access = PROTECTED)`, `@AllArgsConstructor`) para evitar boilerplate de getters/setters.
- `when` exaustivo → `if`/`switch` do Java (não havia `when` complexo o suficiente pra precisar de switch expressions).
- Nulabilidade (`Tenant?`) → retorno `null` direto nos repositórios (mantido simples, sem introduzir `Optional` em todo lugar só por completude).

## Consequências
- Todos os 12 testes de integração (RLS, API REST, JWT/Cognito, outbox→Kafka) passaram sem nenhuma mudança de comportamento — a reescrita foi mecânica, a arquitetura Clean/DDD/RLS/outbox permaneceu idêntica.
- Build scripts (`build.gradle.kts`) continuam em Kotlin DSL — é uma escolha independente da linguagem de aplicação, comum mesmo em projetos Java puros.
- Módulos passaram a depender do plugin **Lombok** (`compileOnly`/`annotationProcessor`), versão gerenciada automaticamente pelo BOM do Spring Boot.
- Módulos Gradle precisam declarar `id("java")` explicitamente (o plugin `org.springframework.boot` sozinho não aplica o plugin `java` — diferente do que se assumiu inicialmente).
