# NorthStar Funding - Knowledge Vault

Personal workspace for development notes, planning, and decision tracking.

## Folder Structure

### 📅 daily-notes/
Daily work logs, progress tracking, and quick notes from development sessions.

**Usage**: Create one note per day (e.g., `2025-10-30.md`)

### 🤖 session-summaries/
Claude Code session summaries - what was accomplished, decisions made, blockers encountered.

**Usage**: Claude Code writes here after completing major work or feature milestones.

### 🎯 feature-planning/
Work-in-progress planning, brainstorming, and design thoughts before they become formal specs.

**Usage**: Draft ideas here, then move to `/specs` when solidified.

### 🔍 decisions/
Architecture Decision Records (ADRs) and important technical decisions.

**Usage**: Document "why" behind major choices (e.g., "Why we chose TEXT[] over JSONB")

### 📥 inbox/
Quick capture for random thoughts, TODOs, links, and ideas that need processing later.

**Usage**: Brain dump here, then organize into proper folders.

## Links to Project Docs

- [Project Architecture](../docs/architecture-crawler-hybrid.md)
- [Domain Model](../docs/domain-model.md)
- [CLAUDE.md](../CLAUDE.md) - Claude Code project guide
- [Specs](../specs/) - Feature specifications

## Obsidian + Claude Code Workflow

1. **Start of day**: Create daily note, review yesterday's progress
2. **During coding**: Ask Claude Code to write session summaries to `session-summaries/`
3. **Planning features**: Draft in `feature-planning/`, move to `/specs` when ready
4. **Quick thoughts**: Dump in `inbox/`, process weekly
5. **Major decisions**: Document in `decisions/` with context and rationale

## Tips

- Use `[[Wiki Links]]` to connect related notes
- Tag notes with `#feature-003`, `#bug`, `#architecture` for easy filtering
- Link to specific code files: `backend/src/main/java/...`
- Claude Code can read from and write to this vault
