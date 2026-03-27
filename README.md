# Distributed Key-Value Store (Java + Spring Boot)

A distributed in-memory key-value store built with Java and Spring Boot, featuring consistent hashing, async data replication across 3 microservice nodes, H2 SQL audit logging, and REST APIs — aligned with enterprise Java backend requirements.

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    REST Client / curl                    │
└──────────┬──────────────────────────────────────────────┘
           │ HTTP REST (GET / POST / DELETE)
┌──────────▼──────────────────────────────────────────────┐
│               Consistent Hash Ring (MD5)                 │
│                                                          │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐     │
│  │ Spring Boot │  │ Spring Boot │  │ Spring Boot │     │
│  │  Node 1     │  │  Node 2     │  │  Node 3     │     │
│  │  :8001      │  │  :8002      │  │  :8003      │     │
│  │             │  │             │  │             │     │
│  │ [KV Store]  │  │ [KV Store]  │  │ [KV Store]  │     │
│  │ [H2 DB]     │  │ [H2 DB]     │  │ [H2 DB]     │     │
│  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘     │
│         │◄───────────────►│◄───────────────►│           │
│                  Async Replication (RF=2)               │
└─────────────────────────────────────────────────────────┘
```

Each node is an independent Spring Boot microservice. Writes are replicated asynchronously to peer nodes. Every write operation is recorded in an H2 in-memory SQL database for audit purposes.

## Features

- **Java 17 + Spring Boot 3.2** — production-grade microservice framework
- **Consistent Hashing** — keys distributed across nodes using MD5 hashing with virtual nodes
- **Data Replication** — every write asynchronously replicated to peer nodes
- **Fault Tolerance** — cluster continues operating with 1 or 2 nodes down
- **REST API** — clean HTTP interface with Spring MVC `@RestController`
- **H2 SQL Audit Log** — every SET/DELETE operation recorded in an embedded SQL database
- **Spring Actuator** — built-in health and info endpoints
- **H2 Console** — browser-based SQL console at `/h2-console`

## Tech Stack

| Technology | Purpose |
|---|---|
| Java 17 | Core language |
| Spring Boot 3.2 | Microservice framework |
| Spring MVC | REST API layer |
| Spring Data JPA | Database access layer |
| H2 Database | In-memory SQL audit log |
| Maven | Build and dependency management |
| RestTemplate | Inter-node replication calls |

## Project Structure

```
distributed-kv-store-java/
├── src/main/java/com/kvstore/
│   ├── App.java                          # Spring Boot entry point
│   ├── controller/
│   │   └── KVController.java             # REST endpoints
│   ├── service/
│   │   ├── KVStoreService.java           # Thread-safe in-memory store
│   │   ├── ReplicationService.java       # Async peer replication
│   │   └── ConsistentHashService.java    # Consistent hashing ring
│   ├── model/
│   │   ├── KeyValue.java                 # Request/response model
│   │   └── AuditLog.java                 # JPA entity for audit log
│   └── repository/
│       └── AuditLogRepository.java       # Spring Data JPA repository
├── src/main/resources/
│   └── application.properties            # Node config (port, peers)
├── src/test/
│   └── AppTest.java
└── pom.xml                               # Maven dependencies
```

## Requirements

- Java 17+
- Maven 3.9+
- Git

## Quick Start

### 1. Clone and build

```bash
git clone https://github.com/YOUR_USERNAME/distributed-kv-store-java.git
cd distributed-kv-store-java
mvn clean install -DskipTests
```

### 2. Start the 3-node cluster

Open **3 separate terminals** and run one command in each:

**Terminal 1 — Node 1:**
```powershell
$env:PORT="8001"; $env:NODE_ID="node1"; $env:PEERS="localhost:8002,localhost:8003"; mvn spring-boot:run
```

**Terminal 2 — Node 2:**
```powershell
$env:PORT="8002"; $env:NODE_ID="node2"; $env:PEERS="localhost:8001,localhost:8003"; mvn spring-boot:run
```

**Terminal 3 — Node 3:**
```powershell
$env:PORT="8003"; $env:NODE_ID="node3"; $env:PEERS="localhost:8001,localhost:8002"; mvn spring-boot:run
```

Wait for all 3 to show:
```
Started App in X seconds
Tomcat started on port XXXX
```

### 3. Test the cluster

```powershell
# Set a key on Node 1
Invoke-RestMethod -Method POST -Uri "http://localhost:8001/set" `
  -ContentType "application/json" -Body '{"key":"city","value":"bangalore"}'

# Get from Node 1
Invoke-RestMethod -Uri "http://localhost:8001/get?key=city"

# Get from Node 2 — replication check!
Invoke-RestMethod -Uri "http://localhost:8002/get?key=city"

# Get from Node 3 — replication check!
Invoke-RestMethod -Uri "http://localhost:8003/get?key=city"

# Audit log — SQL database check!
Invoke-RestMethod -Uri "http://localhost:8001/audit"

# Health check
Invoke-RestMethod -Uri "http://localhost:8001/health"
```

