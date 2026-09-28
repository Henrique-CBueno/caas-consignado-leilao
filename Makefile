COMPOSE = docker compose -f infra/docker/docker-compose.dev.yml
K8S_NS = caas
K8S_DIR = infra/k8s
# Perfil dedicado: o perfil "minikube" padrão pode pertencer a outro projeto e não
# dá para redimensionar a memória de um perfil existente (ADR-0011).
MINIKUBE_PROFILE ?= caas
MINIKUBE_MEMORY ?= 8192
MINIKUBE_CPUS ?= 6
MK = minikube -p $(MINIKUBE_PROFILE)

.PHONY: up down ps logs test deploy-local smoke-test k8s-down

up:
	$(COMPOSE) up -d
	$(COMPOSE) ps

down:
	$(COMPOSE) down -v

ps:
	$(COMPOSE) ps

logs:
	$(COMPOSE) logs -f

# Milestone 9: sobe o sistema inteiro (Kafka+ZK, Vault, LocalStack de runtime,
# Postgres por serviço, os 9 serviços de app + dashboard) num cluster minikube
# do zero. helm precisa estar no PATH (ver ADR-0008 sobre as imagens do Kafka).
#
# SLIM=1 (usado no CI, ver ADR-0009): só o caminho crítico do fluxo feliz, sem
# tenant-service, api-gateway, notification-gateway, dashboard e Vault. O
# cluster completo usa ~5,2 GiB — não cabe nos 7 GB de um runner privado do
# GitHub Actions (o API server do minikube morre por falta de memória).
#
# Build no Docker do host (não no docker-env do minikube) + `minikube image
# load`: o docker-env quebra em runners onde o minikube usa containerd como
# runtime interno (buildx tenta subir um builder container e falha com 404 —
# achado empírico na Milestone 10, "highly experimental" no próprio aviso do
# minikube). `minikube image load` funciona com qualquer runtime interno.
FULL_SERVICES = tenant-service api-gateway proposal-service credit-analysis-service auction-service notification-gateway-service funder-bot-service contract-service disbursement-service
SLIM_SERVICES = proposal-service credit-analysis-service auction-service funder-bot-service contract-service disbursement-service
FULL_POSTGRES = tenant-service proposal-service credit-analysis-service contract-service disbursement-service
SLIM_POSTGRES = proposal-service credit-analysis-service contract-service disbursement-service
SERVICES = $(if $(SLIM),$(SLIM_SERVICES),$(FULL_SERVICES))
POSTGRES = $(if $(SLIM),$(SLIM_POSTGRES),$(FULL_POSTGRES))

test:
	./gradlew test integrationTest contractTest --continue

deploy-local:
	$(MK) status -f '{{.Host}}' | grep -q Running || $(MK) start --driver=docker --memory=$(MINIKUBE_MEMORY) --cpus=$(MINIKUBE_CPUS)
	kubectl config use-context $(MINIKUBE_PROFILE)
	./gradlew bootJar
	$(if $(SLIM),,cd dashboard && npx ng build)
	$(foreach s,$(SERVICES),docker build -t caas/$(s):local services/$(s) &&) true
	$(if $(SLIM),,docker build -t caas/dashboard:local dashboard)
	$(foreach s,$(SERVICES),$(MK) image load caas/$(s):local &&) true
	$(if $(SLIM),,$(MK) image load caas/dashboard:local)
	kubectl create namespace $(K8S_NS) --dry-run=client -o yaml | kubectl apply -f -
	helm repo add bitnami https://charts.bitnami.com/bitnami >/dev/null 2>&1 || true
	helm repo add hashicorp https://helm.releases.hashicorp.com >/dev/null 2>&1 || true
	helm repo update
	helm upgrade --install kafka bitnami/kafka --version 31.5.0 -n $(K8S_NS) -f $(K8S_DIR)/helm-values/kafka-values.yaml --wait --timeout 5m
	$(if $(SLIM),,helm upgrade --install vault hashicorp/vault -n $(K8S_NS) -f $(K8S_DIR)/helm-values/vault-values.yaml --wait --timeout 5m)
	$(if $(SLIM),,kubectl create configmap prometheus-config -n $(K8S_NS) --from-file=prometheus.yml=$(K8S_DIR)/observability/config/prometheus.yml --dry-run=client -o yaml | kubectl apply -f -)
	$(if $(SLIM),,kubectl create configmap grafana-datasources -n $(K8S_NS) --from-file=datasources.yml=$(K8S_DIR)/observability/config/grafana-datasources.yml --dry-run=client -o yaml | kubectl apply -f -)
	$(if $(SLIM),,kubectl create configmap grafana-dashboard-provider -n $(K8S_NS) --from-file=provider.yml=$(K8S_DIR)/observability/config/grafana-dashboard-provider.yml --dry-run=client -o yaml | kubectl apply -f -)
	$(if $(SLIM),,kubectl create configmap grafana-dashboards -n $(K8S_NS) --from-file=caas-overview.json=$(K8S_DIR)/observability/config/caas-overview.json --dry-run=client -o yaml | kubectl apply -f -)
	kubectl apply -f $(K8S_DIR)/localstack/ $(foreach p,$(POSTGRES),-f $(K8S_DIR)/postgres/postgres-$(p).yaml) $(foreach s,$(SERVICES),-f $(K8S_DIR)/services/$(s).yaml) $(if $(SLIM),,-f $(K8S_DIR)/dashboard/) $(if $(SLIM),,-f $(K8S_DIR)/observability/)
	# 600s: runners de CI têm bem menos CPU que uma máquina de desenvolvedor
	# (2 vCPUs no GitHub Actions) — pods subindo juntos com pull de imagem a
	# frio disputam CPU e demoram mais para ficar Ready.
	kubectl wait --for=condition=ready pod --all -n $(K8S_NS) --timeout=600s

# Publica uma proposta real via HTTP e espera o fluxo feliz completo terminar
# num desembolso consultável — critério de "pronto" do plano original,
# rodando inteiramente dentro do cluster.
smoke-test:
	kubectl create configmap smoke-test-script -n $(K8S_NS) --from-file=smoke-test.sh=$(K8S_DIR)/smoke-test.sh --dry-run=client -o yaml | kubectl apply -f -
	kubectl delete pod smoke-test -n $(K8S_NS) --ignore-not-found
	sed 's/CHECK_OBSERVABILITY_VALUE/$(if $(SLIM),0,1)/' $(K8S_DIR)/smoke-test-pod.yaml | kubectl apply -f -
	kubectl wait --for=jsonpath='{.status.phase}'=Succeeded pod/smoke-test -n $(K8S_NS) --timeout=420s
	kubectl logs -n $(K8S_NS) smoke-test

k8s-down:
	-helm uninstall kafka vault -n $(K8S_NS)
	kubectl delete namespace $(K8S_NS) --ignore-not-found
	$(MK) stop
