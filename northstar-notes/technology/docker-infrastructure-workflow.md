# NorthStar Docker Infrastructure - Workflow & Management

**Date**: 2025-11-07
**Location**: Mac Studio (192.168.1.10)
**Management**: Rsync from MacBook to Mac Studio

---

## Infrastructure Overview

### Current Setup (Mac Studio ~/northstar)

```
~/northstar/
├── docker-compose.yml          # Main orchestration file
├── .env                         # Environment variables
├── README.md                    # Infrastructure documentation
├── config/
│   └── init-db.sql             # PostgreSQL initialization
├── searxng/
│   └── settings.yml            # SearXNG configuration
├── qdrant/
│   └── qdrant-config.yaml      # Qdrant configuration
└── scripts/                     # Management scripts
```

### Services Running

| Service | Container Name | Port | Status | Purpose |
|---------|---------------|------|--------|---------|
| PostgreSQL | northstar-postgres | 5432 | ✅ Healthy | Primary database |
| pgAdmin | northstar-pgadmin | 5050 | ⚠️ Unhealthy | Database admin GUI |
| Qdrant | qdrant | 6333-6334 | ✅ Healthy | Vector database (future RAG) |
| SearXNG | searxng | 8080 | ❌ Failing | Search engine aggregator |
| LM Studio | N/A (native) | 1234 | ✅ Running | Local LLM inference |

---

## Deployment Workflow

### Local Development (MacBook)
```
/Users/kevin/github/northstar-funding/docker/
├── docker-compose.yml
├── searxng/settings.yml
├── qdrant/qdrant-config.yaml
└── config/init-db.sql
```

### Deployment Process

**1. Make changes locally on MacBook:**
```bash
cd /Users/kevin/github/northstar-funding/docker
# Edit docker-compose.yml or config files
```

**2. Rsync to Mac Studio:**
```bash
# You handle this - Kevin's workflow
rsync -av docker/ macstudio:~/northstar/
```

**3. Apply changes on Mac Studio:**
```bash
ssh macstudio
cd ~/northstar
docker compose down
docker compose up -d
docker ps  # Verify status
```

---

## Current Issue: SearXNG Configuration Error

### Problem
SearXNG container is restarting with error:
```
brand.issue_url: The value has to be one of these types/values: str
brand.new_issue_url: The value has to be one of these types/values: str
brand.docs_url: The value has to be one of these types/values: str
ValueError: Invalid settings.yml
```

### Root Cause
In `searxng/settings.yml`, the `brand` section uses `false` (boolean) but SearXNG expects strings:

```yaml
# WRONG (current)
brand:
  new_issue_url: false  # ❌ Boolean not allowed
  docs_url: false       # ❌ Boolean not allowed
  issue_url: false      # ❌ Boolean not allowed
```

### Solution
Fix `docker/searxng/settings.yml` in project, then rsync to Mac Studio:

```yaml
# CORRECT
brand:
  new_issue_url: ""  # ✅ Empty string
  docs_url: ""       # ✅ Empty string
  issue_url: ""      # ✅ Empty string
  public_instances: ""
  wiki_url: ""
```

**Or completely remove the brand section** (use defaults):
```yaml
# Just remove these lines entirely
# brand:
#   ...
```

---

## Adding Perplexica to Docker Compose

### Updated docker-compose.yml

Add this service to your `docker/docker-compose.yml`:

```yaml
  # Perplexica - AI Search Engine (Perplexity Clone)
  # =========================================
  # Version: v1.11.2 (latest as of 2025-11-07)
  # LM Studio integration added in v1.11.0
  perplexica:
    image: itzcrazykns1337/perplexica:latest
    container_name: perplexica
    hostname: perplexica
    ports:
      - "3000:3000"
    volumes:
      - perplexica-data:/home/perplexica/data
      - perplexica-uploads:/home/perplexica/uploads
      - ./perplexica/config.toml:/home/perplexica/config.toml:ro
    environment:
      - SEARXNG_API_URL=http://searxng:8080
      # NOTE: LM Studio is configured via config.toml, not environment variables
    networks:
      - northstar-network
    restart: unless-stopped
    depends_on:
      searxng:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:3000/api/health"]
      interval: 30s
      timeout: 10s
      retries: 3
    logging:
      driver: json-file
      options:
        max-size: "10m"
        max-file: "3"
```

### Add Perplexica volume to volumes section:

