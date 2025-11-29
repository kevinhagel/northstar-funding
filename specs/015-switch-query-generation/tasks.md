# Tasks: Switch Query Generation to LM Studio

**Input**: Design documents from `/specs/015-switch-query-generation/`
**Prerequisites**: plan.md, research.md, spec.md
**Status**: COMPLETE - All tasks verified in session 2025-11-29

## Summary

Feature 015 is a verification/configuration feature with minimal code changes. The primary work was updating Perplexica to use LM Studio instead of Ollama.

## Phase 3.1: Setup
- [x] T001 Verify LM Studio running on Mac Studio at port 1234
  - **File**: N/A (infrastructure verification)
  - **Command**: `ssh macstudio "curl -s http://localhost:1234/v1/models"`
  - **Result**: `llama-3.1-8b-instruct` model available

## Phase 3.2: Configuration Updates
- [x] T002 Update docker-compose.yml for Perplexica LM Studio
  - **File**: `docker/docker-compose.yml`
  - **Change**: Replace `OLLAMA_BASE_URL` with `LM_STUDIO_BASE_URL=http://192.168.1.10:1234`
  - **Commit**: be10b86

- [x] T003 Update Perplexica config.toml
  - **File**: `docker/perplexica/config.toml`
  - **Change**: Add `[MODELS.LM_STUDIO]` section with baseURL
  - **Commit**: be10b86

- [x] T004 Rsync docker config to Mac Studio
  - **Command**: `rsync -av docker/ macstudio:~/northstar/`

- [x] T005 Restart Perplexica with fresh data volume
  - **Command**: `docker compose stop perplexica && docker compose rm -f perplexica && docker volume rm -f northstar-perplexica-data && docker compose up -d perplexica`

## Phase 3.3: Verification
- [x] T006 Verify Perplexica config.json shows LM Studio provider
  - **Command**: `docker exec northstar-perplexica cat /home/perplexica/data/config.json`
  - **Result**: `"type": "lmstudio", "config": {"baseURL": "http://192.168.1.10:1234"}`

- [x] T007 Run query generation tests (57 tests)
  - **Command**: `mvn test -pl northstar-query-generation`
  - **Result**: All tests passing

- [x] T008 Manual test Perplexica UI responsiveness
  - **URL**: http://192.168.1.10:3001
  - **Result**: User confirmed "MUCH faster than it was with Ollama"

## Phase 3.4: Documentation
- [x] T009 Commit all changes
  - **Commit**: be10b86 "feat: Switch Perplexica from Ollama to LM Studio"

- [ ] T010 Update session summary
  - **File**: `northstar-notes/session-summaries/2025-11-29-feature-015-lm-studio.md`

- [ ] T011 Merge to main branch
  - **Command**: `git checkout main && git merge 015-switch-query-generation`

## Dependencies
- T001 before T002-T005 (LM Studio must be running)
- T002-T005 before T006-T008 (config must be deployed)
- T006-T008 before T009-T011 (verification before documentation)

## Validation Checklist
- [x] LM Studio responding at port 1234
- [x] Perplexica config shows LM Studio provider
- [x] All 57 query generation tests pass
- [x] Manual UI test confirms improved performance
- [x] Changes committed to feature branch

## Notes
- Feature 015 was primarily a verification/configuration feature
- The original plan assumed Perplexica couldn't use LM Studio (wrong)
- Discovery: Perplexica supports `LM_STUDIO_BASE_URL` environment variable
- Ollama parallelism issues confirmed - LM Studio is the correct choice
