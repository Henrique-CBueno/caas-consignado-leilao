# 0005 — JevOpenRouterAdapter (decisão real) e segredo via Vault

## Status
Aceita

## Contexto
A Milestone 3 deixou o `credit-analysis-service` com uma porta `CreditDecisionPort` desacoplada da implementação, especificamente para permitir plugar uma decisão real (Jev, via OpenRouter) sem tocar no restante do pipeline. Esta milestone implementa essa segunda implementação e resolve como o segredo (`OPENROUTER_API_KEY`) chega até ela via Vault — e validando empiricamente essa integração, achou dois problemas reais no caminho (mesma disciplina da ADR-0002).

## Decisão

### JevOpenRouterAdapter
Chama `POST https://openrouter.ai/api/alpha/decisions` (schema real, verificado na documentação oficial da OpenRouter) com o primitivo **Choice**: `model: "typesafe/jev-1.13"`, um `state` livre (borrowerId, score simulado, valor solicitado) e `questions.credit_decision` com `criteria` mapeando as três opções (`APPROVE`/`REJECT`/`MANUAL_REVIEW`) para descrições em linguagem natural. A resposta (`answers.credit_decision.choice` + `.confidence`) mapeia 1:1 para `CreditDecisionResult` — nenhuma lógica de negócio adicional no adapter (a trava de confiança já vive em `CreditDecisionService`, Milestone 3, e continua valendo sem alteração).

Circuit Breaker + Rate Limiter (Resilience4j, instância `jev-openrouter`) envolvem a chamada HTTP, mesma configuração já usada para `cognito-jwks` no `api-gateway`.

Seleção de adapter por `@ConditionalOnProperty`: `MockDecisionAdapter` é o padrão absoluto (`matchIfMissing = true`); `JevOpenRouterAdapter` só ativa com `app.credit-decision.provider=jev` — nunca em CI/local por acidente.

### Vault (dois problemas reais encontrados e corrigidos)

1. **Ordem de bootstrap**: `spring.config.import=vault://` é resolvido no `ConfigDataEnvironmentPostProcessor`, uma fase **mais cedo** do bootstrap do Spring Boot do que quando `@DynamicPropertySource` consegue injetar valores (via `ApplicationContextInitializer`, registrado depois). Um teste com Testcontainers Vault usando `@DynamicPropertySource` para `spring.cloud.vault.uri`/`token` falha com `"Cannot create authentication mechanism for TOKEN"` porque o valor ainda não existe quando o Vault tenta autenticar. **Correção**: setar via `System.setProperty(...)` num bloco estático, antes do Spring sequer começar a subir — `SystemPropertiesPropertySource` existe desde o início do bootstrap.

2. **`spring-cloud-vault-config` ativo só por estar no classpath**: mesmo sem `spring.config.import` conter `vault://` e sem nenhum profile Vault ativo, a mera presença da dependência faz o Spring Cloud Vault tentar autenticar contra `localhost:8200` (endereço/porta padrão dele) em **qualquer** contexto Spring do módulo — quebrando testes que não têm nada a ver com Vault. **Correção**: `spring.cloud.vault.enabled: false` explícito no documento YAML padrão (sem profile), religado só dentro do bloco do profile `jev`.

Com os dois achados corrigidos: o profile Spring `jev` (`spring.profiles.active=jev`) liga `app.credit-decision.provider=jev` + `spring.cloud.vault.enabled=true` + `spring.config.import=optional:vault://` juntos, endereçando o Vault do `docker-compose.dev.yml` (token `dev-root-token`) para ler `secret/credit-analysis-service` → `app.jev.api-key`. O prefixo `optional:` evita falha dura se o Vault não estiver acessível quando o profile é usado sem ele já estar de pé.

## Consequências
- Testes automatizados/CI nunca tocam Vault nem a API real da OpenRouter — `MockDecisionAdapter` continua sendo o que roda por padrão, e o teste de Vault usa um Testcontainers Vault isolado, não o do `docker-compose.dev.yml`.
- Demonstrar a integração real exige rodar com `--spring.profiles.active=jev`, `OPENROUTER_API_KEY` de fato presente no Vault (`secret/credit-analysis-service`), e o `docker-compose.dev.yml` (Vault) no ar — documentado no `README.md`/roteiro de demo (a preencher na Milestone 14).
- `spring.cloud.vault.enabled: false` por padrão é um lembrete de que bibliotecas de terceiros podem ter comportamento "ativo por presença no classpath" sem gate explícito — vale checar isso cedo em qualquer integração nova, não assumir que profile/config-import sozinhos bastam.
