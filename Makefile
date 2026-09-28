COMPOSE = docker compose -f infra/docker/docker-compose.dev.yml
K8S_NS = caas
K8S_DIR = infra/k8s

.PHONY: up down ps logs deploy-local smoke-test k8s-down

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

deploy-local:
	minikube status -f '{{.Host}}' | grep -q Running || minikube start --driver=docker
	./gradlew bootJar
	$(if $(SLIM),,cd dashboard && npx ng build)
	$(foreach s,$(SERVICES),docker build -t caas/$(s):local services/$(s) &&) true
	$(if $(SLIM),,docker build -t caas/dashboard:local dashboard)
	$(foreach s,$(SERVICES),minikube image load caas/$(s):local &&) true
	$(if $(SLIM),,minikube image load caas/dashboard:local)
	kubectl create namespace $(K8S_NS) --dry-run=client -o yaml | kubectl apply -f -
	helm repo add bitnami https://charts.bitnami.com/bitnami >/dev/null 2>&1 || true
	helm repo add hashicorp https://helm.releases.hashicorp.com >/dev/null 2>&1 || true
	helm repo update
	helm upgrade --install kafka bitnami/kafka --version 31.5.0 -n $(K8S_NS) -f $(K8S_DIR)/helm-values/kafka-values.yaml --wait --timeout 5m
	$(if $(SLIM),,helm upgrade --install vault hashicorp/vault -n $(K8S_NS) -f $(K8S_DIR)/helm-values/vault-values.yaml --wait --timeout 5m)
	kubectl apply -f $(K8S_DIR)/localstack/ $(foreach p,$(POSTGRES),-f $(K8S_DIR)/postgres/postgres-$(p).yaml) $(foreach s,$(SERVICES),-f $(K8S_DIR)/services/$(s).yaml) $(if $(SLIM),,-f $(K8S_DIR)/dashboard/)
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
	kubectl apply -f $(K8S_DIR)/smoke-test-pod.yaml
	kubectl wait --for=jsonpath='{.status.phase}'=Succeeded pod/smoke-test -n $(K8S_NS) --timeout=300s
	kubectl logs -n $(K8S_NS) smoke-test

k8s-down:
	-helm uninstall kafka vault -n $(K8S_NS)
	kubectl delete namespace $(K8S_NS) --ignore-not-found
	minikube stop
