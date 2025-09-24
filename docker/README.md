# NorthStar Funding Discovery - Docker Infrastructure

**Fresh Docker setup for Mac Studio deployment**  
**Constitutional Compliance: Java 25 + Spring Boot 3.5.5 + PostgreSQL + Clean Architecture**

## 🎯 **Overview**

This directory contains the complete Docker infrastructure for the NorthStar Funding Discovery platform, designed to run on Mac Studio (192.168.1.10) with management from MacBook M2.

### **Services Included**

**Core Infrastructure:**
- ✅ **PostgreSQL 16** - Primary database with DDD schema
- ✅ **Qdrant** - Vector database for future RAG functionality  
- ✅ **LM Studio (Ollama)** - Local AI models for enhancement

**Application Services:**
- ✅ **NorthStar Backend** - Spring Boot 3.5.5 + Java 25 API
- ✅ **NorthStar Frontend** - Streamlit admin interface

**Supporting Services:**
- ✅ **SearXNG** - Open source search engine for discovery
- ✅ **Health Monitor** - Infrastructure health checking

## 🏗️ **Architecture**

```
MacBook M2 (Development)        Mac Studio (Production)
├── Code Development            ├── Docker Containers
├── Docker Compose Files   →   ├── PostgreSQL 16
└── Deployment Scripts          ├── Spring Boot Backend
                               ├── Streamlit Frontend  
                               ├── Qdrant Vector DB
                               ├── LM Studio (AI)
                               └── SearXNG (Search)
```

## 📋 **Prerequisites**

### **MacBook M2 Requirements:**
- Docker installed and running
- SSH access to Mac Studio (192.168.1.10)
- NorthStar project code in `~/github/northstar-funding/`

### **Mac Studio Requirements:**
- Docker installed and running  
- SSH server enabled
- All previous containers stopped (✅ completed)

## 🚀 **Quick Start**

### **1. Deploy to Mac Studio**

```bash
# From MacBook M2, copy infrastructure to Mac Studio
cd ~/northstar
scp -r . 192.168.1.10:~/northstar-infra/

# SSH to Mac Studio and deploy
ssh 192.168.1.10
cd ~/northstar-infra
docker compose up -d
```

### **2. Verify Deployment**

```bash
# Check all services are running
docker compose ps

# Watch logs
docker compose logs -f

# Check health status
curl http://192.168.1.10:5432  # PostgreSQL
curl http://192.168.1.10:6333/health  # Qdrant
curl http://192.168.1.10:8080/api/actuator/health  # Backend
curl http://192.168.1.10:8501  # Frontend
```

### **3. Access Services**

- **PostgreSQL**: `192.168.1.10:5432` (northstar_user/northstar_password)
- **Backend API**: `http://192.168.1.10:8080/api`
- **Admin UI**: `http://192.168.1.10:8501`
- **Qdrant**: `http://192.168.1.10:6333`
- **LM Studio**: `http://192.168.1.10:11434`
- **SearXNG**: `http://192.168.1.10:8082`

## 📁 **File Structure**

```
~/northstar/
├── docker-compose.yml          # Main infrastructure definition
├── .env                        # Environment configuration
├── config/
│   ├── init-db.sql            # PostgreSQL initialization
│   └── searxng/               # SearXNG configuration
├── scripts/
│   ├── deploy.sh              # Deployment automation
│   ├── backup.sh              # Database backup
│   └── monitor.sh             # Health monitoring
└── README.md                  # This file
```

## 🔧 **Management Commands**

### **Start All Services**
```bash
ssh 192.168.1.10 "cd ~/northstar-infra && docker compose up -d"
```

### **Stop All Services**  
```bash
ssh 192.168.1.10 "cd ~/northstar-infra && docker compose down"
```

### **View Logs**
```bash
ssh 192.168.1.10 "cd ~/northstar-infra && docker compose logs -f [service-name]"
```

### **Update Application**
```bash
# After code changes, rebuild and deploy
cd ~/northstar
scp -r . 192.168.1.10:~/northstar-infra/
ssh 192.168.1.10 "cd ~/northstar-infra && docker compose up -d --build northstar-backend northstar-frontend"
```

## 🗄️ **Database Management**

### **Connect to PostgreSQL**
```bash
# From Mac Studio
docker exec -it northstar-postgres psql -U northstar_user -d northstar_funding

# From MacBook M2 (remote)
psql -h 192.168.1.10 -p 5432 -U northstar_user -d northstar_funding
```

### **Database Backup**
```bash
ssh 192.168.1.10 "docker exec northstar-postgres pg_dump -U northstar_user northstar_funding > ~/backups/northstar-$(date +%Y%m%d).sql"
```

### **Database Restore**
```bash
ssh 192.168.1.10 "docker exec -i northstar-postgres psql -U northstar_user northstar_funding < ~/backups/northstar-backup.sql"
```

## 🔒 **Security Configuration**

### **Environment Variables**
Update `.env` file with secure passwords:
```bash
# Generate secure passwords
openssl rand -base64 32  # For ADMIN_PASSWORD
openssl rand -base64 32  # For JWT_SECRET
```

### **Contact Intelligence Encryption**
Database includes encrypted fields for PII (constitutional requirement):
- Email addresses encrypted with AES-256-GCM
- Phone numbers encrypted at rest
- Encryption keys managed through environment variables

## 📊 **Monitoring & Health Checks**

### **Service Health**
All services include health checks:
- **PostgreSQL**: Connection test every 30s
- **Qdrant**: API health endpoint every 30s  
- **Backend**: Spring Boot actuator health every 30s
- **Frontend**: Streamlit health check every 30s

### **Logs**
- **Log Rotation**: 10MB max size, 3 files retained
- **Centralized Logging**: JSON format for easy parsing
- **Log Access**: `docker compose logs -f [service]`

## 🎯 **Next Steps**

Once infrastructure is running:

1. **Complete Backend Development** (Tasks T007-T012: Database migrations)
2. **Build Docker Images** (Tasks T052-T053: Application containerization)  
3. **Deploy Applications** (Tasks T053-T054: End-to-end validation)

## 🆘 **Troubleshooting**

### **Common Issues**

**PostgreSQL Connection Failed:**
```bash
# Check if PostgreSQL is running
docker compose ps postgres
# Check logs
docker compose logs postgres
```

**Backend Won't Start:**
```bash
# Check database connectivity
docker compose logs northstar-backend
# Verify environment variables
docker compose config
```

**Frontend UI Not Loading:**
```bash
# Check Streamlit health
curl http://192.168.1.10:8501/_stcore/health
# Check backend API availability
curl http://192.168.1.10:8080/api/actuator/health
```

## ✅ **Constitutional Compliance Verification**

- ✅ **Technology Stack**: Java 25 + Spring Boot 3.5.5 + Maven 3.9.9 + PostgreSQL 16
- ✅ **Infrastructure Integration**: Mac Studio deployment (192.168.1.10)
- ✅ **Complexity Management**: Clean service boundaries, single network
- ✅ **Contact Intelligence**: Encrypted PII fields in database
- ✅ **Domain-Driven Design**: Proper aggregate boundaries in schema
- ✅ **Human-AI Collaboration**: Admin user management and audit trails

**Infrastructure is ready for NorthStar Funding Discovery implementation! 🚀**
