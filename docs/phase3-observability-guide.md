# Phase 3: Observability Integration

## What's running

- **Kiali** (http://localhost:20001): Mesh topology + live traffic visualization
- **Grafana** (http://localhost:3000): Dashboards for RED metrics (Request rate, Error rate, Duration)
- **Jaeger** (http://localhost:16686): Distributed request tracing
- **Prometheus** (backend): Scrapes Envoy metrics from sidecars

## What to explore

### 1. Kiali — Mesh Topology

**URL**: http://localhost:20001

**What to look for:**
- Left sidebar → "Graph" → Select "app-dev" namespace
- You'll see a **topology diagram**:
  - order-service pod (with sidecar)
  - stock-service pod (with sidecar)
  - Postgres, Redis, Pulsar (external to mesh, no sidecars)

**Key features:**
- **Edges**: Lines between pods show traffic flow
- **Padlock icons**: Green padlocks = mTLS encrypted (STRICT mode is active)
- **Arrow thickness**: Thicker arrows = more traffic
- **Request rates**: Hover over edges to see req/s, error %, latency p50/p95/p99

**Try this:**
1. In Kiali, click the order-service pod → "Outbound Traffic"
2. Observe: 90% going to stock-service v1 subset (VirtualService split)
3. Refresh and watch metrics update in real-time

### 2. Jaeger — Distributed Tracing

**URL**: http://localhost:16686

**What to look for:**
- Left sidebar → Service dropdown → select "order-service"
- Operation: "POST /api/orders"
- Click "Find Traces"
- Select a trace to see the **full request journey**

**What you'll see:**
- **Trace timeline**: shows all spans (app + Envoy at each hop)
  - order-service app: creates the HTTP request
  - order-service Envoy: encrypts, forwards (mTLS handshake)
  - network latency
  - stock-service Envoy: decrypts
  - stock-service app: processes request
  - All the way back

**Spans to look for:**
- `order.service /app` (app code)
- `order-service.app-dev.svc.cluster.local` (Envoy client-side)
- `stock-service.app-dev.svc.cluster.local` (Envoy server-side)
- `stock.service /app` (app code)

**Timing breakdown:**
- mTLS handshake + encryption overhead ~1-5ms per hop
- App processing dominates (JDBC, business logic)

### 3. Grafana — RED Metrics Dashboard

**URL**: http://localhost:3000 (admin/admin)

**What to look for:**
- Home → "Istio Service Dashboard" (or search "Istio")
- Select "app-dev" namespace
- Select "order-service" in the dropdown

**Dashboards show:**
- **Request rate**: requests/sec (red line)
- **Error rate**: % of 5xx responses
- **Duration**: p50, p95, p99 latencies
- **Traffic breakdown**: by destination (order→stock split)

**To see VirtualService effect:**
1. Pick a 5-10 minute time window
2. Grafana shows: order sending traffic to stock
3. If you had v2 running: would see 90/10 split in the "Outgoing Requests" panel

## How they connect

```
Sidecars (Envoy)
    ↓ (emit metrics via /stats endpoint)
Prometheus (scrapes every 15s)
    ↓
Kiali (queries Prometheus + Jaeger API)
    ↓
UI (topology, traffic, metrics)

Sidecars (Envoy)
    ↓ (forward traces via OTEL)
Jaeger collector
    ↓
Jaeger UI (timeline, spans, latency)
```

## Key takeaways

1. **Kiali**: The mesh is there, traffic flows, mTLS works (padlocks)
2. **Jaeger**: A single request touches multiple containers in sequence
3. **Grafana**: The mesh adds ~1-5ms latency (Envoy TLS overhead), but it's observable

## Next: Phase 4

Once you're comfortable here, Phase 4 moves to **Flux GitOps**:
- Move Istio CRDs (PeerAuthentication, VirtualService, DestinationRule) into git
- Commit a change (e.g., shift traffic from 90/10 to 80/20)
- Watch Flux apply it automatically
- Observe the change in Kiali (in real-time)