### 4. View H2 Database Console

While a node is running, open in browser:
```
http://localhost:8001/h2-console
```

Use these credentials:
- **JDBC URL:** `jdbc:h2:mem:kvstore`
- **Username:** `sa`
- **Password:** (leave empty)

Then run:
```sql
SELECT * FROM AUDIT_LOG;
```

## API Reference

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/set` | Store a key-value pair + replicate + audit log |
| GET | `/get?key=<key>` | Retrieve a value by key |
| DELETE | `/delete?key=<key>` | Delete a key + audit log |
| GET | `/keys` | List all keys on this node |
| GET | `/health` | Node health, ID and port |
| GET | `/audit` | Last 10 operations from H2 audit log |
| POST | `/internal/replicate` | Internal replication endpoint (node-to-node) |

### Example Payloads

**POST /set request:**
```json
{ "key": "city", "value": "bangalore" }
```

**POST /set response:**
```json
{ "status": "ok", "key": "city", "node": "node1", "owner": "localhost:8001" }
```

**GET /audit response:**
```json
[
  {
    "id": 1,
    "action": "SET",
    "key": "city",
    "value": "bangalore",
    "nodeId": "node1",
    "timestamp": "2026-03-28T01:37:08"
  }
]
```

**GET /health response:**
```json
{ "status": "ok", "nodeId": "node1", "port": "8001" }
```

## Design Decisions

### Consistent Hashing
Keys are mapped to nodes using MD5 hashing with 3 virtual nodes per physical node (`TreeMap<Long, String>`). This ensures even key distribution and minimises reshuffling when nodes join or leave.

### Async Replication with RestTemplate
Each write spawns a new thread per peer using `new Thread(() -> restTemplate.postForObject(...))`. This makes writes non-blocking — the client gets an immediate response while replication happens in the background (AP system).

### H2 In-Memory SQL Audit Log
Every SET and DELETE operation is persisted to an H2 embedded database via Spring Data JPA. This demonstrates SQL integration, JPA entity mapping, and repository pattern — all common enterprise Java requirements.

### ConcurrentHashMap for Thread Safety
The in-memory store uses `ConcurrentHashMap` instead of `HashMap` to safely handle concurrent reads and writes from multiple HTTP request threads without explicit locking.

### CAP Theorem Position
This system is **AP (Available + Partition Tolerant)**. It favours availability — writes succeed even during partial failures — at the cost of eventual (not strong) consistency between replicas.

## Fault Tolerance

| Scenario | Behaviour |
|----------|-----------|
| 1 node down (2/3 alive) | Cluster continues; replication to 1 peer |
| 2 nodes down (1/3 alive) | Single node handles all requests |
| Node comes back online | Resumes serving immediately |
| Replication failure | Logged to console; write still succeeds |

## Running Tests

```bash
mvn test
```

## What I Would Add Next

- **Kafka** for reliable async replication (replace RestTemplate threads)
- **MongoDB** for persistent key-value storage across restarts
- **Spring Security** for API authentication
- **Docker Compose** to spin up 3 nodes with a single command
- **AWS EC2 deployment** with 3 nodes across availability zones
- **Circuit breaker** with Resilience4j for failed peer calls
- **Raft consensus** for strong consistency and leader election

## References

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Dynamo: Amazon's Highly Available Key-Value Store](https://www.allthingsdistributed.com/files/amazon-dynamo-sosp2007.pdf)
- [Designing Data-Intensive Applications — Martin Kleppmann](https://dataintensive.net/)
- [Consistent Hashing and Random Trees — Karger et al.](https://dl.acm.org/doi/10.1145/258533.258660)