```yaml
volumes:
  postgres-data:
    name: northstar-postgres-data
    driver: local

  pgadmin-data:
    name: northstar-pgadmin-data
    driver: local

  qdrant-data:
    name: northstar-qdrant-data
    driver: local

  perplexica-data:  # ← ADD THIS
    name: northstar-perplexica-data
    driver: local

  perplexica-uploads:  # ← ADD THIS
    name: northstar-perplexica-uploads
    driver: local
```

### Create Perplexica Configuration

Create `docker/perplexica/config.toml`:

```toml
[GENERAL]
PORT = 3000
SIMILARITY_MEASURE = "cosine"

[API_KEYS]
OPENAI = ""  # Not needed for local LLMs

[API_ENDPOINTS]
SEARXNG = "http://searxng:8080"

[MODELS.LM_STUDIO]
API_URL = "http://host.docker.internal:1234"  # LM Studio on Mac Studio

[CHAT_MODEL]
PROVIDER = "lm_studio"  # LM Studio provider added in Perplexica v1.11.0
MODEL = "qwen2.5-0.5b-instruct"  # Model loaded in LM Studio
```

**Note**: Perplexica v1.11.0+ includes native LM Studio provider support. The `MODELS.LM_STUDIO` section configures the API endpoint, and `CHAT_MODEL.PROVIDER = "lm_studio"` tells Perplexica to use this provider.

### Deployment Steps

**On MacBook:**
```bash
cd /Users/kevin/github/northstar-funding/docker

# 1. Fix SearXNG settings
edit searxng/settings.yml
# Remove or fix brand section

# 2. Add Perplexica to docker-compose.yml
edit docker-compose.yml
# Add perplexica service

# 3. Create Perplexica config
mkdir -p perplexica
# Create perplexica/config.toml

# 4. Rsync to Mac Studio (your workflow)
# Kevin handles this step
```

**On Mac Studio:**
```bash
ssh macstudio
cd ~/northstar

# Stop all services
docker compose down

# Pull new Perplexica image
docker compose pull perplexica

# Start all services
docker compose up -d

# Check status
docker ps
docker logs perplexica --tail 50
docker logs searxng --tail 50

# Test Perplexica
curl http://localhost:3000/api/health
```

### Verify Setup

**1. Check all services:**
```bash
docker ps --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}'
```

**Expected output:**
```
NAMES                STATUS              PORTS
northstar-postgres   Up (healthy)        5432
northstar-pgadmin    Up (healthy)        5050
qdrant               Up (healthy)        6333-6334
searxng              Up (healthy)        8080  ← Should be fixed now
perplexica           Up (healthy)        3000  ← NEW
```

**2. Test Perplexica UI:**
```
http://192.168.1.10:3000
```

**3. Test Perplexica API:**
```bash
curl -X POST http://192.168.1.10:3000/api/search \
  -H "Content-Type: application/json" \
  -d '{
    "query": "EU Horizon grants for educational infrastructure Bulgaria",
    "focus_mode": "webSearch"
  }'
```

---

## Resource Impact Analysis

### Before Perplexica
```
System:              ~3 GB
Docker:              ~2 GB
  - PostgreSQL:      ~500 MB
  - Qdrant:          ~500 MB
  - pgAdmin:         ~300 MB
  - SearXNG:         ~200 MB
LM Studio:           ~1 GB
Available:           ~30 GB (83%)
```

### After Perplexica
```
System:              ~3 GB
Docker:              ~2.5 GB
  - PostgreSQL:      ~500 MB
  - Qdrant:          ~500 MB
  - pgAdmin:         ~300 MB
  - SearXNG:         ~200 MB
  - Perplexica:      ~300 MB ← NEW
LM Studio:           ~1 GB
Available:           ~29.5 GB (82%)
```

**Impact**: Minimal (~300 MB)

---

## Service Dependencies

### Dependency Graph
```
PostgreSQL (no dependencies)
    ↓
pgAdmin (depends on postgres)

Qdrant (no dependencies)

SearXNG (no dependencies)

Perplexica (depends on searxng + LM Studio)
    ↓
    ├── SearXNG (web search)
    └── LM Studio (AI reasoning, native app)
```

### Startup Order
1. PostgreSQL
2. pgAdmin (waits for postgres healthy)
3. Qdrant (independent)
4. SearXNG (independent)
5. Perplexica (waits for searxng healthy)

---

## Network Configuration

### Network: northstar-network
- **Type**: Bridge
- **Subnet**: 172.20.0.0/16
- **Access**: All containers can communicate by service name

