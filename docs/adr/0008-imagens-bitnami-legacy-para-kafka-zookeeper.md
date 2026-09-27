# 0008 — Imagens `bitnamilegacy/*` para Kafka+Zookeeper no Helm chart

## Status
Aceita

## Contexto
O plano exige Kafka **explicitamente com Zookeeper**, não KRaft (decisão já registrada antes desta ADR). O Bitnami Helm chart `bitnami/kafka` na sua versão mais recente (32.x) só empacota Kafka 4.0.0 — versão em que o modo Zookeeper foi removido da própria distribuição do Apache Kafka (KIP-500 completo, KRaft obrigatório). A única forma de manter Zookeeper é fixar uma versão mais antiga do chart (`31.5.0`, Kafka `3.9.0` — a última release do Apache Kafka antes da remoção do modo Zookeeper), com `kraft.enabled: false` e `zookeeper.enabled: true`.

Validado empiricamente: a imagem `docker.io/bitnami/kafka:3.9.0-debian-12-r12` (a que o chart `31.5.0` referencia por padrão) não existe mais no registry `bitnami/*` — a Bitnami passou a manter, sob o nome de imagem padrão, só a última major version de cada produto; todo o histórico anterior foi movido para o registry `bitnamilegacy/*` (ainda público, sem necessidade de autenticação, só sob outro namespace). O mesmo vale para `bitnami/zookeeper:3.9.3-debian-12-r21` → `bitnamilegacy/zookeeper:3.9.3-debian-12-r21`.

## Decisão
Instalar `bitnami/kafka` (chart `31.5.0`) e `bitnami/zookeeper` (chart embutido como subchart) sobrescrevendo `image.registry`/`image.repository` de cada um para apontar para `docker.io/bitnamilegacy/kafka` e `docker.io/bitnamilegacy/zookeeper` respectivamente, mantendo as mesmas tags (`3.9.0-debian-12-r12` / `3.9.3-debian-12-r21`) que o chart já espera.

## Consequências
- `bitnamilegacy/*` são imagens arquivadas, não recebem mais patch de segurança — aceitável para este projeto de portfólio/demo, mas seria um bloqueador real num ambiente de produção (justificaria migrar para o Strimzi Kafka Operator, que continua mantendo Zookeeper como opção suportada por mais tempo, ou reabrir a decisão de usar KRaft).
- Mesmo padrão de correção já usado nas ADR-0002 (limitações do LocalStack Community), ADR-0004 (Kotlin → Java) e ADR-0006 (Servlet+STOMP em vez de WebFlux): o plano original é ajustado à luz de uma restrição real encontrada durante a implementação, sem reabrir a decisão de arquitetura por trás dela (Kafka com Zookeeper, não KRaft).
- Critério de migração futura, não implementado agora: se o projeto precisasse continuar recebendo patches de segurança do Kafka, a saída seria trocar para KRaft (reabrindo a decisão original) ou para o Strimzi Operator.
