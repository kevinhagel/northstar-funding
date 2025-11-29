# Research: Switch Query Generation to LM Studio

**Feature**: 015-switch-query-generation
**Date**: 2025-11-29

## Key Findings

### 1. LM Studio vs Ollama Protocols

| Aspect | LM Studio | Ollama |
|--------|-----------|--------|
| Port | 1234 | 11434 |
| API | OpenAI-compatible (`/v1/chat/completions`) | Ollama protocol (`/api/generate`) |
| LangChain4j Support | Via OpenAiChatModel | Via OllamaChatModel |
| Perplexica Support | No (wrong protocol) | Yes (native) |

**Decision**: Use LM Studio for NorthStar query generation, keep Ollama for Perplexica.

### 2. Current Configuration Status

| Component | Config File | Status |
|-----------|-------------|--------|
| Query Generation | `application.yml` | LM Studio (port 1234) |
| LlmConfig.java | `LlmConfig.java` | LM Studio (commit b62ba09) |
| Perplexica | `config.toml` | Ollama (port 11434) |

### 3. Why Two LLM Backends is Acceptable

- **NorthStar Query Generation**: Generates search queries for funding discovery. Uses LangChain4j with OpenAI-compatible API (LM Studio).
- **Perplexica**: Third-party AI search engine. Uses Ollama internally for its own AI features. We call it as a search provider, not its LLM.

These are separate concerns. Perplexica is a black box search adapter - we don't control its internal LLM usage.

### 4. Test Analysis

- 57 tests in `northstar-query-generation`
- Tests use mocked LLM responses
- Real LLM validation requires manual testing or integration tests against live LM Studio

## Alternatives Considered

| Option | Pros | Cons | Decision |
|--------|------|------|----------|
| All LM Studio | Single backend | Perplexica doesn't support it | REJECTED |
| All Ollama | Perplexica works | Parallelism issues documented | REJECTED |
| Mixed (LM Studio + Ollama) | Each tool uses best backend | Two backends to maintain | SELECTED |

## Conclusion

The current architecture is correct:
- LM Studio for NorthStar query generation (reliable, proven)
- Ollama for Perplexica internal use (required by Perplexica)

No code changes needed. Feature 015 is a verification feature.
