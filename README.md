# Flux + Istio Test Setup

Sandbox to test Flux and istio with a simple microservices setup combining Spring Boot services, Istio service mesh, and GitOps via FluxCD.

## Components

- **order-service** – Spring Boot service that accepts orders, calls stock-service to reserve inventory, and publishes `OrderPlaced` events.
- **stock-service** – Spring Boot service that manages inventory, exposes a reserve/replenish API, and consumes `OrderPlaced` events to write an audit trail.
- **PostgreSQL** – primary datastore for both services (separate logical databases).
- **Redis** – idempotency cache for order-service request deduplication.
- **Apache Pulsar** – event broker (standalone mode) carrying `OrderPlaced` events from order-service to stock-service.
- **Istio** – service mesh providing mTLS, traffic routing, and policy enforcement.
- **Kiali** – Istio mesh visualization and observability dashboard.
- **Jaeger** – distributed tracing backend for request timeline analysis.
- **Prometheus + Grafana** – metrics collection and RED (Rate/Error/Duration) dashboard.

![Kiali mesh topology](./docs/kiali.png)

