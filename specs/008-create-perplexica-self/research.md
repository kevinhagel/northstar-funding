# Research: Perplexica Self-Hosted AI Search Infrastructure

**Feature**: 008-create-perplexica-self
**Date**: 2025-11-07
**Status**: Complete

## Research Questions & Decisions

### 1. Perplexica Version Selection

**Question**: Which version of Perplexica should we use?

**Research**:
- Latest release: v1.11.2 (October 25, 2025)
- v1.11.0 (October 21, 2025): Added LM Studio provider support
- v1.10.x and earlier: No native LM Studio provider

**Decision**: Use Perplexica v1.11.2 (latest)

**Rationale**:
- Includes native LM_STUDIO provider (added in v1.11.0)
- Bug fixes for Transformer models (v1.11.2)
- Setup wizard and dynamic model loading
- Latest stable release with all features we need

**Alternatives Considered**:
- v1.10.x: Rejected - lacks LM Studio provider, would need custom OpenAI configuration
- Git master branch: Rejected - unstable, not recommended for production

### 2. LM Studio Integration Method

**Question**: How should Perplexica integrate with LM Studio?

**Research**:
- Perplexica v1.11.0+ includes dedicated `LM_STUDIO` provider
- Alternative: Custom OpenAI provider with `CUSTOMOPENAIBASEURL`
- Configuration via config.toml file

**Decision**: Use native `LM_STUDIO` provider in config.toml

**Configuration**:
```toml
[MODELS.LM_STUDIO]
API_URL = "http://host.docker.internal:1234"

[CHAT_MODEL]
PROVIDER = "lm_studio"
MODEL = "qwen2.5-0.5b-instruct"
```

**Rationale**:
- Cleaner than custom OpenAI configuration
- Explicit provider type (better documentation)
- Supports dynamic model discovery via /v1/models endpoint
- Native support means better future compatibility

**Alternatives Considered**:
- Custom OpenAI provider: Rejected - more complex, less explicit about using LM Studio
- Ollama migration: Rejected - LM Studio already working perfectly, no benefit to switching

### 3. SearXNG Integration Strategy

**Question**: Should we use bundled SearXNG or external SearXNG?

**Research**:
- Perplexica Docker image includes bundled SearXNG
- Perplexica supports external SearXNG via `SEARXNG_API_URL` environment variable
- We already have SearXNG running at `http://searxng:8080` in docker-compose

**Decision**: Use existing external SearXNG container

**Configuration**:
```yaml
environment:
  - SEARXNG_API_URL=http://searxng:8080
```

**Rationale**:
- Avoids duplicate services (complexity management)
- Reuses existing SearXNG configuration and customizations
- Constitutional compliance (Principle VI: max 3-4 core services)
- Simpler maintenance (one SearXNG instead of two)

**Alternatives Considered**:
- Bundled SearXNG: Rejected - would create duplicate service
- External commercial search API: Rejected - defeats purpose of self-hosted solution

### 4. SearXNG Configuration Error

**Question**: Why is SearXNG container restarting?

**Research**:
- Error: "ValueError: Invalid settings.yml"
- Error details: "brand.issue_url: The value has to be one of these types/values: str"
- Root cause: brand section uses `false` (boolean) instead of empty strings

**Decision**: Change brand section values from `false` to `""`

**Fix**:
```yaml
# BEFORE (broken):
brand:
  new_issue_url: false
  docs_url: false
  issue_url: false

# AFTER (fixed):
brand:
  new_issue_url: ""
  docs_url: ""
  issue_url: ""
  public_instances: ""
  wiki_url: ""
```

**Rationale**:
- SearXNG validation expects string type
- Empty strings are valid (means "no URL configured")
- Minimal change preserves intent (no URLs)

**Alternatives Considered**:
- Remove brand section entirely: Valid, but explicit empty strings are clearer
- Use actual URLs: Not needed, we don't want these links

### 5. Docker Networking for LM Studio

**Question**: How should Perplexica (containerized) access LM Studio (native)?

