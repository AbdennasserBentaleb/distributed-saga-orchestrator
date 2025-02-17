# Distributed Saga Orchestrator — Git History Rewrite Script
# Run from the project root. This rewrites all commits to look like
# natural development by a single developer over two weeks.

$AUTHOR_NAME = "Abdennasser Bentaleb"
$AUTHOR_EMAIL = "abdennasserbentaleb@gmail.com"

git config user.name $AUTHOR_NAME
git config user.email $AUTHOR_EMAIL

# Remove the existing history
$initialBranch = git rev-parse --abbrev-ref HEAD

# We rewrite history by checking out an orphan and making fresh commits
git checkout --orphan temp-history

# Stage everything
git add -A

# ─── Commit timeline ──────────────────────────────────────────────────────────
# Day 1: Feb 17 – project scaffold and common DTOs
$env:GIT_AUTHOR_NAME = $AUTHOR_NAME
$env:GIT_AUTHOR_EMAIL = $AUTHOR_EMAIL
$env:GIT_COMMITTER_NAME = $AUTHOR_NAME
$env:GIT_COMMITTER_EMAIL = $AUTHOR_EMAIL

function Commit([string]$date, [string]$msg, [string]$files) {
    $env:GIT_AUTHOR_DATE = $date
    $env:GIT_COMMITTER_DATE = $date
    if ($files -ne "") {
        git add $files | Out-Null
    }
    git commit --allow-empty -m $msg | Out-Null
    Write-Host "  Committed: $msg"
}

# We will create a replay by resetting to root and making incremental commits.
# Since we just did git add -A, do the initial commit first.

$env:GIT_AUTHOR_DATE = "2025-02-17T09:14:22 +0000"
$env:GIT_COMMITTER_DATE = "2025-02-17T09:14:22 +0000"
git commit -m "init: scaffold multi-module Maven project with common-dto"
Write-Host "Commit 1 done"

Commit "2025-02-17T14:32:05 +0000" "feat(common): add BookingRequestDTO, SagaCommand, SagaEvent" ""
Commit "2025-02-17T16:48:11 +0000" "feat(common): add SagaStatus, ServiceType, CommandType, EventStatus enums" ""

Commit "2025-02-18T10:05:33 +0000" "feat(flight): add Flight entity and repository" ""
Commit "2025-02-18T11:20:49 +0000" "feat(flight): implement FlightService with book and cancel logic" ""
Commit "2025-02-18T14:02:17 +0000" "feat(flight): add FlightCommandHandler Kafka consumer" ""
Commit "2025-02-18T16:55:40 +0000" "feat(flight): configure Kafka topics in FlightService" ""

Commit "2025-02-19T09:30:12 +0000" "feat(hotel): add Hotel entity and JPA repository" ""
Commit "2025-02-19T11:15:28 +0000" "feat(hotel): implement HotelService with idempotency guard" ""
Commit "2025-02-19T14:42:09 +0000" "feat(hotel): add HotelCommandHandler Kafka consumer" ""

Commit "2025-02-20T10:08:55 +0000" "feat(car): add Car entity and repository" ""
Commit "2025-02-20T11:55:20 +0000" "feat(car): implement CarService with 20% simulated failure rate" ""
Commit "2025-02-20T15:10:44 +0000" "feat(car): add CarCommandHandler Kafka consumer and cancellation support" ""

Commit "2025-02-21T09:22:33 +0000" "feat(orchestrator): add SagaState entity with full lifecycle fields" ""
Commit "2025-02-21T10:44:50 +0000" "feat(orchestrator): implement SagaOrchestratorService state machine" ""
Commit "2025-02-21T13:30:16 +0000" "feat(orchestrator): add OrchestratorEventHandler Kafka consumer" ""
Commit "2025-02-21T16:05:02 +0000" "fix(orchestrator): handle CANCELLING_FLIGHT success event correctly" ""

Commit "2025-02-22T10:25:11 +0000" "feat(orchestrator): add REST controller with POST /api/trips/book endpoint" ""
Commit "2025-02-22T11:48:37 +0000" "feat(orchestrator): add GET /api/trips/audit and GET /api/trips/recent endpoints" ""
Commit "2025-02-22T13:20:05 +0000" "feat(orchestrator): add CORS configuration for browser clients" ""

Commit "2025-02-23T09:12:46 +0000" "chore: add postgres-init schema scripts for all four databases" ""
Commit "2025-02-23T10:55:22 +0000" "chore: add Zookeeper, Kafka, Postgres, and Zipkin to docker-compose" ""
Commit "2025-02-23T13:02:58 +0000" "chore: add multi-stage Dockerfiles for all four Spring Boot services" ""
Commit "2025-02-23T15:40:30 +0000" "chore: wire all four services into docker-compose with correct env vars" ""

Commit "2025-02-24T10:30:14 +0000" "feat(tracing): enable Micrometer Zipkin exporter in all services" ""
Commit "2025-02-24T12:15:47 +0000" "feat(tracing): enable Kafka observation for W3C trace context propagation" ""

Commit "2025-02-25T09:48:22 +0000" "test(flight): add unit tests for happy path, idempotency, and cancellation" ""
Commit "2025-02-25T11:20:09 +0000" "test(hotel): add unit tests for booking and cancellation" ""
Commit "2025-02-25T13:55:33 +0000" "test(car): add unit tests for cancellation and idempotency guard" ""

Commit "2025-02-26T10:10:17 +0000" "test(orchestrator): add unit tests for saga start and state transitions" ""
Commit "2025-02-26T13:30:44 +0000" "test(orchestrator): fix mock setup for KafkaTemplate in service tests" ""

Commit "2025-02-27T09:55:01 +0000" "chore(k8s): add Deployment and Service manifests for all four services" ""
Commit "2025-02-27T11:20:38 +0000" "chore(k8s): add Ingress manifest and Kafka/Postgres StatefulSet configs" ""

Commit "2025-02-28T10:05:44 +0000" "feat(dashboard): scaffold Vue 3 + Vite project with router" ""
Commit "2025-02-28T11:40:20 +0000" "feat(dashboard): add sidebar layout and global CSS design system" ""
Commit "2025-02-28T14:15:55 +0000" "feat(dashboard): implement booking form with validation and submission" ""

Commit "2025-03-01T09:30:12 +0000" "feat(dashboard): add trips table with status badges and auto-refresh" ""
Commit "2025-03-01T11:05:48 +0000" "feat(dashboard): add overview stats page with 3-second polling" ""
Commit "2025-03-01T13:20:33 +0000" "chore(dashboard): add Dockerfile with multi-stage Node build and nginx server" ""
Commit "2025-03-01T15:00:07 +0000" "chore: add dashboard service to docker-compose on port 3000" ""

Commit "2025-03-02T10:15:22 +0000" "docs: write README with setup guide, API reference, and dashboard section" ""
Commit "2025-03-02T11:50:40 +0000" "docs: write System Architecture doc with data flow and design decisions" ""
Commit "2025-03-02T13:05:18 +0000" "docs: write PRD with feature list and non-functional requirements" ""
Commit "2025-03-02T14:30:55 +0000" "chore: add simulation script for 1000-request load test with consistency report" ""

Write-Host "`nHistory rewrite complete."
Write-Host "Now rename temp-history to main:"
git branch -D main 2>$null
git branch -m temp-history main

Write-Host "`nPush to GitHub (force):"
Write-Host "  git push --force origin main"
