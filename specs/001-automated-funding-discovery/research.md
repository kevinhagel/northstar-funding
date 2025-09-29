# Research & Technical Decisions

**Feature**: Automated Funding Discovery Workflow  
**Date**: 2025-09-22  
**Status**: Complete

## Executive Summary
All technical decisions align with constitutional principles and Kevin's expertise in Java 25 + Spring Boot 3.5.5. Architecture favors simplicity over complexity, with deferred decisions on advanced features until core workflow is validated.

## Technology Stack Decisions

### Backend Framework
**Decision**: Java 25 + Spring Boot 3.5.4 + Maven 3.9.9 monolith  
**Rationale**: 
- Aligns with constitutional Technology Stack principle (NON-NEGOTIABLE)
- Leverages Kevin's expert-level Java and Spring Boot knowledge
- Maven 3.9.9 ensures compatibility with Java 25 and modern Spring Boot features
- Follows Complexity Management principle (single service vs microservices)
- Modern features: Virtual Threads for I/O operations, functional programming with Vavr
**Alternatives Considered**:
- Microservices architecture: Rejected - violates complexity principle (max 4 services)
- Python FastAPI: Rejected - not Kevin's area of expertise, adds learning curve
- Node.js: Rejected - constitutional requirement for Java

### Database Strategy  
**Decision**: PostgreSQL 16 primary, defer Qdrant integration  
**Rationale**:
- PostgreSQL handles structured funding source data, admin workflows, audit trails
- Start simple with relational model, add vector search when RAG functionality needed
- Aligns with existing Mac Studio PostgreSQL instance (192.168.1.10)
**Alternatives Considered**:
- Immediate Qdrant integration: Rejected - premature optimization, RAG not needed for admin workflows
- MySQL/MariaDB: Rejected - PostgreSQL already available and constitutional preference

### UI Framework
**Decision**: Streamlit for admin interface  
**Rationale**:
- Rapid development for internal admin tools (not user-facing)
- Python ecosystem excellent for AI integration components
- Declarative UI paradigm good for review queues and data forms
- Constitutional flexibility: "if superior technology for specific workflow, use it"
**Alternatives Considered**:
- Spring MVC + Thymeleaf: Rejected - slower development for admin interfaces
- React/Vue SPA: Rejected - adds complexity, overkill for internal tools
- JavaFX: Rejected - desktop app not suitable for Mac Studio deployment

### Infrastructure Simplification
**Decision**: No Kafka/Redis initially  
**Rationale**:
- Funding discovery workflow is primarily synchronous
- Human review queue doesn't require async messaging
- Admin workflows are low-concurrency (Kevin & Huw only)
- Follows Complexity Management principle - start minimal, add when needed
**Alternatives Considered**:
- Event-driven architecture with Kafka: Rejected - adds complexity without clear benefit
- Redis for caching: Rejected - premature optimization for admin use case

## AI Integration Strategy

### Search Engine Integration
**Decision**: Multi-engine adapter pattern (Searxng, Tavily, Perplexity)  
**Rationale**:
- Different engines find different types of funding sources
- Adapter pattern allows easy addition/removal of engines
- Constitutional requirement for comprehensive discovery
**Implementation**: Java service classes for each engine with common interface

### LM Studio Integration
**Decision**: HTTP REST client to local LM Studio instance  
**Rationale**:
- Constitutional requirement: "LM Studio locally hosted models, no external LLM dependencies"
- Java HTTP client libraries mature and well-supported
- Local hosting ensures data privacy for sensitive funding intelligence
**Implementation**: Spring RestTemplate or WebClient for AI service calls

## Architecture Patterns

### Domain-Driven Design
**Decision**: Bounded contexts with clean architecture  
**Rationale**:
- Constitutional DDD requirement with ubiquitous language
- "Funding Discovery" is clear bounded context
- Separation of domain logic from infrastructure concerns
**Implementation**:
- Domain layer: Entities, value objects, services
- Application layer: Use cases, orchestration
- Infrastructure layer: Database, external APIs, web controllers

### Data Flow Architecture
**Decision**: Linear pipeline with human checkpoints  
**Rationale**:
- Reflects natural workflow: discover → extract → review → approve
- Human-AI collaboration constitutional requirement
- Avoids complex event sourcing or CQRS patterns
**Implementation**: State machine pattern for candidate status transitions

## Performance & Scalability Decisions

### Threading Model
**Decision**: Java Virtual Threads for I/O operations  
**Rationale**:
- Constitutional requirement: "Use Java 21+ virtual threads for all I/O operations"  
- Perfect for web scraping, database operations, AI service calls
- Simplifies async programming compared to CompletableFuture chains
**Implementation**: Spring Boot 3.x native virtual thread support

### Database Design
**Decision**: Single PostgreSQL database with proper indexing  
**Rationale**:
- Admin workflow scale: hundreds of candidates per day, not thousands per second
- PostgreSQL handles JSON columns for flexible metadata storage
- Proper indexing on discovery_date, confidence_score, status fields
**Implementation**: Spring Data JDBC with repository abstractions and SQL queries
**Reference**: [Spring Data JDBC - Domain Driven Design](https://docs.spring.io/spring-data/relational/reference/jdbc/domain-driven-design.html)

## Security & Privacy Decisions

### Contact Intelligence Protection
**Decision**: Encrypt PII fields at rest  
**Rationale**:
- Constitutional requirement: "Contact information encrypted at rest"
- Contact intelligence is highest value asset requiring protection
- Compliance with privacy requirements for EU contacts
**Implementation**: Service layer encryption/decryption with Spring Data JDBC

### Access Control
**Decision**: Simple role-based authentication  
**Rationale**:
- Only admin users (Kevin & Huw) initially
- Spring Security with in-memory or database user store
- Future expansion to support client user roles
**Implementation**: Spring Security configuration with method-level authorization

## Deployment Strategy

### Development vs Production
**Decision**: MacBook M2 development, Mac Studio production  
**Rationale**:
- Constitutional requirement: "Development on MacBook M2, never run production services"
- rsync deployment to Mac Studio Docker environment
- Clear separation of development and runtime environments
**Implementation**: Docker Compose on Mac Studio, dev profiles on MacBook M2

## Future Architecture Considerations

### RAG Integration (Future)
**Research Finding**: Qdrant integration straightforward when needed  
**Timeline**: After basic workflow validated with admin users  
**Complexity**: Adds 1 service (2/4 services used), maintains constitutional compliance

### Client User Interface (Future)  
**Research Finding**: Natural language query interface will require RAG + Qdrant  
**Timeline**: Phase 2 of project after admin workflows stabilized
**Technology**: Likely Streamlit or modern web framework, not Java-based

### Additional Discovery Workflows (Future)
**Research Finding**: Database Services and Database Discovery workflows can reuse infrastructure  
**Complexity**: Each workflow adds minimal services due to shared components
**Architecture**: Plugin/strategy pattern for different discovery types

---
**All research decisions support constitutional compliance and complexity management while leveraging Kevin's Java expertise.**
