# Claude Code Learning Guide for NorthStar Funding Backend Development

**Created**: 2025-10-16  
**Project**: NorthStar Funding Discovery  
**Context**: Backend service layer development with Spring Boot, Spring Data JDBC, PostgreSQL

---

## 🎯 **Your Claude Code Learning Path - Best Tutorials (Time-Ordered)**

This guide provides the quickest and most practical tutorials for learning Claude Code, sub-agents, MCP servers, and automation hooks - specifically tailored for your backend development workflow.

---

## **Phase 1: Claude Code Basics (30-45 minutes)**

### **1. Setup & Installation (10 mins)**
📖 **Official Docs**: [Claude Code Setup](https://docs.claude.com/en/docs/claude-code/setup)

**Quick Start in Your Terminal**:
```bash
# Already in your Cursor terminal
cd ~/github/northstar-funding/backend
claude  # Launch Claude Code in your project

# Key commands you'll use immediately:
/help    # See all commands
/agents  # Manage sub-agents
/hooks   # Setup automation
```

**You already have**:
- ✅ Node.js (required)
- ✅ Project directory (northstar-funding)
- ✅ Mac Studio infrastructure

**System Requirements**:
- macOS 10.15+ ✅
- 4GB+ RAM ✅
- Node.js 18+ ✅
- Internet connection (for authentication and AI processing)

**Installation**:
```bash
# Standard installation
npm install -g @anthropic-ai/claude-code

# Or native binary (recommended)
curl -fsSL https://claude.ai/install.sh | bash

# Check installation
claude doctor
```

---

## **Phase 2: Sub-Agents for Backend Development (45-60 minutes)**

### **2. Sub-Agents Deep Dive (CRITICAL FOR YOUR WORKFLOW)**
📖 **Official Guide**: [Claude Code Sub-Agents](https://docs.claude.com/en/docs/claude-code/sub-agents)

**Why This Matters for Your Backend Work**:
- Create specialized agents for **repository testing**, **service layer development**, **JPA entity validation**
- Each sub-agent has its own context window (won't pollute your main conversation)
- Perfect for your TDD workflow: test agent → implementation agent → review agent

**Hands-On Exercise (15 mins)**:
```bash
# In your Cursor terminal with Claude Code running:
/agents

# Create these sub-agents for your backend work:
# 1. "repository-tester" - Focuses on *RepositoryIT.java tests
# 2. "service-developer" - Builds service layer with business logic
# 3. "jpa-expert" - Spring Data JDBC and domain model expert
```

### **Example Sub-Agent #1: Repository Tester**

Create `.claude/agents/repository-tester.md`:

```yaml
---
name: repository-tester
description: Spring Data JDBC repository testing specialist. Use proactively when working with *RepositoryIT.java files or creating integration tests. Focuses on PostgreSQL integration tests with Mac Studio database.
tools: Read, Edit, Bash, Grep, Glob
model: inherit
---

You are a Spring Data JDBC testing expert specializing in repository integration tests for PostgreSQL 16.

When invoked:
1. Analyze the repository interface and entity model
2. Create comprehensive @DataJdbcTest integration tests
3. Test CRUD operations, custom @Query methods, pagination
4. Validate PostgreSQL-specific features (JSONB, arrays, full-text search)
5. Ensure tests use Mac Studio PostgreSQL (192.168.1.10)

Testing checklist:
- @SpringBootTest with @Transactional for automatic rollback
- Test all custom query methods return expected results
- Verify database constraints and foreign key relationships work
- Test null handling, edge cases, and error conditions
- Validate pagination and sorting functionality
- Test PostgreSQL-specific operations (arrays, JSONB, full-text search)
- Ensure proper test data setup and teardown

Database configuration:
- Mac Studio PostgreSQL: 192.168.1.10:5432
- Database: northstar_funding
- Test profile: application-test.yml
- Schema: managed by Flyway migrations (V1-V7)

Example test structure:
```java
@SpringBootTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Transactional
class FundingSourceCandidateRepositoryIT {
    
    @Autowired
    private FundingSourceCandidateRepository repository;
    
    @Test
    void shouldSaveAndRetrieveCandidateWithJsonbData() {
        // Test implementation
    }
}
```

Focus on production parity with PostgreSQL 16 and comprehensive test coverage.
```

### **Example Sub-Agent #2: Service Layer Developer**

Create `.claude/agents/service-developer.md`:

```yaml
---
name: service-developer
description: Spring Boot service layer specialist. Use when implementing business logic, transaction management, and service layer patterns. Follows DDD principles and NorthStar constitution.
tools: Read, Edit, Write, Bash, Grep, Glob
model: sonnet
---

You are a Spring Boot service layer expert following Domain-Driven Design principles.

When invoked:
1. Read domain models and repository interfaces
2. Implement service layer with business logic
3. Apply @Service and @Transactional annotations correctly
4. Coordinate multiple repositories for complex workflows
5. Implement proper error handling and validation
6. Follow NorthStar constitution principles

Service layer responsibilities:
- Business rule enforcement and validation
- Transaction coordination across repositories
- Domain logic orchestration
- Data transformation (entities ↔ DTOs)
- Security and authorization checks
- Exception translation to business exceptions

Key patterns to follow:
- Services delegate to repositories, never expose repositories to controllers
- Use @Transactional for operations that modify data
- Implement proper exception handling with custom business exceptions
- Validate input at service boundaries
- Keep services focused (single responsibility)
- Document business rules in method comments

Example service structure:
```java
@Service
@Transactional(readOnly = true)
public class CandidateValidationService {
    
    private final FundingSourceCandidateRepository candidateRepository;
    private final AdminUserRepository adminUserRepository;
    
    @Transactional
    public FundingSourceCandidate assignCandidateToReviewer(
            UUID candidateId, UUID reviewerId) {
        // Business logic implementation
        // - Validate candidate status
        // - Check reviewer workload capacity
        // - Update candidate assignment
        // - Update reviewer workload
        // - Return updated candidate
    }
}
```

Always coordinate with repository-tester to ensure integration tests exist before implementation.
```

### **Example Sub-Agent #3: JPA Domain Expert**

Create `.claude/agents/jpa-expert.md`:

```yaml
---
name: jpa-expert
description: Spring Data JDBC and domain model expert. Use when working with entity classes, domain models, or database mappings. Understands PostgreSQL-specific features.
tools: Read, Edit, Grep, Glob
model: inherit
---

You are a Spring Data JDBC and domain modeling expert specializing in PostgreSQL 16.

When invoked:
1. Review or create domain entity classes
2. Validate Spring Data JDBC annotations
3. Ensure proper PostgreSQL type mappings
4. Review repository interface designs
5. Validate domain model relationships

Domain model checklist:
- @Table annotation with correct table name
- @Id annotation on primary key field (UUID)
- Proper column mappings with @Column
- Enum converters registered in JdbcConfiguration
- JSON/JSONB fields properly mapped
- Array fields (List<String>, Set<String>) correctly configured
- Relationships (@MappedCollection) properly defined
- Lombok @Data/@Builder for boilerplate reduction

PostgreSQL-specific mappings:
- UUID primary keys
- JSONB columns → Map<String, Object>
- TEXT[] arrays → List<String> or Set<String>
- Enums → VARCHAR with CHECK constraints + custom converters
- TIMESTAMP → LocalDateTime

Example entity:
```java
@Table("funding_source_candidate")
@Data
@Builder
public class FundingSourceCandidate {
    
    @Id
    @Column("candidate_id")
    private UUID candidateId;
    
    @Column("status")
    private CandidateStatus status;
    
    @Column("extracted_data")
    private Map<String, Object> extractedData;  // JSONB
    
    @Column("tags")
    private Set<String> tags;  // TEXT[]
    
    @Column("discovered_at")
    private LocalDateTime discoveredAt;
}
```

Enum converter example:
```java
@ReadingConverter
public static class CandidateStatusReadingConverter 
        implements Converter<String, CandidateStatus> {
    @Override
    public CandidateStatus convert(String source) {
        return CandidateStatus.valueOf(source);
    }
}
```

Always ensure enum converters are registered in JdbcConfiguration.
```

### **Managing Sub-Agents**

**Create via command line**:
```bash
/agents
# Interactive UI for creating, editing, deleting sub-agents
# Choose project-level (.claude/agents/) or user-level (~/.claude/agents/)
```

**Invoke explicitly**:
```bash
> Use the repository-tester subagent to review FundingSourceCandidateRepositoryIT

> Have the service-developer subagent help me create CandidateValidationService tests

> Ask the jpa-expert to validate my domain model annotations
```

**Best Practices**:
- Start with Claude-generated agents, then customize
- Design focused agents (single responsibility)
- Write detailed system prompts with examples
- Limit tool access to only what's needed
- Version control project-level agents (.claude/agents/)
- Share agents with your team for consistency

---

## **Phase 3: MCP Servers (45-60 minutes)**

### **3. Understanding MCP (Model Context Protocol)**
📖 **Best Tutorial**: [MCP Tutorial: Build Your First Server in 6 Steps](https://towardsdatascience.com/model-context-protocol-mcp-tutorial-build-your-first-mcp-server-in-6-steps/)

**What is MCP?**
- Standardized protocol for connecting LLMs to external tools/data
- Think of it as "USB ports for AI" - plug in any tool/database/API
- Your backend could expose MCP servers for Claude to interact with

**Architecture**:
```
Claude Code (Client) ←→ MCP Server ←→ Your Resource
                                      (Database, API, Files)
```

**MCP Capabilities**:
- **Tools**: Functions that LLM can call
- **Resources**: Data/content that LLM can read
- **Prompts**: Templates for common workflows

### **Quick MCP Server Example (Python)**

Create `mcp-server/northstar-db-query.py`:

```python
from mcp import FastMCP
import asyncpg

mcp = FastMCP("northstar-db-query")

@mcp.tool()
async def query_candidates(status: str) -> dict:
    """Query funding source candidates by status from Mac Studio PostgreSQL"""
    conn = await asyncpg.connect(
        host='192.168.1.10',
        port=5432,
        user='northstar_user',
        password='northstar_password',
        database='northstar_funding'
    )
    
    query = """
        SELECT candidate_id, organization_name, program_name, 
               confidence_score, discovered_at
        FROM funding_source_candidate
        WHERE status = $1
        ORDER BY confidence_score DESC
        LIMIT 10
    """
    
    rows = await conn.fetch(query, status)
    await conn.close()
    
    return {
        "count": len(rows),
        "candidates": [dict(row) for row in rows]
    }

@mcp.tool()
async def get_candidate_details(candidate_id: str) -> dict:
    """Get detailed information about a specific candidate"""
    # Implementation here
    pass

# Run the server
if __name__ == "__main__":
    mcp.run()
```

### **Connecting MCP Server to Claude Code**

Add to `~/.claude/settings.json` or `.claude/settings.json`:

```json
{
  "mcpServers": {
    "northstar-db": {
      "command": "python3",
      "args": ["mcp-server/northstar-db-query.py"],
      "env": {
        "DB_HOST": "192.168.1.10",
        "DB_PORT": "5432",
        "DB_NAME": "northstar_funding"
      }
    }
  }
}
```

### **Practical Use Cases for NorthStar**:
1. **Database Query Server**: Expose PostgreSQL via MCP (Claude can query candidates, sessions, etc.)
2. **Service Layer Testing**: MCP server that runs service layer tests
3. **LM Studio Integration**: Connect to LM Studio via MCP for AI-assisted enhancements
4. **Flyway Migrations**: MCP tools to check migration status, run migrations
5. **Test Data Factory**: MCP server that generates test data on demand

### **More MCP Resources**:
- 📖 [Official MCP Quickstart](https://modelcontextprotocol.io/quickstart)
- 📖 [Ultimate MCP Guide (Generect)](https://generect.com/blog/claude-mcp/)
- 📖 [MCP Guide with Demo (DataCamp)](https://www.datacamp.com/tutorial/mcp-model-context-protocol)
- 📖 [Building MCP Server (Medium)](https://heeki.medium.com/building-an-mcp-server-as-an-api-developer-cfc162d06a83)

---

## **Phase 4: Hooks & Automation (30-45 minutes)**

### **4. Claude Code Hooks (Automate Your Workflow)**
📖 **Official Guide**: [Claude Code Hooks](https://docs.claude.com/en/docs/claude-code/hooks-guide)

**What are Hooks?**
- User-defined shell commands that execute at various points in Claude Code's lifecycle
- Provide deterministic control (always happens, not relying on LLM choice)
- Turn suggestions into app-level code that executes automatically

**Hook Events**:
- **PreToolUse**: Runs before tool calls (can block them)
- **PostToolUse**: Runs after tool calls complete
- **UserPromptSubmit**: Runs when user submits a prompt
- **Notification**: Runs when Claude sends notifications
- **Stop**: Runs when Claude finishes responding
- **SubagentStop**: Runs when subagent tasks complete
- **SessionStart**: Runs when session starts
- **SessionEnd**: Runs when session ends

### **Why Hooks Matter for Backend Dev**:
- Auto-format Java code after every file edit
- Run integration tests automatically after repository changes
- Block modifications to production configuration files
- Validate code against your constitution rules
- Log all bash commands for security audit
- Notify when tests fail

### **Practical Hooks for Your Backend**

#### **Hook 1: Auto-run Integration Tests After Repository Changes**

Add to `.claude/settings.json`:

```json
{
  "hooks": {
    "PostToolUse": [
      {
        "matcher": "Edit|MultiEdit|Write",
        "hooks": [
          {
            "type": "command",
            "command": "jq -r '.tool_input.file_path' | { read file_path; if echo \"$file_path\" | grep -q 'Repository\\.java$'; then echo \"Running tests for $(basename \"$file_path\")\"; cd ~/github/northstar-funding/backend && mvn test -Dtest=\"$(basename \"$file_path\" .java)IT\" 2>&1 | tail -20; fi; }"
          }
        ]
      }
    ]
  }
}
```

#### **Hook 2: Auto-format Java Files**

```json
{
  "hooks": {
    "PostToolUse": [
      {
        "matcher": "Edit|MultiEdit|Write",
        "hooks": [
          {
            "type": "command",
            "command": "jq -r '.tool_input.file_path' | { read file_path; if echo \"$file_path\" | grep -q '\\.java$'; then google-java-format --replace \"$file_path\" 2>/dev/null || echo \"Skipping format for $file_path\"; fi; }"
          }
        ]
      }
    ]
  }
}
```

#### **Hook 3: Block Edits to Production Configuration**

```json
{
  "hooks": {
    "PreToolUse": [
      {
        "matcher": "Edit|MultiEdit|Write",
        "hooks": [
          {
            "type": "command",
            "command": "python3 -c \"import json, sys; data=json.load(sys.stdin); path=data.get('tool_input',{}).get('file_path',''); sensitive_files=['application.yml', 'application-mac-studio.yml', '.env', 'pom.xml']; sys.exit(2 if any(f in path for f in sensitive_files) else 0)\""
          }
        ]
      }
    ]
  }
}
```

#### **Hook 4: Validate Against Constitution**

Create `.claude/hooks/constitution-validator.sh`:

```bash
#!/bin/bash
# Validate code changes against NorthStar constitution

FILE_PATH=$(jq -r '.tool_input.file_path' | tr -d '\n')

# Check for XML tag hallucinations (<n> instead of <name>)
if grep -q '<n>' "$FILE_PATH" 2>/dev/null; then
    echo "⚠️  WARNING: Found <n> tag in $FILE_PATH - this is a hallucination!"
    echo "Constitution violation: XML Tag Accuracy (CRITICAL)"
    exit 2
fi

# Check for forbidden patterns
if grep -qi 'Grant' "$FILE_PATH" 2>/dev/null; then
    if ! grep -qi 'Funding Source' "$FILE_PATH" 2>/dev/null; then
        echo "⚠️  WARNING: Using 'Grant' instead of 'Funding Source'"
        echo "Constitution violation: Domain-Driven Design"
        exit 2
    fi
fi

exit 0
```

Make executable:
```bash
chmod +x .claude/hooks/constitution-validator.sh
```

Add to `.claude/settings.json`:
```json
{
  "hooks": {
    "PreToolUse": [
      {
        "matcher": "Edit|MultiEdit|Write",
        "hooks": [
          {
            "type": "command",
            "command": ".claude/hooks/constitution-validator.sh"
          }
        ]
      }
    ]
  }
}
```

#### **Hook 5: Log All Bash Commands**

```json
{
  "hooks": {
    "PreToolUse": [
      {
        "matcher": "Bash",
        "hooks": [
          {
            "type": "command",
            "command": "jq -r '\"\\(.tool_input.command) - \\(.tool_input.description // \"No description\")\"' >> ~/.claude/bash-command-log.txt"
          }
        ]
      }
    ]
  }
}
```

### **Managing Hooks**

**Interactive UI**:
```bash
/hooks
# Select hook event (PreToolUse, PostToolUse, etc.)
# Add matchers (tool names, wildcards)
# Add hook commands
# Choose storage (user or project settings)
```

**Direct File Editing**:
- User settings: `~/.claude/settings.json`
- Project settings: `.claude/settings.json`

**Security Warning**: Hooks run with your credentials. Always review hook code before registration. Malicious hooks can exfiltrate data.

---

## **Phase 5: Spec-Kit Integration (30 minutes)**

### **5. Using Spec-Kit with Claude Code**

**Your Current Setup**:
```
.specify/
├── memory/constitution.md      ← Your project laws
├── scripts/
│   ├── setup-plan.sh          ← Generate plans
│   └── update-agent-context.sh ← Update Claude context
└── templates/                  ← Document templates

specs/
└── 001-automated-funding-discovery/
    ├── spec.md                 ← Business requirements
    ├── plan.md                 ← Technical design
    ├── data-model.md           ← Domain model
    ├── tasks.md                ← Task breakdown
    └── ...
```

### **Integration Strategy**

#### **Create a Spec-Kit Sub-Agent**

Create `.claude/agents/spec-kit-planner.md`:

```yaml
---
name: spec-kit-planner
description: Specification-driven development expert. Use when creating new features, generating technical plans, or ensuring constitutional compliance. Follows the NorthStar spec-kit framework.
tools: Read, Write, Bash, Grep, Glob
model: sonnet
---

You are a spec-kit framework expert who follows the NorthStar Funding constitution.

When invoked:
1. Read .specify/memory/constitution.md for project rules
2. Use .specify/templates/ to structure documents
3. Follow the 4-phase workflow: specify → plan → tasks → implement
4. Ensure all work follows the 7 core principles

Key principles to enforce:
- **XML Tag Accuracy**: NEVER use <n>, always use full tag names
- **Domain-Driven Design**: "Funding Sources" not "Grants"
- **Human-AI Collaboration**: Every automated process needs validation workflows
- **Technology Stack**: Java 25 + Spring Boot 3.5+ (non-negotiable)
- **Three-Workflow Architecture**: Discovery, Services, Database Discovery
- **Complexity Management**: Max 4 services, break work into small steps
- **Contact Intelligence Priority**: Contacts are first-class entities

Before making any architectural decisions:
1. Read .specify/memory/constitution.md
2. Check if decision violates any core principle
3. Document rationale if deviation is necessary
4. Follow spec-kit templates for documentation

When creating new features:
1. Start with spec.md (WHAT users need, not HOW)
2. Generate plan.md (technical architecture, HOW to implement)
3. Break down into tasks.md (implementation steps)
4. Create domain models using data-model.md template
5. Ensure TDD approach: tests before implementation

Always reference constitution.md before making architectural decisions.
```

#### **Hook for Auto-Loading Constitution**

Add to `.claude/settings.json`:

```json
{
  "hooks": {
    "SessionStart": [
      {
        "matcher": "",
        "hooks": [
          {
            "type": "command",
            "command": "if [ -f .specify/memory/constitution.md ]; then echo '📜 Constitution loaded. Project rules active.'; cat .specify/memory/constitution.md | head -50; fi"
          }
        ]
      }
    ]
  }
}
```

#### **Hook for Constitution Compliance Check**

Create `.claude/hooks/spec-kit-validator.sh`:

```bash
#!/bin/bash
# Validate changes comply with spec-kit structure and constitution

FILE_PATH=$(jq -r '.tool_input.file_path')

# Check if modifying spec-kit structure
if echo "$FILE_PATH" | grep -q '\.specify/'; then
    echo "⚠️  Modifying spec-kit framework. Ensure compliance with templates."
fi

# Check if creating new feature
if echo "$FILE_PATH" | grep -q 'specs/[0-9]\{3\}-'; then
    FEATURE_DIR=$(echo "$FILE_PATH" | grep -o 'specs/[0-9]\{3\}-[^/]*')
    
    # Ensure required files exist
    for required in spec.md plan.md tasks.md data-model.md; do
        if [ ! -f "$FEATURE_DIR/$required" ]; then
            echo "⚠️  Missing $required in $FEATURE_DIR"
            echo "Spec-kit requires: spec.md → plan.md → tasks.md"
        fi
    done
fi

exit 0
```

---

## 🚀 **Your Action Plan for Today**

### **Morning Session (2-3 hours)**

#### **1. Claude Code Basic Setup (30 mins)**
```bash
cd ~/github/northstar-funding/backend
claude  # Launch in your project

# Try basic commands:
/help
/settings
/agents
/hooks

# Test basic queries:
> Help me understand the current state of the repository layer
> What integration tests are passing?
> Show me the persistence layer architecture
```

#### **2. Create Your First Sub-Agents (1 hour)**
```bash
/agents

# Create 3 essential sub-agents:
# 1. repository-tester (for *RepositoryIT.java)
# 2. service-developer (for business logic)
# 3. jpa-expert (for entity/domain models)

# Use the templates provided in this guide
```

#### **3. Test Sub-Agent Workflow (30 mins)**
```bash
> Use the repository-tester subagent to review FundingSourceCandidateRepositoryIT

> Have the service-developer subagent help me plan CandidateValidationService implementation

> Ask the jpa-expert to validate my domain model annotations
```

### **Afternoon Session (2 hours)**

#### **4. Add Automation Hooks (1 hour)**
```bash
/hooks

# Add these hooks in order:
# 1. PreToolUse → Block edits to production config
# 2. PostToolUse → Auto-format Java files (if you have google-java-format)
# 3. PostToolUse → Run integration tests after repository changes

# Test each hook after adding it
```

#### **5. Explore MCP Servers (1 hour)**
- Read: [MCP Tutorial (TowardsDataScience)](https://towardsdatascience.com/model-context-protocol-mcp-tutorial-build-your-first-mcp-server-in-6-steps/)
- Consider: Should you expose your PostgreSQL database via MCP?
- Consider: MCP server for your service layer testing?
- Decide: Is MCP needed now, or later when you have more services?

---

## 📚 **Quick Reference - Bookmark These**

### **Essential Documentation**:
1. 🎯 **[Claude Code Setup](https://docs.claude.com/en/docs/claude-code/setup)** - Installation & authentication
2. 🤖 **[Sub-Agents Guide](https://docs.claude.com/en/docs/claude-code/sub-agents)** - Your most important tool
3. 🪝 **[Hooks Guide](https://docs.claude.com/en/docs/claude-code/hooks-guide)** - Automation workflows
4. 🔌 **[MCP Quickstart](https://modelcontextprotocol.io/quickstart)** - Build your first MCP server
5. ⚙️ **[Claude Code Settings](https://docs.claude.com/en/docs/claude-code/settings)** - Configuration reference

### **Best Tutorials**:
1. 📖 **[MCP Server Tutorial (TowardsDataScience)](https://towardsdatascience.com/model-context-protocol-mcp-tutorial-build-your-first-mcp-server-in-6-steps/)** - 6 steps, very practical
2. 📖 **[Ultimate MCP Guide (Generect)](https://generect.com/blog/claude-mcp/)** - Setup to automation with real examples
3. 📖 **[MCP Guide with Demo (DataCamp)](https://www.datacamp.com/tutorial/mcp-model-context-protocol)** - GitHub & Notion integration
4. 📖 **[Building MCP Server (Medium)](https://heeki.medium.com/building-an-mcp-server-as-an-api-developer-cfc162d06a83)** - API developer perspective

---

## 💡 **Pro Tips for Your Backend Work**

### **1. Start Small**
- Create one sub-agent today (repository-tester)
- Use it for your service layer work
- Add more as you discover needs

### **2. Leverage Context Separation**
- Sub-agents prevent context pollution
- Main conversation stays focused on architecture
- Sub-agents handle implementation details

### **3. Automate Repetitive Tasks**
- Hook to run integration tests after repository changes = instant feedback
- Auto-format prevents manual formatting work
- Block dangerous operations before they happen

### **4. Constitution Integration**
- Make a sub-agent that reads your constitution.md
- Add hooks to validate against constitutional principles
- Let automation enforce your project rules

### **5. MCP for Advanced Integration**
- Start with sub-agents and hooks first
- Add MCP servers when you need database/API integration
- Use MCP to expose your backend to other AI tools

### **6. Version Control Your Agents**
```bash
# Add to git
git add .claude/agents/
git commit -m "Add specialized sub-agents for backend development"

# Share with team
# They can use your agents immediately
```

### **7. Iterative Improvement**
- Start with Claude-generated sub-agents
- Customize based on actual usage
- Add hooks as you discover automation needs
- Measure impact (time saved, errors prevented)

---

## 🎯 **Success Metrics for Today**

By end of day, you should be able to:
- ✅ Launch Claude Code in your backend project
- ✅ Create 2-3 specialized sub-agents
- ✅ Add at least 1 automation hook
- ✅ Understand when to use sub-agents vs main conversation
- ✅ Know how MCP could integrate with your backend
- ✅ Have automated at least one repetitive task

---

## 🔥 **Common Pitfalls to Avoid**

### **1. Too Many Sub-Agents**
- Don't create 20 sub-agents on day 1
- Start with 2-3 focused agents
- Add more as genuine needs emerge

### **2. Overly Complex Hooks**
- Start with simple hooks (logging, formatting)
- Add complexity gradually
- Test each hook thoroughly

### **3. Ignoring Security**
- Always review hook code before adding
- Be cautious with hooks that run bash commands
- Limit hook permissions appropriately

### **4. Context Pollution**
- Use sub-agents for implementation details
- Keep main conversation for architecture
- Don't mix concerns

### **5. MCP Server Complexity**
- Don't build MCP servers before you need them
- Start with built-in tools
- Add MCP when you have clear integration needs

---

## 📞 **Getting Help**

### **Claude Code Issues**:
- Check: `claude doctor` (diagnose issues)
- Docs: [Troubleshooting Guide](https://docs.claude.com/en/docs/claude-code/troubleshooting)
- GitHub: [Claude Code Issues](https://github.com/anthropics/claude-code/issues)

### **MCP Server Issues**:
- Docs: [MCP Documentation](https://modelcontextprotocol.io/)
- GitHub: [MCP Servers Repo](https://github.com/modelcontextprotocol/servers)

### **Your Project Context**:
- Constitution: `.specify/memory/constitution.md`
- Architecture: `specs/001-automated-funding-discovery/`
- Current Work: Service layer development following TDD

---

## 🎊 **Ready to Start!**

**Open Cursor → Terminal → Run**:
```bash
cd ~/github/northstar-funding/backend
claude
```

**First Commands to Try**:
```bash
> Show me the current repository integration test results

> Help me understand the service layer architecture we need to build

> What are the next steps according to our tasks.md?
```

**Then**:
```bash
/agents  # Create your first sub-agent
/hooks   # Add your first automation hook
```

Good luck with your Claude Code journey! 🚀

---

**Document Version**: 1.0  
**Last Updated**: 2025-10-16  
**Next Review**: After first week of Claude Code usage
