# 0024 — Kubernetes: StatefulSets, Helm e Vault em modo dev

## Status
Aceita

## Contexto
O ambiente demonstrado deve mostrar orquestração de workloads stateful, não só Deployments; ao mesmo tempo, roda numa máquina de desenvolvimento.

## Decisão
No minikube (perfil dedicado `caas`, 8 GiB e 6 CPUs): Kafka e Zookeeper por Helm (`bitnami/kafka` 31.5.0 com imagens `bitnamilegacy`, ADR-0008), Vault por Helm em **modo dev** (memória, token raiz fixo, sem persistência), um `StatefulSet` com PVC de Postgres **por serviço** (5), LocalStack e os serviços como Deployments em manifestos simples (`infra/k8s`). Imagens locais `caas/<serviço>:local` com `imagePullPolicy: Never` via `minikube image load`; acesso por NodePort (gateway 30080, notification 30086, dashboard 30090, Jaeger 30686, Grafana 30300), sem Ingress. `make deploy-local` sobe tudo; `SLIM=1` sobe a topologia reduzida (ADR-0009). JVMs limitadas a `-Xmx192m`.

## Consequências
- Divergência do plano: manifestos planos com o Makefile em vez de Kustomize `base/overlays` — sem segundo ambiente, overlays seriam cerimônia.
- O cluster completo consome cerca de 5 GiB.
- **Vault em modo dev perde os segredos ao reiniciar** e o token é público; nunca confundir com produção, onde seriam Vault HA com auto-unseal, Strimzi para o Kafka, NetworkPolicies e Ingress com TLS.
