# Feature 008: Perplexica Integration - Scope Analysis

**Date**: 2025-11-07
**Status**: Pre-Planning (Scope Definition)
**Goal**: Replace Tavily API with self-hosted Perplexica AI search

---

## Critical Discovery: SearXNG Integration Options

### Perplexica Offers TWO Deployment Models:

**1. Full Image (Bundled SearXNG)**
- Image: `itzcrazykns1337/perplexica:latest`
- Includes: Perplexica + SearXNG (bundled together)
- Pros: Simple, everything in one container
- Cons: Duplicate SearXNG (we already have one)

**2. Slim Image (External SearXNG)**
- Image: `itzcrazykns1337/perplexica:slim` (assumed naming)
- Requires: External SearXNG via `SEARXNG_API_URL` env var
- Pros: **Uses our existing SearXNG!** No duplication
- Cons: Must configure connection

### Recommended Approach: Use Existing SearXNG ✅

**Configuration:**
```yaml
perplexica:
  image: itzcrazykns1337/perplexica:latest  # v1.11.2 (latest)
  environment:
    - SEARXNG_API_URL=http://searxng:8080  # Point to OUR SearXNG
  volumes:
    - ./perplexica/config.toml:/home/perplexica/config.toml:ro
```

**Note**: LM Studio is configured via config.toml, not environment variables.

**Benefits:**
- Reuses existing SearXNG (port 8080)
- No duplicate services
- Constitutional compliance (stay under 5 services)
- Shares SearXNG configuration we already maintain

---

## Feature 008 Scope Options

### Option A: Minimal Scope (Recommended)
**Focus**: Infrastructure only - get Perplexica running

**Phases:**
1. **Infrastructure Setup**
   - Fix SearXNG configuration error (brand section)
   - Add Perplexica to docker-compose.yml
   - Configure Perplexica to use existing SearXNG
   - Configure Perplexica to use LM Studio
   - Deploy and verify on Mac Studio

