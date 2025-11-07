# Tasks: Perplexica Self-Hosted AI Search Infrastructure

**Input**: Design documents from `/Users/kevin/github/northstar-funding/specs/008-create-perplexica-self/`
**Prerequisites**: plan.md ✅, deployment.md ✅, verification.md ✅, quickstart.md ✅

## Execution Flow
```
1. Load plan.md from feature directory ✅
   → Infrastructure deployment (not code implementation)
   → Tech stack: Docker Compose 2.x, TOML, Perplexica v1.11.2
2. Load deployment artifacts ✅
   → deployment.md: 6 deployment steps identified
   → verification.md: Health check procedures
   → quickstart.md: Manual testing scenarios
3. Generate infrastructure deployment tasks:
   → Pre-deployment checks
   → Configuration fixes (SearXNG)
   → Docker Compose updates (Perplexica service, volumes)
   → Perplexica configuration (config.toml)
   → Deployment (Kevin performs rsync and restart)
   → Verification (health checks, API tests)
   → Documentation updates
4. Apply task rules:
   → Infrastructure tasks are SEQUENTIAL (no parallelization)
   → Configuration before deployment
   → Deployment before verification
   → Kevin performs all deployment operations
5. Number tasks sequentially (T001-T012)
6. No parallel execution (infrastructure dependencies)
7. Validate task completeness ✅
   → All configuration files updated
   → All deployment steps covered
   → All verification procedures included
8. Return: SUCCESS (tasks ready for execution)
```

---

## Format: `[ID] Description`
- **No [P] markers**: All infrastructure tasks are sequential
- All file paths are absolute and exact
- Tasks marked "Kevin performs" indicate manual deployment steps

## Repository Paths
- **Docker Configuration**: `/Users/kevin/github/northstar-funding/docker/`
- **Feature Specs**: `/Users/kevin/github/northstar-funding/specs/008-create-perplexica-self/`
- **Documentation**: `/Users/kevin/github/northstar-funding/northstar-notes/`

---

## Phase 3.1: Pre-Deployment Checks

### T001: Verify Mac Studio Infrastructure Ready
**Description**: Verify Mac Studio pre-deployment checklist before making any changes

**Actions**:
1. Verify Mac Studio accessible at 192.168.1.10:
   ```bash
   ping -c 3 192.168.1.10
   ```

2. Verify LM Studio running on Mac Studio:
   ```bash
   ssh macstudio "curl -s http://localhost:1234/v1/models || echo 'LM Studio not responding at port 1234'"
   ```

3. Verify Qwen2.5-0.5B-Instruct model loaded:
   ```bash
   ssh macstudio "curl -s http://localhost:1234/v1/models | jq '.data[].id' | grep qwen2.5-0.5b-instruct"
   ```

