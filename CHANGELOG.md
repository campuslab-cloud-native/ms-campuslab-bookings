# Changelog

Todos los cambios relevantes de este repositorio se documentan aquí.
El formato sigue [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/) y el versionamiento sigue [SemVer](https://semver.org/lang/es/).

## [1.0.0] - 2026-11-22

Primera versión estable.

- Reservas y ciclo de vida automático de estados.
- Publica comandos en RabbitMQ (`cmd.direct`) y eventos de reserva en Kafka (`bookings.events`).
- API versionada en `/api/v1/bookings`.
