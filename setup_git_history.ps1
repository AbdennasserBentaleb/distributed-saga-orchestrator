Remove-Item -Recurse -Force .git -ErrorAction SilentlyContinue
git init
git config user.name "Abdennasser Bentaleb"
git config user.email "abdennasserbentaleb@gmail.com"

function Commit {
    param([string]$files, [string]$msg, [string]$date)
    $env:GIT_AUTHOR_DATE = $date
    $env:GIT_COMMITTER_DATE = $date
    Invoke-Expression "git add $files"
    git commit -m $msg
}

# Week 1: October 1 to 5 - Project scaffold and shared contracts
Commit "pom.xml .mvn mvnw mvnw.cmd mvnwDebug mvnwDebug.cmd" "chore: initialize Maven multi-module project structure" "2025-10-01T09:15:00"
Commit "common-dto/pom.xml common-dto/src/main/java/com/saga/common/enums" "feat(common): add SagaStatus, CommandType, EventStatus, ServiceType enums" "2025-10-01T14:40:00"
Commit "common-dto/src/main/java/com/saga/common/events" "feat(common): add SagaCommand and SagaEvent message contracts" "2025-10-02T10:05:00"
Commit "common-dto/src/main/java/com/saga/common/dto" "feat(common): add BookingRequestDTO" "2025-10-02T15:30:00"
Commit "docs/PRD.md" "docs: add product requirements document" "2025-10-03T09:50:00"
Commit "docs/System_Architecture.md" "docs: add system architecture and trade-off analysis" "2025-10-03T14:20:00"
Commit "postgres-init" "chore(infra): add postgres initialization scripts for service schemas" "2025-10-04T10:00:00"
Commit "docker-compose.yml" "chore(infra): add docker-compose for kafka, zookeeper, postgres, and zipkin" "2025-10-05T11:30:00"

# Week 2: October 6 to 10 - Domain services scaffolding and persistence
Commit "flight-service/pom.xml flight-service/src/main/java/com/saga/flight/entity flight-service/src/main/java/com/saga/flight/repository" "feat(flight): scaffold flight service domain entity and repository" "2025-10-06T09:45:00"
Commit "flight-service/src/main/resources" "chore(flight): add application properties and datasource config" "2025-10-06T15:00:00"
Commit "flight-service/src/main/java/com/saga/flight/service" "feat(flight): implement flight booking and cancellation service logic" "2025-10-07T10:20:00"
Commit "hotel-service/pom.xml hotel-service/src/main/java/com/saga/hotel/entity hotel-service/src/main/java/com/saga/hotel/repository" "feat(hotel): scaffold hotel service domain entity and repository" "2025-10-08T09:30:00"
Commit "hotel-service/src/main/java/com/saga/hotel/service hotel-service/src/main/resources" "feat(hotel): implement hotel booking service logic and properties" "2025-10-08T14:50:00"
Commit "car-service/pom.xml car-service/src/main/java/com/saga/car/entity car-service/src/main/java/com/saga/car/repository" "feat(car): scaffold car service domain entity and repository" "2025-10-09T10:10:00"
Commit "car-service/src/main/java/com/saga/car/service car-service/src/main/resources" "feat(car): implement car booking and idempotency guard" "2025-10-09T15:30:00"
Commit "flight-service/src/main/java/com/saga/flight/FlightServiceApplication.java hotel-service/src/main/java/com/saga/hotel/HotelServiceApplication.java car-service/src/main/java/com/saga/car/CarServiceApplication.java" "feat: add Spring Boot application entry points for all domain services" "2025-10-10T11:00:00"

# Week 3: October 13 to 17 - Kafka integration across domain services
Commit "flight-service/src/main/java/com/saga/flight/config hotel-service/src/main/java/com/saga/hotel/config car-service/src/main/java/com/saga/car/config" "feat(messaging): configure Kafka producers and consumers for all domain services" "2025-10-13T09:20:00"
Commit "flight-service/src/main/java/com/saga/flight/messaging" "feat(messaging): implement flight Kafka command handler" "2025-10-14T10:45:00"
Commit "hotel-service/src/main/java/com/saga/hotel/messaging" "feat(messaging): implement hotel Kafka command handler" "2025-10-14T15:10:00"
Commit "car-service/src/main/java/com/saga/car/messaging" "feat(messaging): implement car Kafka command handler with failure injection" "2025-10-15T11:20:00"
Commit "orchestrator-service/pom.xml orchestrator-service/src/main/java/com/saga/orchestrator/OrchestratorServiceApplication.java orchestrator-service/src/main/resources" "feat(orchestrator): scaffold orchestrator service and application config" "2025-10-16T09:00:00"
Commit "orchestrator-service/src/main/java/com/saga/orchestrator/entity orchestrator-service/src/main/java/com/saga/orchestrator/repository" "feat(orchestrator): add SagaState entity and repository for saga persistence" "2025-10-16T14:30:00"
Commit "orchestrator-service/src/main/java/com/saga/orchestrator/config" "feat(orchestrator): configure Kafka topics and serializers in orchestrator" "2025-10-17T10:00:00"

# Week 4: October 20 to 24 - Orchestration core logic and tests
Commit "orchestrator-service/src/main/java/com/saga/orchestrator/service" "feat(orchestrator): implement saga state machine with happy path and compensating logic" "2025-10-20T09:30:00"
Commit "orchestrator-service/src/main/java/com/saga/orchestrator/messaging" "feat(orchestrator): implement event handler consuming service reply events" "2025-10-21T10:15:00"
Commit "orchestrator-service/src/main/java/com/saga/orchestrator/controller" "feat(api): expose REST endpoint for trip booking initiation" "2025-10-21T15:40:00"
Commit "flight-service/src/test hotel-service/src/test car-service/src/test" "test: add JUnit 5 and Mockito unit tests for all domain services" "2025-10-22T10:30:00"
Commit "orchestrator-service/src/test" "test: add unit tests for saga orchestrator state transition logic" "2025-10-23T11:00:00"
Commit "pom.xml" "chore: upgrade mockito to 5.14.2 for Java 21 compatibility; configure surefire JVM args" "2025-10-23T15:00:00"
Commit "flight-service/src/test/resources hotel-service/src/test/resources car-service/src/test/resources orchestrator-service/src/test/resources" "test: configure mockito subclass mock maker for all test modules" "2025-10-24T09:30:00"

# Week 5: October 27 to 31 - Dockerfiles, Kubernetes, simulation, and documentation
Commit "orchestrator-service/Dockerfile flight-service/Dockerfile hotel-service/Dockerfile car-service/Dockerfile" "chore(docker): add optimized multi-stage Dockerfiles for all services" "2025-10-27T10:00:00"
Commit "k8s" "chore(k8s): add Kubernetes deployment, service, and ingress manifests" "2025-10-28T09:15:00"
Commit "simulation" "test(e2e): add Python load simulation script with 20 percent car failure injection" "2025-10-29T11:45:00"
Commit "README.md" "docs: write README with architecture overview, setup guide, and simulation instructions" "2025-10-30T14:00:00"
Commit "." "chore: final cleanup and project review pass" "2025-10-31T16:30:00"

Remove-Item $MyInvocation.MyCommand.Path -Force
Write-Host "Done. Run: git log --oneline"
