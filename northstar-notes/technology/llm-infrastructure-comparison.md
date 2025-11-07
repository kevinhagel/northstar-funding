# LLM Infrastructure Comparison: LM Studio vs Ollama

**Date**: 2025-11-07
**Environment**: Mac Studio M4 Max, 36 GB RAM
**Purpose**: Query generation for NorthStar Funding Discovery

---

## Current Setup

### Hardware
- **Mac Studio M4 Max**
- **36 GB Unified Memory**
- **14 Cores** (10 performance + 4 efficiency)
- **macOS 26.0.1** (Sequoia)
- **Network**: Accessible at `192.168.1.10`

### Current LM Studio Configuration
- **Version**: 0.3.17+11
- **Port**: 1234 (listening on all interfaces `*:1234`)
- **API**: OpenAI-compatible HTTP API
- **Models Loaded**:
  - `llama-3.1-8b-instruct` (8B parameters)
  - `phi-3-medium-4k-instruct` (~14B parameters)
  - `text-embedding-nomic-embed-text-v1.5` (embedding model)

### Integration with NorthStar
- **Library**: LangChain4j 1.8.0
- **Configuration**: `northstar-query-generation/src/main/java/com/northstar/funding/querygeneration/config/LmStudioConfig.java`
- **HTTP Version**: Forced to HTTP/1.1 (LM Studio requirement)
- **Base URL**: `http://192.168.1.10:1234/v1`
- **Timeout**: 30 seconds

---

## Problem Statement

**Query generation is slow** - integration tests timeout after 30 seconds waiting for LM Studio to respond.

**Root Cause**: Using an 8B parameter model (Llama 3.1 8B) for simple query generation is overkill. A 0.5B-3B model would be 10-20x faster with no quality loss for this task.

---

## LM Studio vs Ollama Comparison

### LM Studio

#### Pros ✅
- **GUI-based**: Easy model management, download, configuration
- **Currently working**: Already configured for remote access on port 1234
- **OpenAI-compatible API**: Drop-in replacement, works with LangChain4j
- **Model stays loaded**: First request loads model, subsequent requests are fast
- **Multi-model support**: Can load multiple models simultaneously
- **Easy configuration**: Click-based settings (CORS, network binding, etc.)
- **Visual monitoring**: Can see requests, performance metrics in real-time

#### Cons ❌
- **Resource heavy**: Electron-based GUI uses ~500 MB RAM
- **Manual model selection**: Must manually select model via GUI
- **Not headless**: Requires GUI to be running
- **Limited CLI**: No command-line model management
- **Configuration via GUI**: Setup required work (your mention)

---

### Ollama

#### Pros ✅
- **Headless by default**: Runs as system service, no GUI required
- **CLI-based**: `ollama run qwen2.5:0.5b` - instant model switching
- **Automatic model management**: Downloads models on first use
- **Lightweight**: ~100 MB RAM overhead (vs 500+ MB for LM Studio)
- **REST API**: OpenAI-compatible endpoints (`/v1/chat/completions`)
- **Model library**: `ollama list` shows all installed models
- **Easy remote access**: `OLLAMA_HOST=0.0.0.0:11434` in environment
- **Systemd/launchd integration**: Auto-start on boot
- **Better performance**: Often faster inference than LM Studio
- **Model versioning**: `ollama pull qwen2.5:0.5b` manages versions

#### Cons ❌
- **No GUI**: Command-line only (but this is also a pro for automation)
- **Different default port**: 11434 (vs LM Studio 1234)
- **Requires migration**: Need to reconfigure NorthStar to use Ollama endpoint
- **Not currently installed**: Would need setup

---

## Performance Comparison

### Model Size Impact (Theoretical)

| Model Size | Tokens/sec (M4 Max) | Time for 50 tokens | Use Case |
|------------|---------------------|-------------------|----------|
| **0.5B** (Qwen2.5) | ~200-300 | **0.2-0.3s** | ✅ Query generation |
| **1.5B** (Qwen2.5) | ~100-150 | **0.4-0.6s** | ✅ Query generation |
| **3B** (Phi-3 Mini) | ~50-80 | **0.7-1.0s** | ✅ Query generation |
| **8B** (Llama 3.1) | ~20-30 | **2.0-3.0s** | ❌ Too slow |
| **14B** (Phi-3 Medium) | ~10-15 | **4.0-6.0s** | ❌ Way too slow |

**Speedup**: Switching from 8B to 0.5B model = **10-15x faster query generation**

---

## Network Accessibility

Both LM Studio and Ollama can be configured for remote access:

### LM Studio Remote Access
```bash
# Already configured - listening on *:1234
curl http://192.168.1.10:1234/v1/models
```

### Ollama Remote Access
```bash
# Set environment variable
export OLLAMA_HOST=0.0.0.0:11434

# Or in launchd plist
<key>EnvironmentVariables</key>
<dict>
    <key>OLLAMA_HOST</key>
    <string>0.0.0.0:11434</string>
</dict>
```

Both support **same OpenAI-compatible API**, so no code changes needed (just URL + port).

---

## Recommendation

### Short Term: **Stick with LM Studio, Use Smaller Model**

