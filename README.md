# ms-campuslab-bookings

Microservicio de gestión de reservas de CampusLab.

## Tecnologías

- Java
- Spring Boot
- Spring Data JPA
- Spring Security
- Oracle

## Funcionalidades

- Crear reservas.
- Consultar reservas.
- Consultar reservas por ID.
- Filtrar reservas.
- Cambiar estado de una reserva.
- Coordinar la aprobación de reservas.

## Endpoints

```http
POST /api/v1/bookings
GET /api/v1/bookings
GET /api/v1/bookings/{id}
PUT /api/v1/bookings/{id}/status
```

## Filtros

```http
GET /api/v1/bookings?status=APROBADA
GET /api/v1/bookings?from=2026-09-01&to=2026-09-30
```

## Estados

- SOLICITADA
- APROBADA
- EN_PREPARACION
- EN_USO
- DEVUELTA
- CANCELADA

## Flujo de estados

```text
SOLICITADA
↓
APROBADA
↓
EN_PREPARACION
↓
EN_USO
↓
DEVUELTA
```

Una reserva también puede pasar a:

```text
CANCELADA
```

## Reglas de negocio

- Una reserva se crea en estado `SOLICITADA`.
- No puede pasar a `EN_USO` sin haber sido aprobada.
- El estudiante puede crear y consultar reservas.
- El técnico puede aprobar reservas y cambiar su estado.

## Base de datos

Oracle.

## Variables de entorno

```env
DB_URL=
DB_USERNAME=
DB_PASSWORD=
```

## Ejecución local

```bash
./mvnw spring-boot:run
```

## Build

```bash
./mvnw clean package
```