2. **Verification**
   - Test Perplexica UI (http://192.168.1.10:3000)
   - Test Perplexica API endpoint
   - Verify SearXNG integration
   - Verify LM Studio integration

**Tasks (6-8):**
- Fix SearXNG settings.yml (brand section)
- Add Perplexica service to docker-compose.yml
- Create Perplexica config.toml
- Update docker README
- Rsync to Mac Studio
- Deploy on Mac Studio
- Test Perplexica UI
- Test Perplexica API

**Timeline**: 1-2 days
**Complexity**: Low
**Risk**: Low (infrastructure only)

---

### Option B: Full Integration Scope
**Focus**: Infrastructure + Java integration + Replace Tavily

**Phases:**
1. **Infrastructure Setup** (same as Option A)
2. **Java Integration**
   - Create PerplexicaSearchService
   - Add Perplexica API client (WebClient)
   - Implement search method with focus modes
   - Add configuration properties
3. **Testing**
   - Unit tests for PerplexicaSearchService
   - Integration tests comparing Perplexica vs Tavily
   - Performance benchmarking
4. **Migration**
   - Switch from Tavily to Perplexica
   - Update configuration
   - Remove Tavily dependency (if not needed)
   - Update documentation

**Tasks (15-20):**
- All from Option A (8 tasks)
- Create PerplexicaSearchService interface/impl
- Create Perplexica API models (request/response)
- Add WebClient configuration for Perplexica
- Create PerplexicaProperties configuration
- Write unit tests (mock WebClient)
- Write integration tests (real Perplexica)
- Performance comparison tests
- Update SearchEngineType enum (add PERPLEXICA)
- Switch configuration from Tavily to Perplexica
- Update documentation
- Remove Tavily API key from config (optional)

**Timeline**: 5-7 days
**Complexity**: Medium
**Risk**: Medium (code changes, testing needed)

---

### Option C: Minimal + Parallel Testing
**Focus**: Infrastructure + Side-by-side comparison

**Phases:**
1. **Infrastructure Setup** (same as Option A)
2. **Java Integration** (minimal)
   - Create PerplexicaSearchService
   - Keep TavilySearchService
   - Add configuration to switch between them
3. **Comparison Testing**
   - Run 100 test queries through both
   - Compare quality, speed, cost
   - Document findings
4. **Decision Point**
   - If Perplexica ≥90% quality → migrate fully
   - If Perplexica <90% quality → keep Tavily for now

**Tasks (10-12):**
- All from Option A (8 tasks)
- Create PerplexicaSearchService (basic)
- Add search provider configuration (Tavily vs Perplexica)
- Create comparison test suite
- Run comparison tests
- Document results
- Decision: migrate or defer

**Timeline**: 3-4 days
**Complexity**: Low-Medium
**Risk**: Low (keep both options available)

---

## Recommendation: Option A (Minimal Scope)

### Why Start Minimal?

**1. Clear Separation of Concerns**
- Feature 008: Infrastructure (Docker, Perplexica deployment)
- Feature 009: Java Integration (PerplexicaSearchService)
- Feature 010: Migration (Replace Tavily)

**2. Faster Feedback**
- Get Perplexica running quickly
- Test it manually before coding
- Validate it meets needs before committing to integration

**3. Constitutional Compliance**
- Each feature focused on ONE thing
- Easier to review and test
- Clear rollback points

**4. Risk Mitigation**
- If Perplexica doesn't work well, minimal wasted effort
- If configuration is tricky, we learn before Java integration
- Can test thoroughly before changing Java code

### Feature 008 Scope (Final Recommendation)

**Goal**: Deploy Perplexica on Mac Studio, integrated with existing SearXNG and LM Studio

**Phases:**

**Phase 1: Configuration Fixes**
1. Fix SearXNG settings.yml (brand section - strings not false)
2. Test SearXNG works after fix
3. Document SearXNG configuration

**Phase 2: Perplexica Deployment**
1. Add Perplexica to docker-compose.yml
2. Create Perplexica config.toml
3. Configure to use existing SearXNG (http://searxng:8080)
4. Configure to use LM Studio (http://host.docker.internal:1234 via LM_STUDIO provider)
5. Update docker README with Perplexica info

**Phase 3: Deployment & Verification**
1. Rsync docker/ to Mac Studio ~/northstar
2. Deploy on Mac Studio (docker compose up -d)
3. Verify all containers healthy
4. Test Perplexica UI (http://192.168.1.10:3000)
5. Test Perplexica API (/api/search endpoint)
6. Document test results

**Success Criteria:**
- ✅ SearXNG working (no restart loop)
- ✅ Perplexica container running and healthy
- ✅ Perplexica UI accessible
- ✅ Perplexica API responding
- ✅ Search queries return results with citations
- ✅ All existing services still working (PostgreSQL, Qdrant, LM Studio)

**Out of Scope (Defer to Future Features):**
- ❌ Java integration (Feature 009)
- ❌ Replacing Tavily in code (Feature 010)
- ❌ Performance testing
- ❌ Production migration

---

## Tasks Breakdown for Feature 008

### Infrastructure Tasks (8 total)

**Task 1: Fix SearXNG Configuration**
- Edit `docker/searxng/settings.yml`
- Change `brand` section values from `false` to `""` (empty strings)
- Or remove `brand` section entirely
- Document the fix in docker README

**Task 2: Add Perplexica to docker-compose.yml**
- Add `perplexica` service definition
- Configure environment variables (SEARXNG_API_URL, OLLAMA_API_URL)
- Add volumes (perplexica-data, perplexica-uploads)
- Add health check
- Add to northstar-network
- Add volume definitions at bottom

**Task 3: Create Perplexica Configuration**
- Create `docker/perplexica/` directory
- Create `config.toml` with:
  - SearXNG endpoint (http://searxng:8080)
  - LM Studio endpoint (http://host.docker.internal:1234/v1)
  - Model selection (qwen2.5-0.5b-instruct)
  - Focus modes configuration

**Task 4: Update Docker Documentation**
- Update `docker/README.md`
- Document Perplexica service
- Document SearXNG integration
- Add troubleshooting section

**Task 5: Deploy to Mac Studio**
- Rsync `docker/` to `macstudio:~/northstar/`
- SSH to Mac Studio
- Run `docker compose down`
- Run `docker compose up -d`
- Check container status

**Task 6: Verify SearXNG Fixed**
- Check SearXNG container logs (no errors)
- Test SearXNG UI (http://192.168.1.10:8080)
- Perform test search
- Verify JSON API enabled

**Task 7: Verify Perplexica Deployment**
- Check Perplexica container logs
- Verify connection to SearXNG
- Verify connection to LM Studio
- Check health status

**Task 8: Test Perplexica Functionality**
- Test UI (http://192.168.1.10:3000)
- Test API endpoint with curl
- Try different focus modes
- Verify citations in results
- Document performance (latency)

---

## Comparison to Feature 006 (Superpowers Brainstorming)

**Feature 006**: Used `/superpowers:brainstorming` which created detailed plan with all phases/tasks in plan.md

**Feature 008 Recommendation**: Use `/specify` normally, let `/plan` generate standard structure

**Why?**
- Feature 006 was complex (new taxonomy, multiple enums, migration)
- Feature 008 is simpler (infrastructure deployment)
- Standard spec-kit workflow is sufficient
- Brainstorming overhead not needed for straightforward infrastructure task

---

## Constitutional Compliance Check

### Technology Stack ✅
- Docker infrastructure (existing pattern)
- PostgreSQL (unchanged)
- SearXNG (reusing existing)
- Perplexica (new AI search, replaces external API)
- LM Studio (unchanged)

### Complexity Management ✅
**Service Count:**
- PostgreSQL (1)
- Qdrant (2)
- SearXNG (3)
- Perplexica (4) ← NEW
- pgAdmin (infrastructure, not counted)

**Still under 5 services!** ✅

### Human-AI Collaboration ✅
- Infrastructure deployed by Kevin (rsync + docker compose)
- Configuration reviewed before deployment
- Testing verifies functionality
- Manual decision point before Java integration

---

## Next Steps

### If We Proceed with Feature 008 (Minimal Scope):

1. **Create Feature Spec**
   ```bash
   /specify Create Perplexica self-hosted AI search infrastructure:
   Fix SearXNG configuration error (brand section strings), add
   Perplexica container to docker-compose.yml configured to use
   existing SearXNG and LM Studio, deploy to Mac Studio, verify
   all services healthy and Perplexica UI/API working. Defers
   Java integration to future feature.
   ```

2. **Generate Plan**
   ```bash
   /plan
   ```

3. **Review Tasks**
   ```bash
   /tasks
   ```

4. **Implement**
   ```bash
   /implement
   ```

### Estimated Timeline:
- Spec/Plan: 30 minutes
- Implementation: 2-3 hours
- Testing/Verification: 1-2 hours
- **Total**: Half day to full day

---

## Questions to Resolve Before Starting

### 1. SearXNG Requirements ✅
**Q**: Does our SearXNG have JSON format enabled?
**A**: Need to check `searxng/settings.yml` - likely yes (default)

**Q**: Does it need Wolfram Alpha engine?
**A**: Perplexica docs mention this - check if required or optional

### 2. Image Selection ✅
**Q**: Use `latest` or `slim`?
**A**: Use `latest` with `SEARXNG_API_URL` pointing to our SearXNG

### 3. Model Selection ✅
**Q**: Keep Qwen2.5-0.5B or upgrade to 3B?
**A**: Start with 0.5B (currently working), can upgrade in separate task

### 4. Testing Scope ✅
**Q**: Manual testing only or automated?
**A**: Feature 008 = manual testing, Feature 009 = automated tests

---

## Recommendation Summary

**Feature 008 Scope**: Minimal (Infrastructure Only)

**Phases**: 3
1. Configuration Fixes (SearXNG)
2. Perplexica Deployment
3. Verification & Testing

**Tasks**: 8

**Timeline**: Half day to 1 day

**Risk**: Low (infrastructure only, no code changes)

**Dependencies**: None (all prerequisites exist)

**Next Feature**: 009 - Java Integration (PerplexicaSearchService)

**Deferred**: Tavily replacement (Feature 010 or later)

---

**Ready to proceed with `/specify` for Feature 008?**