4. Verify current Docker services healthy:
   ```bash
   ssh macstudio "docker ps --format 'table {{.Names}}\t{{.Status}}'"
   ```
   Expected: northstar-postgres (healthy), qdrant (healthy), northstar-pgadmin (healthy), searxng (may be unhealthy - we'll fix this)

**Success Criteria**:
- ✅ Mac Studio responding to SSH
- ✅ LM Studio API responding at port 1234
- ✅ qwen2.5-0.5b-instruct model loaded
- ✅ PostgreSQL, Qdrant, pgAdmin containers healthy

**Constitutional Compliance**: Principle VIII - Verification only, no deployment actions

**Blocks**: T002

---

### T002: Backup Current Docker Configuration
**Description**: Create backup of docker/ directory before making changes

**Actions**:
1. Create backup of docker-compose.yml:
   ```bash
   cp /Users/kevin/github/northstar-funding/docker/docker-compose.yml \
      /Users/kevin/github/northstar-funding/docker/docker-compose.yml.backup-$(date +%Y%m%d-%H%M%S)
   ```

2. Create backup of searxng/settings.yml:
   ```bash
   cp /Users/kevin/github/northstar-funding/docker/searxng/settings.yml \
      /Users/kevin/github/northstar-funding/docker/searxng/settings.yml.backup-$(date +%Y%m%d-%H%M%S)
   ```

3. Verify backups created:
   ```bash
   ls -lh /Users/kevin/github/northstar-funding/docker/*.backup-*
   ls -lh /Users/kevin/github/northstar-funding/docker/searxng/*.backup-*
   ```

**Success Criteria**:
- ✅ docker-compose.yml backup exists
- ✅ settings.yml backup exists
- ✅ Backups have recent timestamps

**Constitutional Compliance**: Principle VIII - Local file operations only, no deployment

**Blocks**: T003

---

## Phase 3.2: Configuration Updates (MacBook M2)

### T003: Fix SearXNG Configuration (brand section)
**Description**: Fix SearXNG settings.yml brand section to use empty strings instead of boolean false

**File**: `/Users/kevin/github/northstar-funding/docker/searxng/settings.yml`

**Actions**:
1. Open settings.yml in editor
2. Locate brand section (around line 30)
3. Replace boolean false values with empty strings:
   ```yaml
   # BEFORE:
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

4. Validate YAML syntax:
   ```bash
   python3 -c "import yaml; yaml.safe_load(open('/Users/kevin/github/northstar-funding/docker/searxng/settings.yml'))"
   ```

**Success Criteria** (FR-001):
- ✅ All brand section values are empty strings ""
- ✅ No boolean false values remain
- ✅ YAML syntax validation passes
- ✅ All 5 brand fields present (new_issue_url, docs_url, issue_url, public_instances, wiki_url)

**Constitutional Compliance**: Configuration file edit on local MacBook M2

**Blocks**: T006

---

### T004: Add Perplexica Service to docker-compose.yml
**Description**: Add Perplexica container service definition to docker-compose.yml

**File**: `/Users/kevin/github/northstar-funding/docker/docker-compose.yml`

**Actions**:
1. Open docker-compose.yml in editor
2. Locate searxng service definition
3. Add Perplexica service after searxng service (before closing services section):
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

4. Validate YAML syntax:
   ```bash
   docker compose -f /Users/kevin/github/northstar-funding/docker/docker-compose.yml config > /dev/null
   ```

**Success Criteria** (FR-004, FR-006, FR-007):
- ✅ Perplexica service definition added
- ✅ Port 3000 exposed
- ✅ SearXNG_API_URL environment variable set
- ✅ depends_on searxng with service_healthy condition
- ✅ healthcheck configured for /api/health endpoint
- ✅ YAML syntax validation passes

**Constitutional Compliance**: Configuration file edit on local MacBook M2

**Blocks**: T006

---

### T005: Add Perplexica Docker Volumes to docker-compose.yml
**Description**: Add Perplexica volume definitions to docker-compose.yml volumes section

**File**: `/Users/kevin/github/northstar-funding/docker/docker-compose.yml`

**Actions**:
1. Open docker-compose.yml in editor
2. Locate volumes section at bottom of file
3. Add two new volume definitions:
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

4. Validate YAML syntax:
   ```bash
   docker compose -f /Users/kevin/github/northstar-funding/docker/docker-compose.yml config > /dev/null
   ```

**Success Criteria** (FR-005):
- ✅ perplexica-data volume defined
- ✅ perplexica-uploads volume defined
- ✅ Volume names use northstar- prefix
- ✅ YAML syntax validation passes

**Constitutional Compliance**: Configuration file edit on local MacBook M2

**Blocks**: T006

---

### T006: Create Perplexica Configuration File
**Description**: Create Perplexica config.toml with LM Studio integration settings

**Directory**: `/Users/kevin/github/northstar-funding/docker/perplexica/` (create if not exists)
**File**: `/Users/kevin/github/northstar-funding/docker/perplexica/config.toml`

**Actions**:
1. Create perplexica directory:
   ```bash
   mkdir -p /Users/kevin/github/northstar-funding/docker/perplexica
   ```

2. Create config.toml with content:
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

3. Validate TOML syntax:
   ```bash
   python3 -c "import tomllib; tomllib.load(open('/Users/kevin/github/northstar-funding/docker/perplexica/config.toml', 'rb'))"
   ```

**Success Criteria** (FR-002, FR-003, FR-006):
- ✅ config.toml file created
- ✅ LM_STUDIO provider configured with host.docker.internal:1234
- ✅ CHAT_MODEL uses lm_studio provider
- ✅ qwen2.5-0.5b-instruct model specified
- ✅ SEARXNG endpoint set to http://searxng:8080
- ✅ TOML syntax validation passes

**Constitutional Compliance**: Configuration file creation on local MacBook M2

**Depends On**: T003, T004, T005
**Blocks**: T007

---

## Phase 3.3: Deployment (Kevin Performs)

### T007: Rsync Configuration to Mac Studio
**Description**: ⚠️ **KEVIN PERFORMS** - Transfer updated docker configuration to Mac Studio

**Constitutional Compliance**: **Principle VIII - Kevin manages all rsync operations to Mac Studio**

**Actions** (Kevin performs):
1. Rsync docker directory to Mac Studio:
   ```bash
   rsync -av /Users/kevin/github/northstar-funding/docker/ macstudio:~/northstar/
   ```

2. Verify files transferred:
   ```bash
   ssh macstudio "ls -lh ~/northstar/docker-compose.yml"
   ssh macstudio "ls -lh ~/northstar/searxng/settings.yml"
   ssh macstudio "ls -lh ~/northstar/perplexica/config.toml"
   ```

3. Verify file contents on Mac Studio:
   ```bash
   # Check SearXNG fix applied
   ssh macstudio "grep -A 5 'brand:' ~/northstar/searxng/settings.yml"

   # Check Perplexica service present
   ssh macstudio "grep -A 10 'perplexica:' ~/northstar/docker-compose.yml"

   # Check config.toml transferred
   ssh macstudio "cat ~/northstar/perplexica/config.toml"
   ```

**Success Criteria** (FR-013, FR-014):
- ✅ docker-compose.yml transferred with Perplexica service
- ✅ searxng/settings.yml transferred with brand fix
- ✅ perplexica/config.toml transferred
- ✅ File contents match local versions

**Constitutional Compliance**: **Kevin performs this task manually**

**Depends On**: T006
**Blocks**: T008

---

### T008: Deploy Perplexica Container on Mac Studio
**Description**: ⚠️ **KEVIN PERFORMS** - Stop containers, pull Perplexica image, restart all containers

**Constitutional Compliance**: **Principle VIII - Kevin manages all Docker container operations on Mac Studio**

**Actions** (Kevin performs):
1. SSH to Mac Studio:
   ```bash
   ssh macstudio
   cd ~/northstar
   ```

2. Stop all containers:
   ```bash
   docker compose down
   ```

3. Pull Perplexica image:
   ```bash
   docker compose pull perplexica
   ```

4. Start all containers:
   ```bash
   docker compose up -d
   ```

5. Check container status:
   ```bash
   docker ps --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}'
   ```

**Expected Output**:
```
NAMES                STATUS              PORTS
northstar-postgres   Up (healthy)        5432
northstar-pgadmin    Up (healthy)        5050
qdrant               Up (healthy)        6333-6334
searxng              Up (healthy)        8080
perplexica           Up (healthy)        3000
```

**Success Criteria**:
- ✅ All 5 containers running
- ✅ All containers show healthy status
- ✅ Perplexica container started successfully
- ✅ SearXNG no longer restarting (brand fix applied)

**Constitutional Compliance**: **Kevin performs this task manually**

**Depends On**: T007
**Blocks**: T009

---

## Phase 3.4: Verification

### T009: Verify Container Health and Logs
**Description**: Run automated health checks on all containers

**Reference**: See `/Users/kevin/github/northstar-funding/specs/008-create-perplexica-self/verification.md` Phase 1 & 2

**Actions**:
1. Check all container status:
   ```bash
   ssh macstudio "docker ps --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}'"
   ```

2. Check Perplexica detailed health:
   ```bash
   ssh macstudio "docker inspect perplexica | jq '.[0].State.Health'"
   ```

3. Check SearXNG logs for errors (should be none):
   ```bash
   ssh macstudio "docker logs searxng --tail 50 | grep -i error"
   ```

4. Check Perplexica startup logs:
   ```bash
   ssh macstudio "docker logs perplexica --tail 50"
   ```

**Success Criteria** (FR-008, FR-009, FR-010):
- ✅ All 5 containers show "Up (healthy)" status
- ✅ Perplexica health status is "healthy"
- ✅ SearXNG logs show no "ValueError: Invalid settings.yml" errors
- ✅ SearXNG logs show no restart loop warnings
- ✅ Perplexica logs show successful startup
- ✅ PostgreSQL, Qdrant, pgAdmin still healthy

**Constitutional Compliance**: Read-only verification, no deployment actions

**Depends On**: T008
**Blocks**: T010

---

### T010: Verify Perplexica API and UI Access
**Description**: Test Perplexica health endpoint and UI accessibility

**Reference**: See `/Users/kevin/github/northstar-funding/specs/008-create-perplexica-self/verification.md` Phase 3

**Actions**:
1. Test Perplexica health endpoint:
   ```bash
   curl -s http://192.168.1.10:3000/api/health | jq '.'
   ```
   Expected: `{"status": "ok", "timestamp": "..."}`

2. Test Perplexica UI loads (from MacBook M2 browser):
   ```
   http://192.168.1.10:3000
   ```
   Expected: Search interface loads, model shows "qwen2.5-0.5b-instruct"

3. Test Perplexica search API:
   ```bash
   curl -X POST http://192.168.1.10:3000/api/search \
     -H "Content-Type: application/json" \
     -d '{"query": "test search", "focus_mode": "webSearch"}' | jq '.status'
   ```
   Expected: `"success"`

**Success Criteria** (FR-011, FR-012):
- ✅ Health endpoint returns HTTP 200 with status "ok"
- ✅ Perplexica UI accessible at port 3000
- ✅ Search interface visible in browser
- ✅ Model selector shows qwen2.5-0.5b-instruct
- ✅ Search API returns successful response

**Constitutional Compliance**: Read-only verification, no deployment actions

**Depends On**: T009
**Blocks**: T011

---

### T011: Verify LM Studio and SearXNG Integration
**Description**: Test Perplexica integration with LM Studio and SearXNG

**Reference**: See `/Users/kevin/github/northstar-funding/specs/008-create-perplexica-self/verification.md` Phase 3.4 & 3.5

**Actions**:
1. Verify Perplexica can reach LM Studio:
   ```bash
   ssh macstudio "docker exec perplexica curl -s http://host.docker.internal:1234/v1/models | jq '.data[0].id'"
   ```
   Expected: `"qwen2.5-0.5b-instruct"`

2. Verify Perplexica can reach SearXNG:
   ```bash
   ssh macstudio "docker exec perplexica curl -s http://searxng:8080/search?q=test\&format=json | jq '.results | length'"
   ```
   Expected: Number of search results (e.g., 10)

3. Run end-to-end search test:
   ```bash
   curl -X POST http://192.168.1.10:3000/api/search \
     -H "Content-Type: application/json" \
     -d '{"query": "EU Horizon grants educational infrastructure Bulgaria", "focus_mode": "webSearch"}' \
     | jq '.answer' | head -20
   ```
   Expected: AI-generated answer about EU funding

**Success Criteria** (FR-003, FR-006):
- ✅ Perplexica can access LM Studio at host.docker.internal:1234
- ✅ LM Studio returns model list with qwen2.5-0.5b-instruct
- ✅ Perplexica can access SearXNG at searxng:8080
- ✅ SearXNG returns search results
- ✅ End-to-end search returns AI answer and web results

**Constitutional Compliance**: Read-only verification, no deployment actions

**Depends On**: T010
**Blocks**: T012

---

## Phase 3.5: Documentation & Completion

### T012: Update Infrastructure Documentation
**Description**: Update docker/README.md and project documentation with Perplexica deployment

**Files**:
- `/Users/kevin/github/northstar-funding/docker/README.md`
- `/Users/kevin/github/northstar-funding/northstar-notes/technology/docker-infrastructure-workflow.md`

**Actions**:
1. Update docker/README.md with Perplexica section:
   - Service description: "Perplexica v1.11.2 - AI-powered search engine"
   - Access URL: http://192.168.1.10:3000
   - Configuration: LM Studio integration via host.docker.internal:1234
   - Dependencies: SearXNG, LM Studio

2. Verify docker-infrastructure-workflow.md already updated:
   ```bash
   grep -A 10 "Perplexica" /Users/kevin/github/northstar-funding/northstar-notes/technology/docker-infrastructure-workflow.md
   ```

3. Create deployment completion marker:
   ```bash
   echo "$(date): Feature 008 Perplexica deployment completed successfully" >> \
     /Users/kevin/github/northstar-funding/specs/008-create-perplexica-self/deployment-log.md
   ```

**Success Criteria** (FR-015):
- ✅ docker/README.md updated with Perplexica documentation
- ✅ Service URLs documented
- ✅ Configuration details documented
- ✅ Deployment completion logged

**Constitutional Compliance**: Documentation updates only

**Depends On**: T011
**Blocks**: None (final task)

---

## Dependencies Graph

```
T001 (Pre-deployment checks)
  ↓
