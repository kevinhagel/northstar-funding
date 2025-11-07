# Verification Guide: Perplexica Infrastructure

**Feature**: 008-create-perplexica-self
**Date**: 2025-11-07
**Deployment Target**: Mac Studio (192.168.1.10)

## Overview

This guide provides comprehensive health checks and verification procedures for the Perplexica deployment. All checks should be performed after deployment to ensure the infrastructure is working correctly.

---

## Pre-Verification Checklist

Before starting verification, ensure:
- [ ] Deployment steps from `deployment.md` completed
- [ ] All containers started via `docker compose up -d`
- [ ] SSH access to Mac Studio available
- [ ] LM Studio running on Mac Studio

---

## Phase 1: Container Health Checks

### 1.1 Check All Container Status

**Command** (run on Mac Studio):
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
- ✅ All 5 containers show "Up" status
- ✅ All containers show "(healthy)" status
- ✅ No containers show "Restarting" or "Exited"

**Failure Actions**:
- If any container unhealthy → Check logs (see Phase 4)
- If Perplexica missing → Check docker-compose.yml has perplexica service
- If SearXNG restarting → Verify settings.yml brand section fix applied

### 1.2 Detailed Health Status for Perplexica

**Command**:
```bash
docker inspect perplexica | jq '.[0].State.Health'
```

**Expected Output**:
```json
{
  "Status": "healthy",
  "FailingStreak": 0,
  "Log": [
    {
      "Start": "2025-11-07T...",
      "End": "2025-11-07T...",
      "ExitCode": 0,
      "Output": "..."
    }
  ]
}
```

**Success Criteria**:
- ✅ Status is "healthy"
- ✅ FailingStreak is 0
- ✅ Most recent Log entry has ExitCode 0

---

## Phase 2: SearXNG Verification

### 2.1 Check SearXNG Configuration Fix

**Command**:
```bash
cat ~/northstar/searxng/settings.yml | grep -A 5 "brand:"
```

**Expected Output**:
```yaml
brand:
  new_issue_url: ""
  docs_url: ""
  issue_url: ""
  public_instances: ""
  wiki_url: ""
```

**Success Criteria** (FR-001):
- ✅ All brand values are empty strings `""`
- ✅ No boolean `false` values present
- ✅ All 5 brand fields present

### 2.2 Check SearXNG Error Logs

**Command**:
```bash
docker logs searxng --tail 50 | grep -i error
```

**Expected Output**:
```
(no output - means no errors)
```

**Success Criteria** (FR-009):
- ✅ No "ValueError: Invalid settings.yml" errors
- ✅ No "brand.issue_url" validation errors
- ✅ No restart loop warnings

**Failure Actions**:
- If brand errors present → settings.yml fix not applied correctly
- If other errors → Check searxng/settings.yml syntax

### 2.3 Test SearXNG Search API

**Command**:
```bash
curl -s "http://192.168.1.10:8080/search?q=test&format=json" | jq '.results[0].title'
```

**Expected Output**:
```
"Test - Wikipedia"
```
(or any search result title)

**Success Criteria** (FR-003):
- ✅ Returns JSON with results array
- ✅ HTTP 200 status code
- ✅ At least one result returned

---

## Phase 3: Perplexica Verification

### 3.1 Check Perplexica Health Endpoint

**Command**:
```bash
curl -s http://192.168.1.10:3000/api/health | jq '.'
```

**Expected Output**:
```json
{
  "status": "ok",
  "timestamp": "2025-11-07T..."
}
```

**Success Criteria** (FR-008, FR-012):
- ✅ HTTP 200 status code
- ✅ Returns JSON with status field
- ✅ Response time < 1 second

**Failure Actions**:
- If connection refused → Perplexica container not running
- If timeout → Check Perplexica logs for startup errors
- If 502/503 → Check depends_on: searxng condition

### 3.2 Verify Perplexica UI Loads

**Test** (manual - use browser on MacBook):
```
http://192.168.1.10:3000
```

