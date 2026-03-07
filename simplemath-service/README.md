# SimpleMath microservice

A small Java (Spring Boot) microservice that provides 4 REST APIs:

- Add
- Subtract
- Multiply
- Divide

Each API accepts **2 positive integers** (`a`, `b`) and returns a result. Requests with **null** or **negative** inputs return `400 Bad Request` with a validation error payload.

## Requirements

- Java 17+
- Maven 3.9+ (or use your IDE’s Maven integration)

## Run

```bash
cd /Users/suryamohang/Documents/workspace/simplemath-service
mvn spring-boot:run
```

Service starts on `http://localhost:8080`.

## APIs

Base path: `/api/v1/math`

### Add

```bash
curl -s -X POST http://localhost:8080/api/v1/math/add \
  -H 'Content-Type: application/json' \
  -d '{"a":5,"b":3}'
```

### Subtract

```bash
curl -s -X POST http://localhost:8080/api/v1/math/subtract \
  -H 'Content-Type: application/json' \
  -d '{"a":5,"b":3}'
```

### Multiply

```bash
curl -s -X POST http://localhost:8080/api/v1/math/multiply \
  -H 'Content-Type: application/json' \
  -d '{"a":5,"b":3}'
```

### Divide

```bash
curl -s -X POST http://localhost:8080/api/v1/math/divide \
  -H 'Content-Type: application/json' \
  -d '{"a":10,"b":2}'
```

## Validation examples

Negative value:

```bash
curl -s -X POST http://localhost:8080/api/v1/math/add \
  -H 'Content-Type: application/json' \
  -d '{"a":-1,"b":2}' | jq .
```

Null / missing value:

```bash
curl -s -X POST http://localhost:8080/api/v1/math/add \
  -H 'Content-Type: application/json' \
  -d '{"a":1}' | jq .
```

