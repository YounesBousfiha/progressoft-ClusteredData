.PHONY: up down logs test clean build restart

up:
	docker-compose up -d --build

down:
	docker-compose down

logs:
	docker-compose logs -f app

test:
	./mvnw clean test

coverage:
	./mvnw clean test jacoco:report
	@echo "Coverage report generated at: target/site/jacoco/index.html"

clean:
	./mvnw clean
	docker-compose down -v

build:
	./mvnw clean package -DskipTests

restart:
	docker-compose restart app

db-logs:
	docker-compose logs -f db

db-shell:
	docker exec -it postgres_fx_deals psql -U postgres -d bloomberg_fx

status:
	docker-compose ps

stop-app:
	docker-compose stop app

start-app:
	docker-compose start app

