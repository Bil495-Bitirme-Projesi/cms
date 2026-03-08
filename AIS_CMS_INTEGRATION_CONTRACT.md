# AIS ↔ CMS Integration Contract

> **Version:** 1.0-draft  
> **Date:** 2026-03-08  
> **Audience:** AI Inference Subsystem (AIS) developer

This document defines the complete communication contract between the
**AI Inference Subsystem (AIS)** and the **Central Management Subsystem (CMS)**.

---

## Table of Contents

1. [Overview](#1-overview)
2. [Authentication](#2-authentication)
3. [HTTP Endpoints (AIS → CMS)](#3-http-endpoints-ais--cms)
4. [WebSocket Channel (Bidirectional)](#4-websocket-channel-bidirectional)
5. [MinIO Clip Upload Flow](#5-minio-clip-upload-flow)
6. [Error Response Format](#6-error-response-format)
7. [Enum / Constant Value Contracts](#7-enum--constant-value-contracts)
8. [Sequence Diagrams](#8-sequence-diagrams)
9. [Development Environment Setup](#9-development-environment-setup)
10. [Open Questions / TODOs](#10-open-questions--todos)

---

## 1. Overview

```
┌───────────────┐         HTTP (REST)          ┌───────────────┐
│               │ ──────────────────────────▶  │               │
│      AIS      │                              │      CMS      │
│  (AI Inference│ ◀════════════════════════▶   │  (Spring Boot │
│   Subsystem)  │     WebSocket (persistent)   │    Backend)   │
│               │                              │               │
│               │ ─── presigned PUT URL ──▶    │    MinIO      │
└───────────────┘                              └───────────────┘
```

| Channel     | Direction       | Purpose                                      |
|-------------|-----------------|----------------------------------------------|
| HTTP REST   | AIS → CMS       | Event ingestion, presigned upload URL request |
| WebSocket   | Bidirectional   | Camera config sync, camera health status      |
| MinIO (S3)  | AIS → MinIO     | Video clip upload via presigned URL           |

---

## 2. Authentication

AIS must obtain a JWT before making any API call or WebSocket connection.

### 2.1 Obtain Token

```
POST /api/auth/subsystem-login
Content-Type: application/json
```

**Request Body:**
```json
{
  "subsystemId": "ai-inference-node",
  "subsystemSecret": "dev-only-subsystem-secret-change-in-prod"
}
```

**Response (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "expiresInMs": 86400000
}
```

### 2.2 Using the Token

| Channel   | How to send token                                      |
|-----------|--------------------------------------------------------|
| HTTP REST | `Authorization: Bearer <token>` header                 |
| WebSocket | Query parameter: `ws://host:8050/ws/inference-sync?token=<token>` |

### 2.3 Token Lifecycle

- Token is valid for **24 hours** (`subsystem-expiration-ms: 86400000`).
- AIS should re-authenticate before expiry.

### 2.4 Dev-Environment Credentials

| Key               | Default Value (dev only)                     |
|-------------------|----------------------------------------------|
| `subsystemId`     | `ai-inference-node`                          |
| `subsystemSecret` | `dev-only-subsystem-secret-change-in-prod`   |


> **For AIS developers:**
> Do **not** hardcode `subsystemSecret` in source code.
> Read it from an environment variable at runtime:
>
> ```
> CMS_SUBSYSTEM_SECRET=<secret>   # or whatever env var name AIS chooses
> ```


## 3. HTTP Endpoints (AIS → CMS)

### 3.1 Ingest Anomaly Event

Reports a detected anomaly to CMS. CMS creates the event record, assigns alerts
to operators, and triggers push notifications.

```
POST /api/events/ingest
Authorization: Bearer <subsystem-token>
Content-Type: application/json
```

**Request Body:**
```json
{
  "sourceEventId": "ais-evt-20260308-cam5-001",
  "cameraId": 5,
  "timestamp": "2026-03-08T14:30:00Z",
  "score": 0.92,
  "severity": "HIGH",
  "type": "INTRUSION",
  "modelVersion": "yolov8-v2.1.0",
  "clipObjectKey": "cameras/5/events/42.mp4"
}
```

| Field           | Type    | Required | Description                                            |
|-----------------|---------|----------|--------------------------------------------------------|
| `sourceEventId` | String  | ✅       | Unique ID from AIS. Used for idempotency — duplicate submissions return `409 Conflict`. |
| `cameraId`      | Long    | ✅       | Must match a camera registered in CMS.                 |
| `timestamp`     | Instant | ✅       | ISO-8601 UTC. When the anomaly was detected.           |
| `score`         | Double  | ✅       | Confidence score (0.0 – 1.0).                          |
| `severity`      | String  | ✅       | See [Enum contracts](#7-enum--constant-value-contracts). |
| `type`          | String  | ✅       | See [Enum contracts](#7-enum--constant-value-contracts). |
| `modelVersion`  | String  | ❌       | Which AI model produced this detection.                |
| `clipObjectKey` | String  | ❌       | MinIO object key if clip was already uploaded.         |

**Response (201 Created):**
```json
{
  "eventId": 42,
  "status": "CREATED"
}
```

**Error Cases:**

| HTTP Status | Condition                              |
|-------------|----------------------------------------|
| 400         | Validation failed (missing required fields) |
| 401         | Missing or invalid subsystem JWT       |
| 404         | `cameraId` not found in CMS            |
| 409         | `sourceEventId` already exists (duplicate) |

**Idempotency:** If AIS retries a failed request with the same `sourceEventId`,
CMS returns `409 Conflict`. AIS should treat `409` as a successful delivery.

---

### 3.2 Request Presigned Upload URL

Gets a presigned PUT URL for uploading a video clip to MinIO.

```
POST /api/clips/upload-url
Authorization: Bearer <subsystem-token>
Content-Type: application/json
```

**Request Body:**
```json
{
  "cameraId": 5,
  "eventId": 42
}
```

| Field      | Type | Required | Description                                |
|------------|------|----------|--------------------------------------------|
| `cameraId` | Long | ✅       | Camera that produced the clip.             |
| `eventId`  | Long | ✅       | Event ID returned from `/api/events/ingest`. |

**Response (200 OK):**
```json
{
  "objectKey": "cameras/5/events/42.mp4",
  "uploadUrl": "http://localhost:9000/anomaly-clips/cameras/5/events/42.mp4?X-Amz-Algorithm=...",
  "expiresInSeconds": 300
}
```

**Upload the clip:**
```bash
curl -X PUT "<uploadUrl>" \
  -H "Content-Type: video/mp4" \
  --data-binary @clip.mp4
```

The presigned URL expires in **5 minutes** (configurable). No additional auth
headers are needed for the MinIO PUT — the signature is embedded in the URL.

---

## 4. WebSocket Channel (Bidirectional)

### 4.1 Connection

```
ws://localhost:8050/ws/inference-sync?token=<subsystem-jwt>
```

- Authentication is validated during the WebSocket handshake via query param.
- On successful connection, CMS logs the session but does NOT automatically send a snapshot.
  AIS must explicitly request it.

### 4.2 Message Format

All messages are JSON. Every message has a `"type"` field for routing.

```json
{ "type": "<MESSAGE_TYPE>", ...payload... }
```

### 4.3 Messages: AIS → CMS

#### 4.3.1 SNAPSHOT (Request full camera config)

AIS should send this:
- Immediately after WebSocket connection is established
- After any reconnection

```json
{
  "type": "SNAPSHOT"
}
```

**CMS responds** with a `ConfigSnapshot` message (see §4.4.1).

#### 4.3.2 CAMERA_STATUS (Report camera connectivity)

AIS sends this whenever it detects a camera status change, or periodically
as a heartbeat.

```json
{
  "type": "CAMERA_STATUS",
  "cameraId": 5,
  "status": "OFFLINE",
  "reportedAt": "2026-03-08T14:30:00Z"
}
```

| Field        | Type    | Required | Description                                    |
|--------------|---------|----------|------------------------------------------------|
| `cameraId`   | Long    | ✅       | Camera whose status is being reported.         |
| `status`     | String  | ✅       | `"ONLINE"` or `"OFFLINE"`.                     |
| `reportedAt` | Instant | ✅       | ISO-8601 UTC. When AIS observed this status.   |


**When to send:**
- When AIS successfully opens an RTSP stream → `ONLINE`
- When AIS fails to connect or loses an RTSP stream → `OFFLINE`
- **Periodically as a heartbeat** even if status has not changed (agreed interval: see §10 item 3)

**Heartbeat requirement:**
AIS **must** send periodic `CAMERA_STATUS` messages for each active camera regardless
of whether the status has changed.
CMS records `lastHeartbeatAt` on every received message for monitoring purposes.

---

### 4.4 Messages: CMS → AIS

#### 4.4.1 ConfigSnapshot (Full camera list)

Sent in response to a `SNAPSHOT` request.

```json
{
  "cameras": [
    {
      "cameraId": 1,
      "name": "Lobby Camera",
      "rtspUrl": "rtsp://192.168.1.100:554/stream1",
      "detectionEnabled": true,
      "threshold": 0.7
    },
    {
      "cameraId": 5,
      "name": "Parking Lot",
      "rtspUrl": "rtsp://192.168.1.105:554/stream1",
      "detectionEnabled": false,
      "threshold": 0.5
    }
  ]
}
```

| Field              | Type    | Description                                               |
|--------------------|---------|-----------------------------------------------------------|
| `cameraId`         | Long    | Unique camera ID.                                         |
| `name`             | String  | Human-readable name.                                      |
| `rtspUrl`          | String  | RTSP stream address to connect to.                        |
| `detectionEnabled` | Boolean | If `false`, AIS should NOT process this camera's stream.  |
| `threshold`        | Double  | Minimum confidence score to report an anomaly.            |

**AIS should:**
- Start/stop RTSP streams accordingly.
- Respect `detectionEnabled` — do not process disabled cameras.

#### 4.4.2 CameraDelta (Incremental update)

Pushed by CMS whenever an admin adds, updates, or deletes a camera.
AIS does NOT need to request this — it arrives automatically.

**UPSERT (camera added or updated):**
```json
{
  "changeType": "UPSERT",
  "cameraId": 5,
  "camera": {
    "cameraId": 5,
    "name": "Parking Lot (Updated)",
    "rtspUrl": "rtsp://192.168.1.105:554/stream2",
    "detectionEnabled": true,
    "threshold": 0.8
  }
}
```

**DELETE (camera removed):**
```json
{
  "changeType": "DELETE",
  "cameraId": 5,
  "camera": null
}
```

**AIS should:**
- On `UPSERT`: add or update the camera in its local config. Restart stream if `rtspUrl` changed.
- On `DELETE`: stop the stream and remove the camera from its local config.

---

## 5. MinIO Clip Upload Flow

Complete sequence for uploading a video clip:

```
1. AIS detects anomaly
2. AIS → CMS:  POST /api/clips/upload-url { cameraId: 5, eventId: 42 }
3. CMS → AIS:  { objectKey: "cameras/5/events/42.mp4", uploadUrl: "http://minio:9000/...", expiresInSeconds: 300 }
4. AIS → MinIO: PUT <uploadUrl> with video binary (Content-Type: video/mp4)
5. AIS → CMS:  POST /api/events/ingest { ..., clipObjectKey: "cameras/5/events/42.mp4" }
```

## 6. Error Response Format

All HTTP error responses follow a consistent format:

```json
{
  "timestamp": "2026-03-08T14:30:00.123Z",
  "status": 400,
  "error": "Validation Failed",
  "message": "sourceEventId is required; score is required",
  "path": "/api/events/ingest"
}
```

| Field       | Type    | Description                              |
|-------------|---------|------------------------------------------|
| `timestamp` | Instant | ISO-8601 UTC when the error occurred.    |
| `status`    | int     | HTTP status code.                        |
| `error`     | String  | Short error category.                    |
| `message`   | String  | Human-readable detail.                   |
| `path`      | String  | Request path that caused the error.      |

### Common Error Codes

| Status | Error                 | When                                           |
|--------|-----------------------|------------------------------------------------|
| 400    | Validation Failed     | Missing or invalid required fields.            |
| 400    | Bad Request           | Illegal argument values.                       |
| 401    | Unauthorized          | Missing/invalid/expired JWT.                   |
| 403    | Forbidden             | Token valid but insufficient scope/role.       |
| 404    | Not Found             | Referenced resource (camera, event) not found. |
| 409    | Conflict              | Duplicate `sourceEventId` (idempotency).       |
| 500    | Internal Server Error | Unexpected CMS failure.                        |

---

## 7. Enum / Constant Value Contracts

> ⚠️ **Important:** The values listed in this section represent the CMS-side
> understanding at the time of writing. The definitive values for `severity`,
> `type`, and other AIS-originated fields **will be determined by the AIS
> team** based on what the model actually detects and how it classifies events.
> Do **not** implement AIS behaviour directly from this document — treat these
> as a starting point for discussion. Agreed values should be reflected back
> into this document before either side finalises their implementation.

### 7.1 Severity

Used in `EventIngestRequest.severity`. Currently stored as a plain String.

| Value      | Meaning                            |
|------------|------------------------------------|
| `LOW`      | Low-confidence or minor anomaly.   |
| `MEDIUM`   | Moderate confidence anomaly.       |
| `HIGH`     | High-confidence anomaly.           |
| `CRITICAL` | Immediate attention required.      |

> **TODO:** Finalize whether these are the only allowed values or if AIS can
> send arbitrary strings. Consider making this a strict enum.

### 7.2 Event Type

Used in `EventIngestRequest.type`. Currently stored as a plain String.

| Value (example)        | Meaning                                  |
|------------------------|------------------------------------------|
| `INTRUSION`            | Unauthorized person in restricted area.  |
| `LOITERING`            | Prolonged presence in a monitored zone.  |
| `OBJECT_LEFT_BEHIND`   | Unattended object detected.              |
| `VIOLENCE`             | Physical altercation detected.           |
| ...                    | (to be extended as model capabilities grow) |

> **TODO:** Agree on a definitive list. CMS currently stores this as a free-form
> string, but the mobile app and analytics dashboard need a known set of values.

### 7.3 Camera Status (WebSocket)

Used in `CAMERA_STATUS` WebSocket messages.

| Value     | Meaning                                  |
|-----------|------------------------------------------|
| `ONLINE`  | AIS is receiving the RTSP stream.        |
| `OFFLINE` | AIS cannot reach the camera.             |

### 7.4 Camera Config Fields

Sent from CMS to AIS in `ConfigSnapshot` and `CameraDelta` messages.

| Field              | How AIS should use it                                    |
|--------------------|----------------------------------------------------------|
| `detectionEnabled` | `false` → do NOT run detection on this camera's stream   |
| `threshold`        | Only report anomalies with `score >= threshold`          |

---

## 8. Sequence Diagrams

### 8.1 Startup / Reconnect

```
AIS                              CMS
 │                                 │
 │──── WebSocket connect ─────────▶│  (ws://host:8050/ws/inference-sync?token=...)
 │                                 │  validates token at handshake
 │◀──── connection established ────│
 │                                 │
 │──── { "type": "SNAPSHOT" } ────▶│
 │                                 │
 │◀─── { "cameras": [...] } ──────│  full camera config
 │                                 │
 │  (AIS starts RTSP streams)      │
 │                                 │
 │──── CAMERA_STATUS ONLINE ──────▶│  per camera
 │──── CAMERA_STATUS ONLINE ──────▶│
 │──── CAMERA_STATUS OFFLINE ─────▶│  if a camera is unreachable
```

### 8.2 Anomaly Detection

```
AIS                              CMS                    MinIO
 │                                 │                       │
 │  (anomaly detected on cam 5)    │                       │
 │                                 │                       │
 │── POST /clips/upload-url ──────▶│                       │
 │◀── { uploadUrl, objectKey } ────│                       │
 │                                 │                       │
 │── PUT <uploadUrl> ─────────────────────────────────────▶│  upload clip
 │◀── 200 OK ──────────────────────────────────────────────│
 │                                 │                       │
 │── POST /events/ingest ─────────▶│                       │
 │   { sourceEventId, cameraId,    │                       │
 │     score, severity, type,      │                       │
 │     clipObjectKey }             │                       │
 │                                 │──▶ save event         │
 │                                 │──▶ create alerts      │
 │                                 │──▶ push notifications │
 │◀── 201 { eventId: 42 } ────────│                       │
```

### 8.3 Camera Config Change (Admin Action)

```
Admin App                        CMS                         AIS
 │                                 │                           │
 │── PUT /api/admin/cameras/5 ────▶│                           │
 │   { threshold: 0.8 }           │                           │
 │                                 │──▶ update DB              │
 │◀── 200 OK ─────────────────────│                           │
 │                                 │                           │
 │                                 │── CameraDelta UPSERT ────▶│
 │                                 │   (via WebSocket)         │
 │                                 │                           │
 │                                 │   (AIS updates threshold) │
```

---

## 9. Development Environment Setup

### 9.1 CMS Address

| Service     | URL                              |
|-------------|----------------------------------|
| CMS API     | `http://localhost:8050`           |
| WebSocket   | `ws://localhost:8050/ws/inference-sync` |
| MinIO API   | `http://localhost:9000`           |
| MinIO Console | `http://localhost:9001`         |

### 9.2 Quick Test: Obtain Token

```bash
curl -s -X POST http://localhost:8050/api/auth/subsystem-login \
  -H "Content-Type: application/json" \
  -d '{
    "subsystemId": "ai-inference-node",
    "subsystemSecret": "dev-only-subsystem-secret-change-in-prod"
  }' | jq .
```

### 9.3 Quick Test: Ingest Event

```bash
TOKEN="<token from above>"

curl -s -X POST http://localhost:8050/api/events/ingest \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "sourceEventId": "test-001",
    "cameraId": 1,
    "timestamp": "2026-03-08T14:30:00Z",
    "score": 0.85,
    "severity": "HIGH",
    "type": "INTRUSION",
    "modelVersion": "yolov8-v2.1.0",
    "clipObjectKey": "cameras/1/events/1.mp4"
  }' | jq .
```

### 9.4 Quick Test: WebSocket

Using `websocat` or any WebSocket client:
```bash
websocat "ws://localhost:8050/ws/inference-sync?token=$TOKEN"

# Then type:
{"type":"SNAPSHOT"}

# To report camera status:
{"type":"CAMERA_STATUS","cameraId":1,"status":"ONLINE","reportedAt":"2026-03-08T14:30:00Z"}
```

---

## 10. Open Questions / TODOs

These are items that need to be agreed upon between CMS and AIS developers:

| # | Topic | Question | Current State |
|---|-------|----------|---------------|
| 1 | **Severity values** | What are the exact allowed severity values? Should CMS enforce a strict enum? | Free-form String. CMS stores whatever AIS sends. |
| 2 | **Event type values** | What anomaly types will the model detect? | Free-form String. Need a definitive list for UI/analytics. |
| 3 | **Heartbeat interval** | Should AIS send periodic CAMERA_STATUS even if nothing changed? How often? | Not enforced. Recommended: every 60s. |
| 4 | **Clip upload ordering** | Should AIS always upload clip before ingesting event, or is ingest-first acceptable? | Both work, but ingest-first requires deterministic objectKey. |
| 5 | **Multiple AIS nodes** | Will there ever be more than one AIS instance? This affects WebSocket broadcast and status reporting. | Currently single-node assumed. |
| 6 | **detectionEnabled=false** | Should AIS keep the RTSP connection alive for a disabled camera (to report ONLINE/OFFLINE), or disconnect entirely? | Not specified. CMS doesn't care — it just flags status. |
| 7 | **Token refresh** | Should AIS proactively refresh the JWT, or just re-login when a 401 is received? | Either works. Proactive recommended. |
| 8 | **WebSocket reconnection** | Backoff strategy on disconnect? CMS sends nothing on reconnect — AIS must re-send SNAPSHOT request. | AIS responsibility. Exponential backoff recommended. |
| 9 | **Clip file format** | Always `.mp4`? Or can it be `.avi`, `.mkv`? | objectKey hardcoded as `.mp4` in CMS. |
| 10 | **Score range** | Is score always 0.0–1.0, or can the model output arbitrary ranges? | CMS stores as-is. Mobile app displays as percentage. |
| 11 | **Secret sharing** | How will `subsystemSecret` be shared across environments? | To be agreed: team password manager, shared `.env`, or CI/CD secret store. Both CMS (`SUBSYSTEM_SECRET`) and AIS must use the same value per environment. |

