COMPOSE = docker compose -f infra/docker/docker-compose.dev.yml

.PHONY: up down ps logs

up:
	$(COMPOSE) up -d
	$(COMPOSE) ps

down:
	$(COMPOSE) down -v

ps:
	$(COMPOSE) ps

logs:
	$(COMPOSE) logs -f
