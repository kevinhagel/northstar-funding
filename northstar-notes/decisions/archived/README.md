# Archived ADRs

This folder contains Architecture Decision Records that were written for **future features** that have not yet been implemented.

These ADRs document planned architectural decisions but reference code, modules, and features that don't exist in the current codebase.

## Archived ADRs

### 001-text-array-over-jsonb.md
**Reason for Archive**: References `SearchQuery` entity and search execution infrastructure that haven't been implemented yet. The domain model and persistence layer exist, but no search/crawler application code has been written.

**Status**: Will be relevant when implementing Feature 003 (Search Execution Infrastructure) or Feature 004 (Metadata Judging).

**Key Concept**: Use PostgreSQL TEXT[] arrays instead of JSONB for simple string collections to avoid Spring Data JDBC complexity.

---

**Note**: When implementing the features these ADRs describe, review the archived ADRs and either:
1. Update them to match actual implementation
2. Move them back to active decisions/ folder
3. Create new ADRs based on lessons learned
