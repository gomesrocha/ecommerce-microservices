up:
	docker compose up -d

down:
	docker compose down

logs:
	docker compose logs -f

ps:
	docker compose ps

build:
	docker compose build

restart:
	docker compose down
	docker compose up -d --build

clean:
	docker compose down -v

user-dev:
	cd user-api && ./mvnw quarkus:dev

test-user:
	cd user-api && ./mvnw test