### Internal Access (Container → Container)
```
perplexica → http://searxng:8080
perplexica → http://host.docker.internal:1234/v1 (LM Studio)
pgadmin → postgres:5432
```

### External Access (MacBook → Mac Studio)
```
PostgreSQL:  192.168.1.10:5432
pgAdmin:     192.168.1.10:5050
Qdrant:      192.168.1.10:6333
SearXNG:     192.168.1.10:8080
Perplexica:  192.168.1.10:3000 (NEW)
LM Studio:   192.168.1.10:1234
```

---

## Management Commands

### View Logs
```bash
# All services
docker compose logs -f

# Specific service
docker logs -f perplexica
docker logs -f searxng

# Last 50 lines
docker logs --tail 50 perplexica
```

### Restart Services
```bash
# Single service
docker compose restart perplexica

# All services
docker compose restart

# Recreate service (pull new image)
docker compose up -d --force-recreate perplexica
```

### Health Checks
```bash
# Check all health status
docker ps

# Detailed health for specific service
docker inspect perplexica | jq '.[0].State.Health'
```

### Resource Usage
```bash
# Memory and CPU usage
docker stats

# Disk usage
docker system df -v
```

### Clean Up
```bash
# Stop all services
docker compose down

# Remove unused images
docker image prune -a

# Remove unused volumes (DANGER: data loss!)
docker volume prune
```

---

## Troubleshooting

### Issue: Service Won't Start

**Check logs:**
```bash
docker logs <container-name> --tail 100
```

**Common causes:**
1. Port already in use
2. Volume permission issues
3. Configuration errors
4. Missing dependencies

### Issue: Can't Connect Between Containers

**Check network:**
```bash
docker network inspect northstar-network
```

**Verify service is in network:**
```bash
docker inspect <container-name> | jq '.[0].NetworkSettings.Networks'
```

### Issue: High Memory Usage

**Check resource usage:**
```bash
docker stats
```

**Solution:**
1. Restart memory-heavy service
2. Reduce container limits
3. Clean up unused images/volumes

---

## Future Enhancements

### Potential Additions

**1. Ollama (alongside LM Studio):**
```yaml
  ollama:
    image: ollama/ollama:latest
    container_name: ollama
    ports:
      - "11434:11434"
    volumes:
      - ollama-data:/root/.ollama
    networks:
      - northstar-network
```

**2. Open WebUI (Alternative to Perplexica):**
```yaml
  open-webui:
    image: ghcr.io/open-webui/open-webui:main
    container_name: open-webui
    ports:
      - "8081:8080"
    volumes:
      - open-webui-data:/app/backend/data
    environment:
      - SEARXNG_QUERY_URL=http://searxng:8080/search?q=<query>
      - OLLAMA_BASE_URL=http://host.docker.internal:1234/v1
    networks:
      - northstar-network
```

**3. Redis (for caching):**
```yaml
  redis:
    image: redis:7-alpine
    container_name: northstar-redis
    ports:
      - "6379:6379"
    volumes:
      - redis-data:/data
    networks:
      - northstar-network
```

---

## Constitutional Compliance

✅ **Complexity Management**: Still under 5 core services
- PostgreSQL (database)
- Qdrant (vector store)
- SearXNG (search)
- Perplexica (AI search) ← NEW
- LM Studio (native, not Docker)

✅ **Technology Stack**: All aligned
- PostgreSQL 16 ✅
- Docker infrastructure ✅
- Local AI (LM Studio) ✅
- Privacy-first search (SearXNG) ✅

✅ **Mac Studio Deployment**: Maintained
- Single deployment target ✅
- Persistent volumes ✅
- Health monitoring ✅

---

## Quick Reference

### Essential Commands
```bash
# Status check
ssh macstudio "docker ps"

# View logs
ssh macstudio "docker logs -f perplexica"

# Restart service
ssh macstudio "cd ~/northstar && docker compose restart perplexica"

# Full restart
ssh macstudio "cd ~/northstar && docker compose down && docker compose up -d"

# Health check
curl http://192.168.1.10:3000/api/health
```

### Service URLs
```
PostgreSQL:  postgresql://192.168.1.10:5432/northstar_funding
pgAdmin:     http://192.168.1.10:5050
Qdrant:      http://192.168.1.10:6333
SearXNG:     http://192.168.1.10:8080
Perplexica:  http://192.168.1.10:3000 (after adding)
LM Studio:   http://192.168.1.10:1234/v1
```

---

**Document Version**: 1.0
**Last Updated**: 2025-11-07
**Next Action**: Fix SearXNG config and add Perplexica