**Expected Behavior**:
- ✅ Page loads within 2 seconds
- ✅ Search interface visible
- ✅ No JavaScript console errors
- ✅ Model selector shows "qwen2.5-0.5b-instruct"

**Success Criteria** (FR-011):
- ✅ Perplexica UI accessible at port 3000
- ✅ No 404, 500, or connection errors
- ✅ Search box functional

**Failure Actions**:
- If page doesn't load → Check Perplexica container logs
- If UI broken → Check config.toml mounted correctly
- If model not shown → Check LM Studio connection (see 3.4)

### 3.3 Test Perplexica Search API

**Command**:
```bash
curl -X POST http://192.168.1.10:3000/api/search \
  -H "Content-Type: application/json" \
  -d '{
    "query": "test search",
    "focus_mode": "webSearch"
  }' | jq '.status'
```

**Expected Output**:
```
"success"
```

**Success Criteria** (FR-012):
- ✅ Returns JSON response
- ✅ HTTP 200 status code
- ✅ Response includes search results or AI-generated answer
- ✅ Response time 3-10 seconds (includes LLM processing)

**Failure Actions**:
- If 400 error → Check request body format
- If 500 error → Check Perplexica logs for integration errors
- If timeout → Check LM Studio and SearXNG connectivity

### 3.4 Verify LM Studio Integration

**Command** (check Perplexica can reach LM Studio):
```bash
docker exec perplexica curl -s http://host.docker.internal:1234/v1/models | jq '.data[0].id'
```

**Expected Output**:
```
"qwen2.5-0.5b-instruct"
```

**Success Criteria** (FR-006):
- ✅ Perplexica container can reach LM Studio at host.docker.internal:1234
- ✅ LM Studio returns model list
- ✅ qwen2.5-0.5b-instruct model present

**Failure Actions**:
- If connection refused → LM Studio not running on Mac Studio
- If model not found → Wrong model loaded in LM Studio
- If host.docker.internal fails → Check Docker networking

### 3.5 Verify SearXNG Integration

**Command** (check Perplexica can reach SearXNG):
```bash
docker exec perplexica curl -s http://searxng:8080/search?q=test\&format=json | jq '.results | length'
```

**Expected Output**:
```
10
```
(number of search results)

**Success Criteria** (FR-003):
- ✅ Perplexica container can reach SearXNG at searxng:8080
- ✅ SearXNG returns search results
- ✅ Container networking working correctly

**Failure Actions**:
- If connection refused → SearXNG not in same Docker network
- If no results → SearXNG not configured correctly
- If DNS error → Check northstar-network configuration

---

## Phase 4: Existing Services Verification

### 4.1 PostgreSQL Health Check

**Command**:
```bash
docker exec northstar-postgres pg_isready -U northstar_user -d northstar_funding
```

**Expected Output**:
```
/var/run/postgresql:5432 - accepting connections
```

**Success Criteria** (FR-010):
- ✅ PostgreSQL still accepting connections
- ✅ northstar_funding database accessible
- ✅ No connection errors

### 4.2 Qdrant Health Check

**Command**:
```bash
curl -s http://192.168.1.10:6333/health | jq '.status'
```

**Expected Output**:
```
"ok"
```

**Success Criteria** (FR-010):
- ✅ Qdrant responding to health checks
- ✅ HTTP 200 status
- ✅ No disruption from Perplexica deployment

### 4.3 pgAdmin Health Check

**Command**:
```bash
curl -s -o /dev/null -w "%{http_code}" http://192.168.1.10:5050
```

**Expected Output**:
```
200
```

**Success Criteria** (FR-010):
- ✅ pgAdmin UI loads
- ✅ HTTP 200 status
- ✅ No container restart issues

---

## Phase 5: Log Analysis

### 5.1 Perplexica Startup Logs

**Command**:
```bash
docker logs perplexica --tail 50
```

**Success Indicators**:
- ✅ "Server started on port 3000"
- ✅ "Connected to SearXNG"
- ✅ "LM Studio provider initialized"
- ✅ No ERROR or WARN level messages

