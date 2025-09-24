# NorthStar Funding Intelligence Platform - Frontend

**Streamlit Admin Interface for Human-AI Collaboration Workflows**

## Overview

This is the Streamlit-based frontend for the NorthStar Funding Intelligence Platform. It provides the administrative interface for human validation and enhancement of AI-discovered funding opportunities.

## Architecture

- **Framework**: Streamlit (>=1.28.0)
- **Development**: MacBook M2 (192.168.1.5) 
- **Backend API**: Spring Boot on Mac Studio (192.168.1.10:8080)
- **Purpose**: Human-AI collaboration workflows per Constitutional Principle III

## Project Structure

```
frontend/
├── app.py                    # Main Streamlit application entry point
├── requirements.txt          # Python dependencies
├── pages/                    # Streamlit pages (multi-page app)
│   ├── discovery_queue.py    # T044: Candidate review queue
│   ├── enhancement.py        # T045: Candidate enhancement interface
│   └── approval.py          # T046: Approval/rejection workflow
├── services/                 # Backend integration services
│   ├── __init__.py
│   └── api_client.py        # T047: API client for Spring Boot backend
└── README.md                # This file
```

## Current Status: Phase 3.1 Complete ✅

### Completed Tasks
- **T003** ✅ Initialize Streamlit project structure
- **T005** ✅ Configure Python requirements (Streamlit, requests, pandas)

### Next Phase: Phase 3.2 - Database Schema
The frontend will remain in basic structure until Phase 3.10 when the actual pages are implemented.

## Development Setup

### Prerequisites
- Python 3.11+
- Backend API running on Mac Studio (Phase 3.8+)

### Installation
```bash
cd /Users/kevin/github/northstar-funding/frontend

# Create virtual environment
python3 -m venv venv

# Activate virtual environment
source venv/bin/activate

# Install dependencies
pip install -r requirements.txt
```

### Running the Application
```bash
# Ensure virtual environment is activated
source venv/bin/activate

# Run Streamlit app
streamlit run app.py
```

The application will be available at: http://localhost:8501

### Deactivating Virtual Environment
```bash
# When finished working, deactivate the virtual environment
deactivate
```

## Constitutional Compliance

### Human-AI Collaboration (Principle III)
- **Discovery Queue**: Human validation of AI-discovered candidates
- **Enhancement**: Human extraction of contact intelligence 
- **Approval**: Human decision-making with AI analysis support

### Technology Stack (Principle IV)
- ✅ Streamlit for rapid UI development
- ✅ Integration with Java 25 + Spring Boot backend
- ✅ Deployment to Mac Studio infrastructure

### Complexity Management (Principle VI)
- ✅ Single responsibility: Admin interface only
- ✅ Clear separation from backend logic
- ✅ Manageable page structure

## Implementation Phases

### Phase 3.10: Streamlit Admin Interface (T043-T047)
- **T043** Main app with navigation (✅ basic structure complete)
- **T044** Candidate review queue page
- **T045** Candidate enhancement page  
- **T046** Approval/rejection workflow page
- **T047** API client service (✅ placeholder complete)

### Phase 3.11: External Integrations
- Integration with LM Studio AI services
- Search engine adapters (Searxng, Tavily, Perplexity)

### Phase 3.12: Polish & Deployment
- Production configuration
- Docker deployment to Mac Studio
- Performance optimization

## API Integration

The frontend communicates with the Spring Boot backend via REST API:

```python
from services.api_client import api_client

# Health check
status = api_client.health_check()

# Get funding candidates (Phase 3.8+)
candidates = api_client.get_candidates()

# Human enhancement workflow
candidate = api_client.get_candidate_detail(candidate_id)
enhanced = api_client.add_contact_intelligence(candidate_id, contact_data)
api_client.approve_candidate(candidate_id)
```

## Human Workflow Integration

Per Constitutional Principle III, every automated process includes human validation:

1. **AI Discovery** → **Human Validation** (Discovery Queue)
2. **AI Analysis** → **Human Enhancement** (Enhancement Page) 
3. **AI Scoring** → **Human Decision** (Approval Workflow)

## Contact Intelligence Priority

Per Constitutional Principle VII, contact information is the highest value asset:

- Contact extraction and validation workflows
- Relationship intelligence tracking
- Communication preference management
- Interaction history and outcome tracking

---

*Phase 3.1 completed - Ready for Phase 3.2: Database Schema (TDD Foundation)*
