
.PHONY: up down logs rebuild shell psql clean

ENV_FILE=app.env

up:
	cp -n app.env.example $(ENV_FILE) || true
	docker compose up --build -d

down:
	docker compose down

logs:
	docker compose logs -f --tail=200

rebuild:
	docker compose build --no-cache

shell:
	docker exec -it qa-service sh

psql:
	docker exec -it qa-db psql -U qaapp -d qaapp

clean:
	docker compose down -v
	docker image rm qa-service:local || true