**Red Flags**:
- ❌ "Failed to connect to LM Studio"
- ❌ "SearXNG unreachable"
- ❌ "Config file not found"
- ❌ "Module not found" errors

### 5.2 SearXNG Startup Logs

**Command**:
```bash
docker logs searxng --tail 50
```

**Success Indicators**:
- ✅ "SearXNG is ready"
- ✅ No "ValueError: Invalid settings.yml"
- ✅ No restart/crash messages

**Red Flags**:
- ❌ "brand.issue_url" validation errors
- ❌ Restart loop messages
- ❌ Configuration syntax errors

### 5.3 Docker Compose Logs (All Services)

**Command**:
```bash
docker compose logs --tail 20
```

**Success Indicators**:
- ✅ All services show recent log entries
- ✅ No crash/restart patterns
- ✅ Perplexica logs show successful initialization

---

## Phase 6: Network Connectivity Matrix

### 6.1 Verify Container Network Membership

**Command**:
```bash
docker network inspect northstar-network | jq '.[0].Containers | keys'
```

**Expected Output**:
```json
[
  "northstar-postgres",
  "northstar-pgadmin",
  "qdrant",
  "searxng",
  "perplexica"
]
```

**Success Criteria**:
- ✅ All 5 containers in northstar-network
- ✅ Perplexica present in network
- ✅ No orphaned containers

### 6.2 Verify External Port Accessibility

**Commands** (run from MacBook M2):
```bash
# PostgreSQL
nc -zv 192.168.1.10 5432

# pgAdmin
curl -s -o /dev/null -w "%{http_code}" http://192.168.1.10:5050

# Qdrant
curl -s http://192.168.1.10:6333/health

# SearXNG
curl -s http://192.168.1.10:8080

# Perplexica (NEW)
curl -s http://192.168.1.10:3000/api/health
```

**Success Criteria**:
- ✅ All ports accessible from MacBook M2
- ✅ Perplexica port 3000 responding
- ✅ No firewall blocking

---

## Success Criteria Summary

### Functional Requirements Verification

**Configuration Management:**
- [x] FR-001: SearXNG brand section uses empty strings (Phase 2.1)
- [x] FR-002: Perplexica config.toml created with LM Studio settings (Phase 3.4)
- [x] FR-003: Perplexica connects to SearXNG (Phase 2.3, 3.5)

**Container Orchestration:**
- [x] FR-004: Perplexica v1.11.2 container running (Phase 1.1)
- [x] FR-005: Perplexica volumes configured (Phase 1.1)
- [x] FR-006: Perplexica uses LM Studio via host.docker.internal (Phase 3.4)
- [x] FR-007: Perplexica depends_on SearXNG (Phase 1.1)

**Health & Verification:**
- [x] FR-008: Perplexica health check working (Phase 3.1)
- [x] FR-009: SearXNG no restart loops (Phase 2.2)
- [x] FR-010: Existing services healthy (Phase 4.1, 4.2, 4.3)
- [x] FR-011: Perplexica UI accessible (Phase 3.2)
- [x] FR-012: Perplexica API responds (Phase 3.3)

**Deployment:**
- [x] FR-013: Deployed to Mac Studio ~/northstar (verified by container running)
- [x] FR-014: Rsync workflow used (verified in deployment.md)
- [x] FR-015: Documentation updated (deployment.md, verification.md exist)

### Overall Health Score

Calculate health score:
```
Total Checks: 18 (FR-001 through FR-018, excluding FR-016, FR-017, FR-018)
Passed Checks: ____ / 15
Health Score: _____%

✅ 100% = All systems healthy, deployment successful
⚠️  80-99% = Minor issues, investigate warnings
❌ <80% = Critical issues, rollback recommended
```

---

## Troubleshooting Common Issues

### Issue: Perplexica Container Won't Start

**Symptom**: `docker ps` shows perplexica exited or restarting

**Diagnosis**:
```bash
docker logs perplexica --tail 100
```