T002 (Backup configuration)
  ↓
T003 (Fix SearXNG config)
  ↓
T004 (Add Perplexica service)
  ↓
T005 (Add Perplexica volumes)
  ↓
T006 (Create config.toml)
  ↓
T007 (Kevin: Rsync to Mac Studio) ⚠️
  ↓
T008 (Kevin: Deploy containers) ⚠️
  ↓
T009 (Verify container health)
  ↓
T010 (Verify API and UI)
  ↓
T011 (Verify integrations)
  ↓
T012 (Update documentation)
```

**All tasks are sequential** - no parallel execution for infrastructure deployment

---

## Parallel Execution

**N/A - Infrastructure deployment requires sequential execution**

Infrastructure tasks have strict dependencies:
- Configuration must be complete before deployment
- Deployment must succeed before verification
- Verification must pass before documentation

**Constitutional Principle VIII**: Kevin performs all deployment operations (T007, T008) manually.

---

## Rollback Procedure

If any task T001-T012 fails, follow rollback procedure from `/Users/kevin/github/northstar-funding/specs/008-create-perplexica-self/deployment.md`:

1. **Stop Perplexica** (if started):
   ```bash
   ssh macstudio "docker compose stop perplexica"
   ```

2. **Restore backup docker-compose.yml**:
   ```bash
   cp /Users/kevin/github/northstar-funding/docker/docker-compose.yml.backup-* \
      /Users/kevin/github/northstar-funding/docker/docker-compose.yml
   ```

3. **Rsync restored config** (Kevin performs):
   ```bash
   rsync -av /Users/kevin/github/northstar-funding/docker/ macstudio:~/northstar/
   ```

4. **Restart containers** (Kevin performs):
   ```bash
   ssh macstudio "cd ~/northstar && docker compose up -d"
   ```

5. **Verify existing services healthy**:
   ```bash
   ssh macstudio "docker ps"
   ```

---

## Success Metrics

### Configuration (T003-T006)
- ✅ SearXNG brand section uses empty strings (FR-001)
- ✅ Perplexica service definition complete (FR-004)
- ✅ Perplexica volumes defined (FR-005)
- ✅ config.toml with LM Studio provider (FR-002, FR-006)

### Deployment (T007-T008)
- ✅ All files transferred to Mac Studio (FR-013, FR-014)
- ✅ All 5 containers running and healthy (FR-004, FR-007)
- ✅ SearXNG no restart loops (FR-009)

### Verification (T009-T011)
- ✅ Perplexica health check passing (FR-008)
- ✅ Perplexica UI accessible (FR-011)
- ✅ Perplexica API responding (FR-012)
- ✅ LM Studio integration working (FR-006)
- ✅ SearXNG integration working (FR-003)
- ✅ Existing services unaffected (FR-010)

### Documentation (T012)
- ✅ Infrastructure documentation updated (FR-015)

---

## Notes

### Constitutional Compliance
- **Principle VIII**: Tasks T007 and T008 are marked "Kevin performs" - AI never executes deployment
- **Principle IV**: Uses existing Mac Studio infrastructure (Docker, LM Studio, PostgreSQL, Qdrant)
- **Principle VI**: Adds 5th service (Perplexica) to replace external Tavily API dependency

### Infrastructure Pattern
- All configuration changes on MacBook M2 (T003-T006)
- Kevin performs rsync and deployment (T007-T008)
- AI performs automated verification (T009-T011)
- AI updates documentation (T012)

### No Code Implementation
- This is infrastructure deployment, not application code
- No Java code changes
- No database migrations
- No unit tests required
- Manual verification via health checks and API testing

### Performance Expectations
- Container startup: <30 seconds
- Perplexica API response: 3-10 seconds
- Perplexica UI load: <2 seconds
- LM Studio token generation: 200-300 tokens/sec

---

## Validation Checklist

- [x] All configuration files have tasks (settings.yml, docker-compose.yml, config.toml)
- [x] All deployment steps covered (backup, config, rsync, deploy, verify)
- [x] All verification procedures included (health checks, API tests, integration tests)
- [x] Tasks follow sequential order (config → deploy → verify → document)
- [x] Kevin's deployment responsibilities clearly marked (T007, T008)
- [x] Each task specifies exact file paths
- [x] Success criteria map to functional requirements (FR-001 through FR-015)
- [x] Rollback procedure documented

---

**Tasks Generation Complete** ✅

**Ready for Execution**: Task T001 can begin immediately after user approval

**Next**: Run `/implement` or execute tasks manually following deployment.md guide