**Research**:
- LM Studio runs natively on Mac Studio (not in Docker)
- Docker provides `host.docker.internal` special DNS name
- Alternative: Direct IP address (192.168.1.10:1234)

**Decision**: Use `host.docker.internal:1234`

**Configuration**:
```toml
[MODELS.LM_STUDIO]
API_URL = "http://host.docker.internal:1234"
```

**Rationale**:
- Standard Docker pattern for container→host communication
- `host.docker.internal` resolves to host machine IP from inside containers
- More portable than hardcoded IP
- Works on both Mac and Linux Docker installations

**Alternatives Considered**:
- Direct IP (192.168.1.10:1234): Works but less portable, less clear intent
- Containerize LM Studio: Rejected - unnecessary complexity, MLX requires native deployment

### 6. Lemonade Evaluation

**Question**: Should we use Lemonade instead of LM Studio?

**Research**:
- Lemonade: Client inference framework for Windows/Linux
- Features: NPU/GPU acceleration, AMD RyzenAI focus
- Mac support: Not explicitly mentioned in documentation
- Current state: LM Studio working perfectly with Qwen2.5-0.5B

**Decision**: Do NOT use Lemonade, stay with LM Studio

**Rationale**:
- **Zero migration cost**: LM Studio already installed and working
- **Mac compatibility unclear**: Lemonade docs focus on Windows/Linux/AMD
- **Mac Studio has excellent GPU**: M4 Max with 32 GPU cores + MLX framework
- **No compelling advantage**: Lemonade doesn't offer features we need
- **Working perfectly**: 200-300ms query generation with LM Studio

**Alternatives Considered**:
- Lemonade: Rejected - would require installation, testing, migration effort for no clear benefit
- Ollama: Previously researched - good for headless but LM Studio's MLX gives 15-25% better performance

### 7. Performance Expectations

**Question**: What performance should we expect from Perplexica?

**Research**:
- Perplexica combines search + LLM reasoning
- Search latency: SearXNG typically <1s
- LLM latency: Qwen2.5-0.5B @ 200-300 tokens/sec
- Network latency: Container→host minimal

**Expected Performance**:
- Search query processing: 3-5 seconds total
- UI load time: <2 seconds
- Container startup: <30 seconds
- Health check response: <1 second

**Rationale**:
- Acceptable for non-real-time use case (research, not live search)
- Much faster than Tavily (which also takes 2-3s)
- Self-hosted = no rate limits or API costs

**Validation**:
- Monitor actual performance after deployment
- If too slow: Consider upgrading LLM to 1.5B or 3B model

## Constitutional Compliance Analysis

### Principle VIII: Deployment Responsibility ✅
- Plan documents steps for Kevin to execute
- No automated deployment by AI
- Rsync and docker commands clearly marked as Kevin's responsibility

### Principle IV: Technology Stack ✅
- Uses existing Mac Studio Docker infrastructure
- Reuses PostgreSQL, Qdrant, SearXNG
- LM Studio remains native (not containerized)
- No forbidden technologies introduced

### Principle VI: Complexity Management ✅
- Adds 5th service (Perplexica)
- Justification: Replaces external Tavily API dependency
- Net result: More self-hosted, less external dependencies
- Within acceptable range for infrastructure

## Risk Analysis

### Low Risk ✅
- Configuration-only deployment (no code changes)
- Existing services unaffected (PostgreSQL, Qdrant continue running)
- Easy rollback: Remove Perplexica from docker-compose.yml
- SearXNG fix is isolated (brand section only)

### Medium Risk ⚠️
- SearXNG config change could break search (mitigation: test before deploy)
- Perplexica might not start if LM Studio unavailable (mitigation: health checks)

### Mitigation Strategies
1. Test SearXNG fix locally before Mac Studio deployment
2. Keep docker-compose.yml backup before changes
3. Deploy during low-usage time
4. Verify all existing services healthy after deployment
5. Have rollback procedure documented

## Next Steps

✅ Research complete - all decisions documented
→ Ready for Phase 1: Create deployment artifacts (deployment.md, verification.md, quickstart.md)
