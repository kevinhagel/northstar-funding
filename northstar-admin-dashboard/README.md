# NorthStar Admin Dashboard

Vue 3 + TypeScript + PrimeVue admin dashboard for reviewing funding source candidates.

## Tech Stack

- **Framework**: Vue 3.5+ with Composition API
- **Language**: TypeScript
- **UI Library**: PrimeVue 3.50 (FREE/MIT components)
- **State Management**: Pinia 2.1
- **HTTP Client**: Axios 1.6
- **Routing**: Vue Router 4.2
- **Build Tool**: Vite 6.0

## Prerequisites

- Node.js 20+
- npm or pnpm
- NorthStar REST API running on http://localhost:8080

## Setup

```bash
# Install dependencies
npm install

# Start development server (http://localhost:5173)
npm run dev

# Build for production
npm run build
```

## Development Workflow

1. **Start REST API** (Terminal 1):
   ```bash
   mvn spring-boot:run -pl northstar-rest-api
   ```

2. **Start Vue Dev Server** (Terminal 2):
   ```bash
   cd northstar-admin-dashboard
   npm run dev
   ```

3. **Access Dashboard**: http://localhost:5173

## Features

### Review Queue
- Paginated table of funding source candidates
- Filters: Status, Confidence, Search Engine
- Sort by any column
- Color-coded confidence scores

### Quick Actions
- **View**: Navigate to candidate detail (placeholder)
- **Enhance**: Navigate to enhancement form (placeholder)
- **Approve**: Mark candidate as APPROVED
- **Reject**: Mark candidate as REJECTED and blacklist domain

## API Integration

The dashboard communicates with the REST API at `http://localhost:8080/api`.
Vite dev server proxies `/api` requests to avoid CORS issues.

## License

Proprietary - NorthStar Funding Discovery Platform
