# ms-campuslab-bookings

Microservicio de reservas de CampusLab. Persiste reservas en Oracle y coordina
la disponibilidad de Catalog y la publicación de eventos.

## Endpoints internos

| Método | Ruta | Uso |
|---|---|---|
| `POST` | `/api/v1/bookings` | Crear una reserva; `userId` es un query parameter opcional |
| `GET` | `/api/v1/bookings` | Listar y filtrar por `status`, `from` y `to` |
| `GET` | `/api/v1/bookings/{id}` | Consultar por ID |
| `GET` | `/api/v1/bookings/user/{userId}` | Consultar por usuario |
| `PUT` | `/api/v1/bookings/{id}/status` | Aplicar una transición normal |
| `PUT` | `/api/v1/bookings/{id}/admin-status` | Actualización administrativa de estado |

El BFF publica sus propias rutas bajo `/api/v1/`; por ejemplo, el usuario
autenticado consulta sus reservas mediante `/api/v1/bookings/me`.

## Estados

Los valores aceptados por la API son los del enum `BookingStatus`:

- `REQUESTED`
- `APPROVED`
- `IN_PREPARATION`
- `IN_USE`
- `RETURNED`
- `CANCELLED`

Ejemplos:

```http
GET /api/v1/bookings?status=APPROVED
GET /api/v1/bookings?from=2026-09-01&to=2026-09-30
```

`startTime` y `endTime` son horas locales de `America/Santiago`, sin offset ni
sufijo `Z`. La autorización JWT y por roles de las rutas públicas se aplica
en el BFF; este microservicio no implementa seguridad HTTP propia.

## Integraciones

- **Catalog:** al crear la solicitud, Bookings reserva el cupo antes de
  persistirla en estado `REQUESTED`. Si la creación falla, intenta compensar
  liberando el cupo. Al cancelar o devolver una reserva, libera el cupo con
  una clave de idempotencia estable.
- **Kafka:** publica eventos de creación y cambio de estado en `bookings.events`,
  usando `bookingId` como clave.
- **RabbitMQ:** publica comandos en `cmd.direct`: `email.send` al solicitar,
  `prep.ticket` al aprobar y `voucher.gen` al devolver. Notify consume las
  colas correspondientes.

## Configuración

```env
DB_URL=
DB_USERNAME=
DB_PASSWORD=
CATALOG_SERVICE_URL=http://localhost:8082
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=your-rabbitmq-username
RABBITMQ_PASSWORD=your-rabbitmq-password
KAFKA_BOOTSTRAP_SERVERS=localhost:9092,localhost:9093,localhost:9094
```

## Ejecución y pruebas locales

Oracle y los servicios dependientes deben estar disponibles:

```bash
./mvnw spring-boot:run
./mvnw test
./mvnw clean package
```

El contrato de los endpoints internos está en
[`openapi.yaml`](./openapi.yaml).
