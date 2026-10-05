# E-commerce System

Микросервисный проект интернет-магазина на Java и Spring Boot. Система создаёт заказы, резервирует товары и обрабатывает оплату. Сервисы обмениваются событиями через Apache Kafka и хранят данные в отдельных базах PostgreSQL.

Проект разработан командой из двух человек. Основной фокус - реализация паттерна Saga на основе событийного взаимодействия сервисов, включая компенсацию при неуспешной оплате и тестирование бизнес-логики.

## Сервисы

| Сервис | Задача |
|---|---|
| Order Service | Создание заказов и управление статусами |
| Inventory Service | Проверка остатков, резервирование и освобождение товаров |
| Payment Service | Проверка баланса и списание средств | 

**Стек:** Java 17, Spring Boot, Spring Data JPA, PostgreSQL, Apache Kafka, Docker Compose, Maven, JUnit, Mockito, Testcontainers, gitlab-ci.

## Обработка заказа

1. `POST /orders` создаёт заказ со статусом `PENDING`. Order Service публикует событие в Kafka.
2. Inventory Service проверяет остатки, рассчитывает стоимость и резервирует товары.
3. Payment Service проверяет баланс пользователя и обрабатывает оплату.
4. При успешной оплате заказ переходит в `APPROVED`.
5. При неуспешной оплате заказ переходит в `CANCELLED`, а Inventory Service освобождает резерв и возвращает товары на склад.

Если товара недостаточно, резервирование не выполняется и заказ отменяется. Доступные товары хранятся в таблице `products`, а резервации - в `reservations`. Для предотвращения повторного уменьшения остатка Inventory Service проверяет наличие резервации по `orderId`.

## API заказов

Создание заказа:

```http
POST http://localhost:8080/orders
Content-Type: application/json
```

```json
{
  "user_id": 1,
  "items": [
    {
      "item_id": 1,
      "quantity": 2
    }
  ]
}
```

Получение заказа и его текущего статуса:

```http
GET http://localhost:8080/orders/{id}
```

Обработка после создания заказа происходит асинхронно, поэтому первоначальный статус `PENDING` меняется не мгновенно.

## Тесты

- Unit-тесты проверяют бизнес-логику Order, Inventory и Payment Service с Mockito.
- Интеграционные тесты используют PostgreSQL через Testcontainers: проверяют сохранение заказа, резервирование товара и списание баланса.

## Структура

```text
e-commerce-system/
├── docker-compose.yml
├── .gitlab-ci.yml
├── init-db/
├── order-service/
├── inventory-service/
├── payment-service/
└── README.md
```
