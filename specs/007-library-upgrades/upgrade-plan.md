# Feature 007: Library Upgrades

**Date**: 2025-11-06
**Branch**: `feature/007-library-upgrades`
**Status**: In Progress

---

## Overview

Upgrade project dependencies to latest stable versions, with focus on LangChain4j which is critically outdated.

---

## Current Versions Audit

### Critical - Very Outdated
| Library | Current | Latest | Gap | Priority |
|---------|---------|--------|-----|----------|
| **LangChain4j** | **0.36.2** | **1.8.0** | **Major** | **🔴 CRITICAL** |

### Moderate - Should Update
| Library | Current | Latest | Status |
|---------|---------|--------|--------|
| Spring Boot | 3.5.6 | TBD | Check |
| Lombok | 1.18.42 | TBD | Check |
| Vavr | 0.10.7 | TBD | Check |
| PostgreSQL Driver | 42.7.5 | TBD | Check |
| Flyway | 10.20.1 | TBD | Check |
| TestContainers | 1.20.4 | TBD | Check |
| Caffeine | 3.1.8 | TBD | Check |

---

## LangChain4j 0.36.2 → 1.8.0 Upgrade

### Version History Missed
- **0.36.2** (early 2024) - What we're on
- **1.0.0** - Stable release
- **1.1.0** through **1.7.0** - Unknown features
- **1.8.0** (October 24, 2025) - Latest

### Notable Changes in 1.8.0
1. **MCP (Model Context Protocol)** support
2. **Streaming Cancellation** - New feature
3. **Thinking/Reasoning Support** - AI models can expose reasoning
4. **Guardrails** - Output validation and rewriting
5. **AgenticScope** - Access from @Tool methods
6. **Custom Attributes** - RAG pipeline to ChatMemoryStore
7. **OpenAI Raw HTTP Response** - Streaming chat model exposes raw response

### Potential Breaking Changes

#### 1. **API Changes**
- Some method signatures may have changed
- Builder patterns might be updated
- Configuration properties may have new names

#### 2. **Dependency Changes**
- May require updated transitive dependencies
- Jackson version requirements
- OkHttp version requirements

#### 3. **OpenAI Client Changes**
- `OpenAiChatModel` builder might have new required/optional fields
- Streaming API might have changed
- Request/Response formats might be updated

### Our Current Usage

**Files Using LangChain4j:**
1. `northstar-query-generation/src/main/java/com/northstar/funding/querygeneration/config/LangChain4jConfiguration.java`
2. `northstar-query-generation/src/main/java/com/northstar/funding/querygeneration/strategy/AiOptimizedQueryStrategy.java`

**APIs We Use:**
- `ChatLanguageModel` - Main LLM interface
- `OpenAiChatModel.builder()` - Builder for LM Studio connection
- `UserMessage.from()` - Create user messages
- `Prompt` - Prompt creation
- `.generate()` - Generate responses

### Migration Strategy

**Phase 1: Update POM**
1. Update `langchain4j.version` from 0.36.2 to 1.8.0
2. Run `mvn clean compile` - Check for compilation errors

**Phase 2: Fix Breaking Changes**
1. Update `LangChain4jConfiguration.java` if builder API changed
2. Update `AiOptimizedQueryStrategy.java` if message API changed
3. Fix any import changes

**Phase 3: Test**
1. Run unit tests: `mvn test -pl northstar-query-generation`
2. Run all tests: `mvn test`
3. Manual verification with LM Studio

**Phase 4: Update Documentation**
1. Update `CLAUDE.md` with new version
2. Add session summary for upgrade
3. Commit changes

---

## Other Dependencies

### Spring Boot 3.5.6
**Status**: Check if 3.6.x or 4.x available
**Priority**: Medium (Spring Boot is usually well-maintained)

### Lombok 1.18.42
**Status**: Check latest
**Priority**: Low (Lombok is stable, breaking changes rare)

### Vavr 0.10.7
**Status**: Check if 1.x available
**Priority**: Low (Not actively used yet)

### PostgreSQL Driver 42.7.5
**Status**: Check latest
**Priority**: Medium (Security updates matter)

### Flyway 10.20.1
**Status**: Check latest
**Priority**: Medium (Database migrations are critical)

### TestContainers 1.20.4
**Status**: Check latest
**Priority**: Low (Test infrastructure)

### Caffeine 3.1.8
**Status**: Check latest
**Priority**: Low (Caching library, stable)

---

## Execution Plan

### Step 1: Research Latest Versions ✅
- [x] LangChain4j: 1.8.0 confirmed
- [ ] Spring Boot: TBD
- [ ] Other dependencies: TBD

### Step 2: LangChain4j Upgrade (Priority)
- [ ] Update pom.xml: `langchain4j.version` → 1.8.0
- [ ] Run `mvn clean compile`
- [ ] Fix compilation errors
- [ ] Run tests
- [ ] Manual verification with LM Studio

### Step 3: Other Dependencies (If Time Permits)
- [ ] Update Spring Boot (if newer version available)
- [ ] Update PostgreSQL driver (security updates)
- [ ] Update other dependencies

### Step 4: Testing & Verification
- [ ] All 427 tests must pass
- [ ] Manual test query generation with LM Studio
- [ ] Check for deprecation warnings
- [ ] Verify no behavioral changes

### Step 5: Documentation & Commit
- [ ] Update CLAUDE.md
- [ ] Create session summary
- [ ] Commit with detailed message
- [ ] Push to remote

---

## Risk Assessment

### High Risk
- **LangChain4j Upgrade**: Major version jump (0.36.2 → 1.8.0)
- **Breaking Changes**: Likely API changes
- **Test Failures**: May require code updates

### Medium Risk
- **Spring Boot**: Usually backwards compatible within 3.x
- **PostgreSQL Driver**: Minor updates usually safe

### Low Risk
- **Lombok**: Very stable
- **TestContainers**: Test infrastructure only
- **Caffeine**: Stable caching library

---

## Rollback Plan

If upgrade fails:
1. `git checkout main`
2. `git branch -D feature/007-library-upgrades`
3. Re-evaluate: Investigate specific breaking changes
4. Create smaller incremental upgrade plan

---

## Success Criteria

✅ **Must Have:**
- All 427 tests passing
- LangChain4j upgraded to 1.8.0
- Query generation working with LM Studio
- No compilation errors

✅ **Nice to Have:**
- Other dependencies updated
- No deprecation warnings
- Performance improvements

---

## Timeline Estimate

- **LangChain4j Upgrade**: 1-2 hours
- **Other Dependencies**: 30 minutes
- **Testing & Verification**: 30 minutes
- **Documentation**: 15 minutes

**Total**: 2-3 hours

---

## Notes

- LangChain4j 1.8.0 was released October 24, 2025 (very recent)
- We're 1.5 years behind on LangChain4j versions
- No breaking changes documented publicly yet (need to discover via compilation)
- LM Studio is operational and ready for testing

---

**Status**: Ready to begin LangChain4j upgrade
**Next Step**: Update pom.xml and compile
