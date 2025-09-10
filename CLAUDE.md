# Claude Code Rules for PCS API

## Container Dependencies

**CRITICAL RULE**: Before running `./gradlew bootWithCCD`, these 5 containers MUST be running:

1. **cftlib-rse-idam-simulator-1** (Port 5062) - IDAM Authentication
2. **cftlib-shared-database-pg-1** (Port 6432) - PostgreSQL Database  
3. **cftlib-ccd-elasticsearch-1** (Port 9200) - Elasticsearch Search
4. **cftlib-xui-manage-org-1** (Port 3001) - Organisation Management
5. **cftlib-xui-manage-cases-1** (Port 3000) - Case Management UI

## Pre-execution Validation

**ALWAYS run this dependency check before `./gradlew bootWithCCD`:**

```bash
# Check all required containers are running
echo "Checking container dependencies..."
REQUIRED_CONTAINERS=("cftlib-rse-idam-simulator-1" "cftlib-shared-database-pg-1" "cftlib-ccd-elasticsearch-1" "cftlib-xui-manage-org-1" "cftlib-xui-manage-cases-1")

for container in "${REQUIRED_CONTAINERS[@]}"; do
    if ! docker ps --format '{{.Names}}' | grep -q "^${container}$"; then
        echo "❌ ERROR: Container ${container} is not running!"
        echo "Please start containers first using the CCD framework."
        exit 1
    else
        echo "✅ Container ${container} is running"
    fi
done

# Check port availability
echo "Checking port availability..."
for port in 5062 6432 9200 3001 3000; do
    if ! nc -z localhost $port 2>/dev/null; then
        echo "❌ ERROR: Port $port is not accessible!"
        exit 1
    else
        echo "✅ Port $port is accessible"
    fi
done

echo "✅ All dependencies are ready. Proceeding with bootWithCCD..."
```

## Quick Dependency Check

**Use the provided helper script:**
```bash
./check-dependencies.sh
```

## Development Workflow Rules

### Before running `./gradlew bootWithCCD`:
1. **MANDATORY**: Run `./check-dependencies.sh` first
2. Verify all 5 containers are running and healthy
3. Ensure ports 5062, 6432, 9200, 3001, 3000 are accessible
4. Only then proceed with `./gradlew bootWithCCD`

### Common Commands
- **Dependency check**: `./check-dependencies.sh`
- **Start local environment**: `./gradlew bootWithCCD` (after dependency check)
- **Run tests**: `./gradlew test`
- **Run functional tests**: `./gradlew functional`  
- **Run integration tests**: `./gradlew integration`
- **Health check**: `curl http://localhost:3206/health`

### Access Points
- **PCS API**: http://localhost:3206
- **Swagger UI**: http://localhost:3206/swagger-ui/index.html
- **XUI Case Management**: http://localhost:3000
- **XUI Organisation Management**: http://localhost:3001

## Container Startup Order
If containers are not running, they should be started via the `rse-cft-lib` Gradle plugin which handles the correct startup sequence and dependencies.

## Error Handling
If `./gradlew bootWithCCD` fails:
1. Check container status: `docker ps`
2. Run dependency check: `./check-dependencies.sh` 
3. Verify database connectivity: `pg_isready -h localhost -p 6432`
4. Check Elasticsearch: `curl localhost:9200/_cluster/health`

## Build Requirements
- Java 21
- Gradle (wrapper included)  
- Docker and Docker Compose
- All containers must be healthy before bootWithCCD

## Linting Commands
```bash
./gradlew check
./gradlew test
```