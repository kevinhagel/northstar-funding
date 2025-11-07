# Deployment Guide: Perplexica Infrastructure

**Feature**: 008-create-perplexica-self
**Date**: 2025-11-07
**Deployment Target**: Mac Studio (192.168.1.10)

## Pre-Deployment Checklist

- [ ] Mac Studio accessible at 192.168.1.10
- [ ] LM Studio running on Mac Studio (port 1234)
- [ ] Qwen2.5-0.5B-Instruct model loaded in LM Studio
- [ ] Current Docker services healthy (PostgreSQL, Qdrant, pgAdmin, SearXNG)
- [ ] Backup of docker/ directory created

## Step 1: Fix SearXNG Configuration (MacBook M2)

**File**: `docker/searxng/settings.yml`

**Change**:
```yaml
# BEFORE (line ~30):
brand:
  new_issue_url: false
  docs_url: false
  issue_url: false

# AFTER:
brand:
  new_issue_url: ""
  docs_url: ""
  issue_url: ""
  public_instances: ""
  wiki_url: ""
```

**Validation**: Check YAML syntax is valid

## Step 2: Add Perplexica to docker-compose.yml (MacBook M2)

**File**: `docker/docker-compose.yml`

**Add this service** (after searxng service):

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

**Add volumes** (in volumes section at bottom of file):

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

  perplexica-data:  # ADD THIS
    name: northstar-perplexica-data
    driver: local

  perplexica-uploads:  # ADD THIS
    name: northstar-perplexica-uploads
    driver: local
```

## Step 3: Create Perplexica Configuration (MacBook M2)

**Create directory**: `docker/perplexica/`

**Create file**: `docker/perplexica/config.toml`

**Content**:
```toml
[GENERAL]
PORT = 3000
SIMILARITY_MEASURE = "cosine"

[API_KEYS]
OPENAI = ""  # Not needed for local LLMs

[API_ENDPOINTS]
SEARXNG = "http://searxng:8080"

[MODELS.LM_STUDIO]
API_URL = "http://host.docker.internal:1234"

[CHAT_MODEL]
PROVIDER = "lm_studio"
MODEL = "qwen2.5-0.5b-instruct"
```

## Step 4: Rsync to Mac Studio (Kevin Performs)

**Command**:
```bash
rsync -av docker/ macstudio:~/northstar/
```

**Verify** files transferred:
- docker-compose.yml (updated)
- searxng/settings.yml (updated)
- perplexica/config.toml (new)

## Step 5: Deploy on Mac Studio (Kevin Performs)

**SSH to Mac Studio**:
```bash
ssh macstudio
cd ~/northstar
```

**Stop all services**:
```bash
docker compose down
```

**Pull Perplexica image**:
```bash
docker compose pull perplexica
```

**Start all services**:
```bash
docker compose up -d
```

**Check status**:
```bash
docker ps
```

**Expected output**: All containers running (northstar-postgres, qdrant, northstar-pgadmin, searxng, perplexica)

## Step 6: Verify Deployment

See `verification.md` for detailed health checks.

**Quick verification**:
```bash
# Check container health
docker ps --format 'table {{.Names}}\t{{.Status}}'

# Check Perplexica logs
docker logs perplexica --tail 50

# Check SearXNG logs (should have no errors)
docker logs searxng --tail 50

# Test Perplexica health endpoint
curl http://localhost:3000/api/health
```

## Rollback Procedure

If deployment fails:

1. **Stop all services**:
   ```bash
   docker compose down
   ```

2. **Restore backup docker-compose.yml**:
   ```bash
   cp docker-compose.yml.backup docker-compose.yml
   ```

3. **Restart existing services**:
   ```bash
   docker compose up -d
   ```

4. **Verify existing services healthy**:
   ```bash
   docker ps
   ```

## Post-Deployment

- [ ] All containers running and healthy
- [ ] SearXNG logs show no errors
- [ ] Perplexica UI accessible at http://192.168.1.10:3000
- [ ] Perplexica API responding to health checks
- [ ] Existing services (PostgreSQL, Qdrant) still working
- [ ] Document any issues in deployment log

## Troubleshooting

### Perplexica won't start

**Check LM Studio**:
```bash
curl http://localhost:1234/v1/models
```

**Check SearXNG**:
```bash
curl http://localhost:8080
```

**Check logs**:
```bash
docker logs perplexica --tail 100
```

### SearXNG still restarting

**Check settings.yml syntax**:
```bash
docker logs searxng | grep -i error
```

**Validate YAML**:
```bash
cat searxng/settings.yml | head -50
```

### Network issues

**Check northstar-network**:
```bash
docker network inspect northstar-network
```

**Verify Perplexica in network**:
```bash
docker inspect perplexica | grep -A 10 Networks
```

## Success Criteria

✅ All containers running and healthy
✅ SearXNG no restart loop
✅ Perplexica health endpoint returns 200
✅ Perplexica UI loads at port 3000
✅ PostgreSQL, Qdrant, pgAdmin unaffected

**Next**: See `verification.md` for detailed testing procedures