**Why:**
1. Already configured and working
2. No migration needed
3. **Just switch to Qwen2.5 0.5B or Phi-3 Mini 3.8B** in LM Studio GUI
4. Instant 10-15x speedup

**Action Items:**
1. Download Qwen2.5 0.5B or Phi-3 Mini 3.8B in LM Studio
2. Switch to that model in LM Studio
3. Test query generation - should see massive speedup
4. No code changes needed

---

### Long Term: **Migrate to Ollama** (Optional)

**Why:**
1. More suitable for headless server operation
2. CLI-based model switching (automation-friendly)
3. Lighter weight, better performance
4. Industry standard for local LLM deployment

**Migration Plan:**
```bash
# 1. Install Ollama on Mac Studio
ssh macstudio
curl -fsSL https://ollama.com/install.sh | sh

# 2. Pull small model
ollama pull qwen2.5:0.5b

# 3. Configure remote access
echo 'export OLLAMA_HOST=0.0.0.0:11434' >> ~/.zshrc
source ~/.zshrc

# 4. Start Ollama service
ollama serve

# 5. Test from laptop
curl http://192.168.1.10:11434/v1/models

# 6. Update NorthStar config
# Change: http://192.168.1.10:1234/v1
# To:     http://192.168.1.10:11434/v1
```

**Code Change:**
```yaml
# northstar-query-generation/src/main/resources/application.yml
query-generation:
  lm-studio:
    base-url: http://192.168.1.10:11434/v1  # Changed from :1234
```

**Rollback Plan:** Keep LM Studio installed, just stop it when using Ollama.

---

## Recommended Small Models

### For Query Generation (3-5 search queries)

| Model | Size | Speed | Quality | Recommendation |
|-------|------|-------|---------|----------------|
| **Qwen2.5:0.5b** | 0.5B | ⚡️⚡️⚡️⚡️⚡️ | ⭐️⭐️⭐️ | ✅ **Best choice** |
| **Qwen2.5:1.5b** | 1.5B | ⚡️⚡️⚡️⚡️ | ⭐️⭐️⭐️⭐️ | ✅ Good balance |
| **Phi-3-mini** | 3.8B | ⚡️⚡️⚡️ | ⭐️⭐️⭐️⭐️⭐️ | ✅ Best quality |
| **Gemma:2b** | 2B | ⚡️⚡️⚡️⚡️ | ⭐️⭐️⭐️⭐️ | ✅ Google model |

All of these are **10-20x faster** than Llama 3.1 8B for query generation.

---

## Testing Methodology

To measure actual performance improvement:

```bash
# Test current 8B model
time curl -X POST http://192.168.1.10:1234/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{
    "model": "llama-3.1-8b-instruct",
    "messages": [{"role": "user", "content": "Generate 3 funding search queries for Bulgaria scholarships"}],
    "max_tokens": 50
  }'

# Test small model (after switching)
time curl -X POST http://192.168.1.10:1234/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{
    "model": "qwen2.5:0.5b",
    "messages": [{"role": "user", "content": "Generate 3 funding search queries for Bulgaria scholarships"}],
    "max_tokens": 50
  }'
```

---

## Decision Matrix

| Criteria | LM Studio (Small Model) | Ollama (Small Model) |
|----------|------------------------|---------------------|
| **Setup Effort** | ✅ Zero (just switch model) | ❌ Medium (install + configure) |
| **Performance** | ⭐️⭐️⭐️⭐️ (very fast) | ⭐️⭐️⭐️⭐️⭐️ (slightly faster) |
| **Ease of Use** | ⭐️⭐️⭐️⭐️⭐️ (GUI) | ⭐️⭐️⭐️ (CLI only) |
| **Automation** | ⭐️⭐️⭐️ (manual model switching) | ⭐️⭐️⭐️⭐️⭐️ (CLI automation) |
| **Resource Usage** | ⭐️⭐️⭐️ (500 MB overhead) | ⭐️⭐️⭐️⭐️⭐️ (100 MB overhead) |
| **Remote Access** | ✅ Already working | ✅ Easy to configure |
| **Model Management** | ⭐️⭐️⭐️⭐️⭐️ (GUI download) | ⭐️⭐️⭐️⭐️ (CLI pull) |

---

## Final Recommendation

### Immediate Action: **Switch to Qwen2.5 0.5B in LM Studio**

This gives you **10-15x speedup** with zero code changes and zero configuration effort.

### Future Consideration: **Ollama Migration**

Once you validate the small model works well, consider migrating to Ollama for:
- Better automation (CLI-based model switching)
- Lower resource usage
- Industry-standard deployment pattern
- Headless operation

But **not urgent** - LM Studio with small model will work perfectly fine.

---

## References

- LM Studio: https://lmstudio.ai/
- Ollama: https://ollama.com/
- Qwen2.5 Models: https://ollama.com/library/qwen2.5
- Phi-3 Models: https://ollama.com/library/phi3
- LangChain4j OpenAI Integration: https://docs.langchain4j.dev/integrations/language-models/open-ai

---

**Updated**: 2025-11-07
**Reviewed**: After Feature 007 Library Upgrades completion
