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
# Build no Docker do host (não no docker-env do minikube) + `minikube image
# load`: o docker-env quebra em runners onde o minikube usa containerd como
# runtime interno (buildx tenta subir um builder container e falha com 404 —
# achado empírico na Milestone 10, "highly experimental" no próprio aviso do
# minikube). `minikube image load` funciona com qualquer runtime interno.
deploy-local:
	minikube status -f '{{.Host}}' | grep -q Running || minikube start --driver=docker
	./gradlew bootJar
	cd dashboard && npx ng build
	docker build -t caas/tenant-service:local services/tenant-service
	docker build -t caas/api-gateway:local services/api-gateway
	docker build -t caas/proposal-service:local services/proposal-service
	docker build -t caas/credit-analysis-service:local services/credit-analysis-service
	docker build -t caas/auction-service:local services/auction-service
	docker build -t caas/notification-gateway-service:local services/notification-gateway-service
	docker build -t caas/funder-bot-service:local services/funder-bot-service
	docker build -t caas/contract-service:local services/contract-service
	docker build -t caas/disbursement-service:local services/disbursement-service
	docker build -t caas/dashboard:local dashboard
	minikube image load caas/tenant-service:local
	minikube image load caas/api-gateway:local
	minikube image load caas/proposal-service:local
	minikube image load caas/credit-analysis-service:local
	minikube image load caas/auction-service:local
	minikube image load caas/notification-gateway-service:local
	minikube image load caas/funder-bot-service:local
	minikube image load caas/contract-service:local
	minikube image load caas/disbursement-service:local
	minikube image load caas/dashboard:local
	kubectl create namespace $(K8S_NS) --dry-run=client -o yaml | kubectl apply -f -
	helm repo add bitnami https://charts.bitnami.com/bitnami >/dev/null 2>&1 || true
	helm repo add hashicorp https://helm.releases.hashicorp.com >/dev/null 2>&1 || true
	helm repo update
	helm upgrade --install kafka bitnami/kafka --version 31.5.0 -n $(K8S_NS) -f $(K8S_DIR)/helm-values/kafka-values.yaml --wait --timeout 5m
	helm upgrade --install vault hashicorp/vault -n $(K8S_NS) -f $(K8S_DIR)/helm-values/vault-values.yaml --wait --timeout 5m
	kubectl apply -f $(K8S_DIR)/localstack/ -f $(K8S_DIR)/postgres/ -f $(K8S_DIR)/services/ -f $(K8S_DIR)/dashboard/
	kubectl wait --for=condition=ready pod --all -n $(K8S_NS) --timeout=180s

# Publica uma proposta real via HTTP e espera o fluxo feliz completo terminar
# num desembolso consultável — critério de "pronto" do plano original,
# rodando inteiramente dentro do cluster.
smoke-test:
	kubectl create configmap smoke-test-script -n $(K8S_NS) --from-file=smoke-test.sh=$(K8S_DIR)/smoke-test.sh --dry-run=client -o yaml | kubectl apply -f -
	kubectl delete pod smoke-test -n $(K8S_NS) --ignore-not-found
	kubectl apply -f $(K8S_DIR)/smoke-test-pod.yaml
	kubectl wait --for=jsonpath='{.status.phase}'=Succeeded pod/smoke-test -n $(K8S_NS) --timeout=200s
	kubectl logs -n $(K8S_NS) smoke-test

k8s-down:
	-helm uninstall kafka vault -n $(K8S_NS)
	kubectl delete namespace $(K8S_NS) --ignore-not-found
	minikube stop
