# CMS — Central Management Subsystem

**AI-Powered Automated Security Camera Monitoring Platform — Backend**

Central Management Subsystem (CMS) is the orchestrator of a 3-node AI security camera monitoring platform. It manages cameras, users, anomaly events, alerts, video clip storage, push notifications, and analytics.

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Runtime | Java 21, Spring Boot 3.4.3 |
| Database | PostgreSQL 16 |
| Object Storage | MinIO (S3-compatible) |
| Auth | JWT (user + subsystem tokens) |
| Push Notifications | Firebase Cloud Messaging |
| Build | Maven Wrapper |
| Containerization | Docker Compose |

## Quick Start

### Prerequisites

- **Java 21** (JDK)
- **Docker & Docker Compose** (for PostgreSQL and MinIO)

### 1. Clone & Configure

```bash
git clone https://github.com/<org>/cms.git
cd cms
copy .env.example .env        # Windows
# cp .env.example .env        # Linux/Mac
```

Edit `.env` if needed (defaults work for local development).

### 2. Start Infrastructure

```bash
docker compose up -d
```

This starts:
- **PostgreSQL 16** on port `5432`
- **MinIO** on port `9000` (API) and `9001` (Console UI)
- Auto-creates the `anomaly-clips` bucket

### 3. Run the Application

```bash
# Windows
.\mvnw.cmd spring-boot:run

# Linux/Mac
./mvnw spring-boot:run
```

The application starts on **http://localhost:8050**.

On first run, the following seed data is created automatically:
- Admin user: `admin@cms.local` / `admin123`
- AI subsystem credentials for JWT authentication

### 4. Verify

```bash
# Health check
curl http://localhost:8050/api/auth/login -X POST -H "Content-Type: application/json" \
  -d '{"email":"admin@cms.local","password":"admin123"}'
```

MinIO Console: http://localhost:9001 (user: `minio` / pass: `admin123`)

## Project Structure

```
src/main/java/com/bitiriciler32/cms/
├── analytics/       # Daily model summary & reporting
├── anomaly/         # Anomaly event ingestion & user alerts
├── common/          # Shared error handling
├── config/          # Data initializer, app config
├── management/      # Users, cameras, access control
├── media/           # MinIO presigned URL generation
├── notification/    # Firebase push notifications
└── security/        # JWT auth, filters, Spring Security config
```

## Configuration

All secrets and connection details are configurable via environment variables. See [`.env.example`](.env.example) for the full list.

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_URL` | `jdbc:postgresql://localhost:5432/cms_db` | PostgreSQL connection URL |
| `DB_USERNAME` | `cms_user` | Database username |
| `DB_PASSWORD` | `cms_password` | Database password |
| `JWT_SECRET` | *(dev placeholder)* | Base64-encoded JWT signing key |
| `SUBSYSTEM_ID` | `ai-inference-node` | AI node subsystem identifier |
| `SUBSYSTEM_SECRET` | *(dev placeholder)* | AI node authentication secret |
| `MINIO_ENDPOINT` | `http://localhost:9000` | MinIO S3 API endpoint |
| `MINIO_ACCESS_KEY` | `minio` | MinIO access key |
| `MINIO_SECRET_KEY` | `admin123` | MinIO secret key |

## License

This project is developed as a senior capstone project (Bitirme Projesi).

