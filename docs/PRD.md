# Product Requirements Document

## 1. Project Overview

**Name:** Distributed Travel Saga Orchestrator

**Purpose:** Build a travel booking system that handles a multi-step distributed transaction across three independent microservices: Flight, Hotel, and Car Rental. The system must guarantee data consistency even when individual services fail mid-workflow, using the Saga Orchestration Pattern to coordinate forward commits and backward compensations.

## 2. Context

This project was built to develop hands-on experience with distributed systems design patterns, event-driven architecture, and cloud-native deployment on Kubernetes. The domain was chosen for its straightforward multi-step nature, which maps cleanly to the saga state machine model.

## 3. Core Features

**Trip Booking Endpoint:** A single REST endpoint accepts a booking request containing flight, hotel, and car rental details, and returns a saga ID the client can use to poll for status.

**Saga Orchestration:** The Orchestrator Service manages the full transaction lifecycle. It issues commands to domain services sequentially and tracks the current state in its PostgreSQL database.

**Compensating Transactions:** If any domain service reports a failure, the Orchestrator issues cancellation commands to all services that have already committed, unwinding the partial transaction cleanly.

**Event-Driven Communication:** Services communicate exclusively through Apache Kafka topics. There are no direct HTTP calls between microservices.

**Idempotent Processing:** Each domain service checks whether a record already exists for a given saga ID before processing a command. Re-delivered messages from Kafka do not produce duplicate bookings.

**Observability:** Structured logging via SLF4J and distributed trace propagation via Micrometer with a Zipkin exporter.

**Operational Dashboard:** A Vue 3 single-page application provides a real-time view into the system. It supports submitting new bookings from the browser, viewing a live audit summary, and inspecting the 20 most recent saga records with their current status.

## 4. Technology Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.2.x |
| Messaging | Apache Kafka |
| Persistence | Spring Data JPA, PostgreSQL |
| Build | Maven (multi-module) |
| Containerization | Docker, Docker Compose |
| Orchestration | Kubernetes |
| Testing | JUnit 5, Mockito |
| Observability | Micrometer, OpenTelemetry, Zipkin |
| Dashboard | Vue 3, Vite |

## 5. Non-Functional Requirements

**Consistency:** The system must never leave a saga in a partial state. If a failure occurs and compensating transactions cannot complete, the saga must remain in a retryable intermediate state rather than silently losing data.

**Stateless Services:** All four microservices are stateless outside of their respective databases. This allows horizontal scaling without session affinity.

**Independent Deployability:** Each service must build, test, and deploy independently. A failure in one service's CI pipeline must not block deployment of the others.

**Idempotency:** All Kafka consumers must handle at-least-once delivery without producing duplicate side effects.