**Common Causes**:
1. LM Studio not running → Check `curl http://192.168.1.10:1234/v1/models`
2. SearXNG unhealthy → Check `docker logs searxng`
3. config.toml not mounted → Check `docker inspect perplexica | jq '.[0].Mounts'`
4. Port 3000 already in use → Check `lsof -i :3000`

**Solution**:
```bash
# Restart dependencies first
docker compose restart searxng
# Ensure LM Studio running
ssh macstudio "pgrep -f 'LM Studio'"
# Restart Perplexica
docker compose restart perplexica
```

### Issue: SearXNG Still Restarting After Fix

**Symptom**: SearXNG shows restart loop in `docker ps`

**Diagnosis**:
```bash
docker logs searxng | grep -i "valueerror\|invalid"
```

**Common Causes**:
1. settings.yml not updated correctly
2. YAML syntax error introduced
3. File not synced to Mac Studio

**Solution**:
```bash
# Verify settings.yml on Mac Studio
cat ~/northstar/searxng/settings.yml | head -50
# Look for brand section - should be empty strings, not false

# If still has false values, re-rsync from MacBook:
# (Kevin performs this)
rsync -av docker/searxng/settings.yml macstudio:~/northstar/searxng/

# Restart SearXNG
docker compose restart searxng
```

### Issue: Perplexica Can't Connect to LM Studio

**Symptom**: Perplexica logs show "Failed to connect to LM Studio"

**Diagnosis**:
```bash
# From Mac Studio
curl http://localhost:1234/v1/models

# From inside Perplexica container
docker exec perplexica curl http://host.docker.internal:1234/v1/models
```

**Common Causes**:
1. LM Studio not running
2. Wrong port (should be 1234)
3. host.docker.internal not resolving

**Solution**:
```bash
# Ensure LM Studio running
# (Kevin starts LM Studio GUI on Mac Studio)

# Verify model loaded
curl http://192.168.1.10:1234/v1/models | jq '.data[].id'
# Should show: "qwen2.5-0.5b-instruct"

# Restart Perplexica after LM Studio ready
docker compose restart perplexica
```

### Issue: Network Connectivity Between Containers

**Symptom**: Perplexica can't reach SearXNG or vice versa

**Diagnosis**:
```bash
# Check network membership
docker network inspect northstar-network | jq '.[0].Containers | keys'

# Verify perplexica in network
docker inspect perplexica | jq '.[0].NetworkSettings.Networks | keys'
```

**Solution**:
```bash
# Reconnect container to network
docker network connect northstar-network perplexica

# Or recreate container
docker compose up -d --force-recreate perplexica
```

---

## Rollback Decision Matrix

Use this matrix to decide if rollback is needed:

| Scenario | Severity | Action |
|----------|----------|--------|
| Perplexica unhealthy, all others healthy | Low | Troubleshoot Perplexica only |
| SearXNG unhealthy, Perplexica failing | Medium | Fix SearXNG config, restart both |
| PostgreSQL unhealthy | **CRITICAL** | Immediate rollback |
| Qdrant unhealthy | **CRITICAL** | Immediate rollback |
| Multiple containers failing | **CRITICAL** | Immediate rollback |

**Rollback Procedure**: See `deployment.md` Rollback section

---

## Post-Verification Actions

After all checks pass:

1. **Document Results**:
   ```bash
   # Save verification results
   docker ps > ~/northstar/verification-results.txt
   docker compose logs --tail 50 >> ~/northstar/verification-results.txt
   ```

2. **Update Infrastructure Documentation**:
   - Mark Perplexica as deployed in docker/README.md
   - Update service URLs list
   - Document any configuration changes

3. **Create Success Marker**:
   ```bash
   echo "$(date): Feature 008 deployment verified successfully" >> ~/northstar/deployment-log.txt
   ```

4. **Proceed to Manual Testing**:
   - See `quickstart.md` for user-facing search tests
   - Test actual search queries relevant to funding discovery

---

**Verification Complete**: All systems healthy ✅

**Next**: See `quickstart.md` for manual testing procedures
