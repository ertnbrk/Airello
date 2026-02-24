# Airello API Documentation

## Table of Contents
- [Overview](#overview)
- [Authentication](#authentication)
- [Base URL](#base-url)
- [API Endpoints](#api-endpoints)
  - [Authentication & Account](#authentication--account)
  - [User Plan & Quota](#user-plan--quota)
  - [Workspaces](#workspaces)
  - [Projects](#projects)
  - [Project Members](#project-members)
  - [Invitations](#invitations)
  - [Agile Management](#agile-management)
  - [Board Management](#board-management)
  - [Planning](#planning)
  - [Chat](#chat)
  - [AI Features](#ai-features)
  - [Diagrams](#diagrams)
  - [Payments](#payments)
- [Data Models](#data-models)
- [Error Handling](#error-handling)

## Overview

Airello (Planmate API) is a comprehensive project management platform with AI-powered features for agile teams. The API supports workspace management, project planning, sprint management, AI-assisted planning, and team collaboration.

**Technology Stack:**
- Framework: Spring Boot 3.x
- Database: PostgreSQL
- Cache/Queue: Redis
- Authentication: JWT (JSON Web Tokens)
- File Storage: AWS S3 (optional)
- Payments: Stripe (optional)

## Authentication

### Authentication Methods

The API supports multiple authentication methods:

1. **Email/Password** - Standard registration and login
2. **Google OAuth2** - Social authentication via Google
3. **Demo Mode** - Anonymous temporary sessions
4. **JWT Tokens** - Token-based authentication for API requests

### Using JWT Tokens

Include the JWT token in the Authorization header for authenticated requests:

```http
Authorization: Bearer <your_jwt_token>
```

### Token Expiration
- Access Token: 15 minutes (default)
- Refresh Token: 30 days (default)

### Security Notes
- In development mode, authentication can be disabled via `ENABLE_AUTH=false`
- In production, always use HTTPS
- Store tokens securely (httpOnly cookies recommended for web apps)

## Base URL

Development: `http://localhost:8080`
Production: Configure via `SERVER_PORT` environment variable

## API Endpoints

### Authentication & Account

#### Register User
```http
POST /auth/register
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "securepassword123",
  "name": "John Doe"
}
```

**Response:** `201 Created`
```json
{
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
  "user": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "user@example.com",
    "name": "John Doe",
    "provider": "LOCAL",
    "userType": "REGISTERED",
    "plan": "FREE"
  }
}
```

**Validation:**
- Email: Required, valid email format
- Password: Required, minimum 6 characters
- Name: Required

---

#### Login
```http
POST /auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "securepassword123"
}
```

**Response:** `200 OK`
```json
{
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
  "user": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "user@example.com",
    "name": "John Doe",
    "provider": "LOCAL",
    "userType": "REGISTERED",
    "plan": "FREE"
  }
}
```

---

#### Get Current User
```http
GET /auth/me
Authorization: Bearer <token>
```

**Response:** `200 OK`
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "email": "user@example.com",
  "name": "John Doe",
  "provider": "LOCAL",
  "userType": "REGISTERED",
  "plan": "FREE"
}
```

---

#### Create Demo Session
```http
POST /auth/demo
```

**Response:** `201 Created`
```json
{
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
  "user": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "email": null,
    "name": "Demo User",
    "provider": "LOCAL",
    "userType": "ANONYMOUS",
    "plan": "DEMO"
  }
}
```

**Note:** Demo sessions expire after 24 hours (configurable via `DEMO_EXPIRATION_HOURS`)

---

#### Upgrade Anonymous User (Lazy Registration)
```http
POST /auth/register-upgrade
Authorization: Bearer <demo_token>
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "securepassword123",
  "name": "John Doe"
}
```

**Response:** `201 Created`
```json
{
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
  "user": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "user@example.com",
    "name": "John Doe",
    "provider": "LOCAL",
    "userType": "REGISTERED",
    "plan": "FREE"
  }
}
```

---

#### Google OAuth Login
```http
GET /auth/oauth2/google
```

**Response:** Redirects to Google authorization page

---

#### Google OAuth Callback
```http
GET /auth/oauth2/google/callback?code=<authorization_code>
```

**Response:** Redirects to frontend with token in URL

---

#### Export User Data (GDPR)
```http
GET /v1/me/export
Authorization: Bearer <token>
```

**Response:** `200 OK`
```json
{
  "user": {...},
  "workspaces": [...],
  "projects": [...],
  "issues": [...]
}
```

---

#### Delete Account (GDPR)
```http
DELETE /v1/me
Authorization: Bearer <token>
```

**Response:** `204 No Content`

---

### User Plan & Quota

#### Get Quota Status
```http
GET /v1/me/plan
Authorization: Bearer <token>
```

**Response:** `200 OK`
```json
{
  "plan": "FREE",
  "aiRequestsUsedToday": 5,
  "aiRequestsLimitPerDay": 20,
  "canMakeAiRequest": true
}
```

---

#### Upgrade to PRO (Mock Endpoint)
```http
POST /v1/me/plan/upgrade
Authorization: Bearer <token>
```

**Response:** `200 OK`
```json
{
  "plan": "PRO",
  "aiRequestsUsedToday": 5,
  "aiRequestsLimitPerDay": 200,
  "canMakeAiRequest": true
}
```

**Note:** In production, this would integrate with Stripe payments

---

### Workspaces

#### List User Workspaces
```http
GET /v1/workspaces
Authorization: Bearer <token>
```

**Response:** `200 OK`
```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "name": "My Workspace",
    "slug": "my-workspace",
    "description": "Team workspace for product development",
    "ownerId": "550e8400-e29b-41d4-a716-446655440001",
    "ownerName": "John Doe",
    "ownerEmail": "john@example.com",
    "createdAt": "2024-01-15T10:30:00Z",
    "updatedAt": "2024-01-15T10:30:00Z"
  }
]
```

---

#### Create Workspace
```http
POST /v1/workspaces
Authorization: Bearer <token>
Content-Type: application/json

{
  "name": "My Workspace",
  "slug": "my-workspace",
  "description": "Team workspace for product development"
}
```

**Response:** `201 Created`
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "name": "My Workspace",
  "slug": "my-workspace",
  "description": "Team workspace for product development",
  "ownerId": "550e8400-e29b-41d4-a716-446655440001",
  "ownerName": "John Doe",
  "ownerEmail": "john@example.com",
  "createdAt": "2024-01-15T10:30:00Z",
  "updatedAt": "2024-01-15T10:30:00Z"
}
```

**Validation:**
- name: Required, max 255 characters
- slug: Required, lowercase letters/numbers/hyphens only, max 100 characters
- description: Optional, max 5000 characters

---

#### Get Workspace
```http
GET /v1/workspaces/{workspaceId}
Authorization: Bearer <token>
```

**Response:** `200 OK` (same as Create Workspace response)

---

#### Get Workspace Members
```http
GET /v1/workspaces/{workspaceId}/members
Authorization: Bearer <token>
```

**Response:** `200 OK`
```json
[
  {
    "userId": "550e8400-e29b-41d4-a716-446655440001",
    "userName": "John Doe",
    "userEmail": "john@example.com",
    "role": "OWNER",
    "joinedAt": "2024-01-15T10:30:00Z"
  }
]
```

---

#### Add Workspace Member
```http
POST /v1/workspaces/{workspaceId}/members
Authorization: Bearer <token>
Content-Type: application/json

{
  "userId": "550e8400-e29b-41d4-a716-446655440002",
  "role": "EDITOR"
}
```

**Response:** `201 Created`

**Workspace Roles:**
- `OWNER` - Full control, can delete workspace
- `MANAGER` - Can manage members and projects
- `EDITOR` - Can edit issues and artifacts
- `VIEWER` - Read-only access
- `COMMENTER` - Can view and comment

---

#### Remove Workspace Member
```http
DELETE /v1/workspaces/{workspaceId}/members/{userId}
Authorization: Bearer <token>
```

**Response:** `204 No Content`

---

### Projects

#### List Projects
```http
GET /v1/projects?workspaceId={workspaceId}
Authorization: Bearer <token>
```

**Response:** `200 OK`
```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440010",
    "workspaceId": "550e8400-e29b-41d4-a716-446655440000",
    "name": "Mobile App",
    "key": "MOBILE",
    "description": "iOS and Android mobile application",
    "ownerId": "550e8400-e29b-41d4-a716-446655440001",
    "ownerName": "John Doe",
    "defaultVelocity": 35,
    "sprintLengthDays": 14,
    "createdAt": "2024-01-15T10:30:00Z",
    "updatedAt": "2024-01-15T10:30:00Z"
  }
]
```

---

#### Create Project
```http
POST /v1/projects
Authorization: Bearer <token>
Content-Type: application/json

{
  "workspaceId": "550e8400-e29b-41d4-a716-446655440000",
  "name": "Mobile App",
  "key": "MOBILE",
  "description": "iOS and Android mobile application",
  "defaultVelocity": 35,
  "sprintLengthDays": 14
}
```

**Response:** `201 Created` (same as List Projects item)

**Validation:**
- workspaceId: Required, valid UUID
- name: Required, max 255 characters
- key: Required, 2-10 uppercase letters (e.g., "MOBILE", "WEB")
- description: Optional, max 5000 characters
- defaultVelocity: 1-200, default 35
- sprintLengthDays: 1-30 days, default 14

---

#### Get Project
```http
GET /v1/projects/{projectId}
Authorization: Bearer <token>
```

**Response:** `200 OK` (same as Create Project response)

---

#### Upload Artifact (if enabled)
```http
POST /v1/projects/{projectId}/artifacts
Authorization: Bearer <token>
Content-Type: multipart/form-data

file: <binary_file_data>
```

**Response:** `201 Created`
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440020",
  "fileName": "requirements.pdf",
  "fileSize": 1024000,
  "s3Key": "projects/550e8400-e29b-41d4-a716-446655440010/artifacts/requirements.pdf",
  "uploadedAt": "2024-01-15T10:30:00Z"
}
```

**Note:** Requires `ARTIFACTS_ENABLED=true` in configuration

---

### Project Members

#### Get Project Members
```http
GET /v1/projects/{projectId}/members
Authorization: Bearer <token>
```

**Response:** `200 OK`
```json
[
  {
    "userId": "550e8400-e29b-41d4-a716-446655440001",
    "userName": "John Doe",
    "userEmail": "john@example.com",
    "role": "ADMIN",
    "joinedAt": "2024-01-15T10:30:00Z"
  }
]
```

---

#### Add Project Member
```http
POST /v1/projects/{projectId}/members
Authorization: Bearer <token>
Content-Type: application/json

{
  "userId": "550e8400-e29b-41d4-a716-446655440002",
  "role": "MEMBER"
}
```

**Response:** `201 Created`

**Project Roles:**
- `ADMIN` - Full project control, can manage members and settings
- `MEMBER` - Can create and edit issues, manage sprints
- `VIEWER` - Read-only access to project

---

#### Update Project Member
```http
PATCH /v1/projects/{projectId}/members/{userId}
Authorization: Bearer <token>
Content-Type: application/json

{
  "role": "ADMIN"
}
```

**Response:** `200 OK`

---

#### Remove Project Member
```http
DELETE /v1/projects/{projectId}/members/{userId}
Authorization: Bearer <token>
```

**Response:** `204 No Content`

---

### Invitations

#### Create Invitation
```http
POST /v1/projects/{projectId}/invitations
Authorization: Bearer <token>
Content-Type: application/json

{
  "email": "newmember@example.com",
  "role": "MEMBER"
}
```

**Response:** `201 Created`
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440030",
  "token": "inv_abc123xyz456",
  "projectId": "550e8400-e29b-41d4-a716-446655440010",
  "email": "newmember@example.com",
  "role": "MEMBER",
  "status": "PENDING",
  "createdAt": "2024-01-15T10:30:00Z",
  "expiresAt": "2024-01-22T10:30:00Z"
}
```

---

#### Get Invitation
```http
GET /v1/invitations/{token}
```

**Response:** `200 OK` (same as Create Invitation response)

**Note:** This endpoint does not require authentication (allows users to view invitation before accepting)

---

#### Accept Invitation
```http
POST /v1/invitations/{token}/accept
Authorization: Bearer <token>
Content-Type: application/json

{}
```

**Response:** `200 OK`
```json
{
  "userId": "550e8400-e29b-41d4-a716-446655440002",
  "userName": "Jane Smith",
  "userEmail": "newmember@example.com",
  "role": "MEMBER",
  "joinedAt": "2024-01-15T11:00:00Z"
}
```

---

### Agile Management

#### Create Epic
```http
POST /v1/projects/{projectId}/epics
Authorization: Bearer <token>
Content-Type: application/json

{
  "name": "User Authentication",
  "description": "Complete user authentication system",
  "color": "#3B82F6"
}
```

**Response:** `201 Created` (Currently not implemented - returns 501)

---

#### List Epics
```http
GET /v1/projects/{projectId}/epics
Authorization: Bearer <token>
```

**Response:** `200 OK`
```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440040",
    "projectId": "550e8400-e29b-41d4-a716-446655440010",
    "name": "User Authentication",
    "description": "Complete user authentication system",
    "color": "#3B82F6",
    "status": "IN_PROGRESS",
    "createdAt": "2024-01-15T10:30:00Z",
    "updatedAt": "2024-01-15T10:30:00Z"
  }
]
```

---

#### Create Issue
```http
POST /v1/projects/{projectId}/issues
Authorization: Bearer <token>
Content-Type: application/json

{
  "epicId": "550e8400-e29b-41d4-a716-446655440040",
  "type": "STORY",
  "title": "Implement login form",
  "description": "Create responsive login form with email/password fields",
  "priority": "HIGH",
  "storyPoints": 5,
  "assigneeId": "550e8400-e29b-41d4-a716-446655440001",
  "labels": ["frontend", "authentication"],
  "originalEstimateHours": 8.0
}
```

**Response:** `201 Created`
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440050",
  "projectId": "550e8400-e29b-41d4-a716-446655440010",
  "epicId": "550e8400-e29b-41d4-a716-446655440040",
  "key": "MOBILE-1",
  "type": "feature",
  "title": "Implement login form",
  "description": "Create responsive login form with email/password fields",
  "status": "backlog",
  "priority": "high",
  "storyPoints": 5,
  "assigneeId": "550e8400-e29b-41d4-a716-446655440001",
  "reporterId": "550e8400-e29b-41d4-a716-446655440001",
  "labels": ["frontend", "authentication"],
  "originalEstimateHours": 8.0,
  "remainingEstimateHours": 8.0,
  "timeSpentHours": 0.0,
  "orderIndex": 1000.0,
  "createdAt": "2024-01-15T10:30:00Z",
  "updatedAt": "2024-01-15T10:30:00Z"
}
```

**Issue Types:**
- `STORY` - User story (frontend: "feature")
- `TASK` - Task (frontend: "task")
- `BUG` - Bug fix (frontend: "bug")

**Issue Priorities:**
- `LOW` - Low priority (frontend: "low")
- `MEDIUM` - Medium priority (frontend: "medium")
- `HIGH` - High priority (frontend: "high")
- `CRITICAL` - Critical (frontend: "urgent")

**Issue Statuses:**
- `BACKLOG` - In backlog (frontend: "backlog")
- `SELECTED` - Selected for development (frontend: "todo")
- `IN_PROGRESS` - In progress (frontend: "in-progress")
- `REVIEW` - In review (frontend: "review")
- `DONE` - Completed (frontend: "done")

---

#### Update Issue
```http
PATCH /v1/projects/{projectId}/issues/{issueId}
Authorization: Bearer <token>
Content-Type: application/json

{
  "title": "Implement enhanced login form",
  "status": "IN_PROGRESS",
  "storyPoints": 8,
  "timeSpentHours": 3.5
}
```

**Response:** `200 OK` (same as Create Issue response)

---

#### List Issues
```http
GET /v1/projects/{projectId}/issues?status=backlog
Authorization: Bearer <token>
```

**Query Parameters:**
- `status` (optional): Filter by status (BACKLOG, SELECTED, IN_PROGRESS, REVIEW, DONE)

**Response:** `200 OK`
```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440050",
    "projectId": "550e8400-e29b-41d4-a716-446655440010",
    "key": "MOBILE-1",
    "type": "feature",
    "title": "Implement login form",
    "status": "backlog",
    "priority": "high",
    "storyPoints": 5,
    "createdAt": "2024-01-15T10:30:00Z"
  }
]
```

---

#### Create Sprint
```http
POST /v1/projects/{projectId}/sprints
Authorization: Bearer <token>
Content-Type: application/json

{
  "name": "Sprint 1",
  "goal": "Complete user authentication flow",
  "startDate": "2024-01-15",
  "endDate": "2024-01-29"
}
```

**Response:** `201 Created`
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440060",
  "projectId": "550e8400-e29b-41d4-a716-446655440010",
  "name": "Sprint 1",
  "goal": "Complete user authentication flow",
  "startDate": "2024-01-15",
  "endDate": "2024-01-29",
  "status": "planning",
  "createdAt": "2024-01-15T10:30:00Z",
  "updatedAt": "2024-01-15T10:30:00Z"
}
```

**Sprint Statuses:**
- `planning` - Sprint planning phase
- `active` - Sprint in progress
- `completed` - Sprint completed

---

#### List Sprints
```http
GET /v1/projects/{projectId}/sprints
Authorization: Bearer <token>
```

**Response:** `200 OK` (array of Sprint objects)

---

#### Add Issue to Sprint
```http
POST /v1/projects/{projectId}/sprints/{sprintId}/issues/{issueId}
Authorization: Bearer <token>
```

**Response:** `204 No Content`

---

### Board Management

#### Get Board View
```http
GET /v1/projects/{projectId}/board?assignee={userId}&label=frontend
Authorization: Bearer <token>
```

**Query Parameters:**
- `assignee` (optional): Filter by assignee UUID
- `label` (optional): Filter by label

**Response:** `200 OK`
```json
{
  "projectId": "550e8400-e29b-41d4-a716-446655440010",
  "columns": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440070",
      "name": "To Do",
      "position": 0,
      "issues": [
        {
          "id": "550e8400-e29b-41d4-a716-446655440050",
          "key": "MOBILE-1",
          "title": "Implement login form",
          "type": "feature",
          "priority": "high",
          "storyPoints": 5,
          "assigneeId": "550e8400-e29b-41d4-a716-446655440001"
        }
      ]
    }
  ]
}
```

---

#### Create Board Column
```http
POST /v1/projects/{projectId}/board/columns
Authorization: Bearer <token>
Content-Type: application/json

{
  "name": "In QA"
}
```

**Response:** `201 Created`
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440080",
  "projectId": "550e8400-e29b-41d4-a716-446655440010",
  "name": "In QA",
  "position": 3
}
```

---

#### Rename Board Column
```http
PUT /v1/projects/{projectId}/board/columns/{columnId}
Authorization: Bearer <token>
Content-Type: application/json

{
  "name": "Quality Assurance"
}
```

**Response:** `200 OK`

---

#### Delete Board Column
```http
DELETE /v1/projects/{projectId}/board/columns/{columnId}
Authorization: Bearer <token>
```

**Response:** `204 No Content`

---

#### Reorder Board Columns
```http
PUT /v1/projects/{projectId}/board/columns/reorder
Authorization: Bearer <token>
Content-Type: application/json

{
  "columnIds": [
    "550e8400-e29b-41d4-a716-446655440070",
    "550e8400-e29b-41d4-a716-446655440080",
    "550e8400-e29b-41d4-a716-446655440071"
  ]
}
```

**Response:** `200 OK`

---

#### Move Issue on Board
```http
PUT /v1/projects/{projectId}/board/issues/{issueId}/move
Authorization: Bearer <token>
Content-Type: application/json

{
  "targetColumnId": "550e8400-e29b-41d4-a716-446655440080",
  "afterIssueId": "550e8400-e29b-41d4-a716-446655440051",
  "beforeIssueId": "550e8400-e29b-41d4-a716-446655440052"
}
```

**Response:** `200 OK`

**Note:** Use `afterIssueId` and `beforeIssueId` to control positioning within the column

---

### Planning

#### Auto Planning
```http
POST /v1/projects/{projectId}/planning/auto
Authorization: Bearer <token>
```

**Response:** `200 OK`
```json
{
  "success": true,
  "sprintsCreated": 3,
  "issuesPlanned": 24,
  "sprints": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440090",
      "name": "Sprint 1",
      "startDate": "2024-01-15",
      "endDate": "2024-01-29",
      "plannedStoryPoints": 35,
      "issues": [...]
    }
  ]
}
```

**Note:** Uses AI to automatically plan sprints based on project backlog and velocity

---

### Chat

#### Get Chat Threads
```http
GET /v1/projects/{projectId}/chat/threads
Authorization: Bearer <token>
```

**Response:** `200 OK`
```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440100",
    "projectId": "550e8400-e29b-41d4-a716-446655440010",
    "title": "Sprint Planning Discussion",
    "isDefault": false,
    "createdAt": "2024-01-15T10:30:00Z"
  }
]
```

---

#### Get Thread Messages
```http
GET /v1/projects/{projectId}/chat/threads/{threadId}/messages
Authorization: Bearer <token>
```

**Response:** `200 OK`
```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440110",
    "threadId": "550e8400-e29b-41d4-a716-446655440100",
    "userId": "550e8400-e29b-41d4-a716-446655440001",
    "userName": "John Doe",
    "content": "Let's discuss the sprint planning",
    "role": "USER",
    "createdAt": "2024-01-15T10:30:00Z"
  }
]
```

---

#### Send Message
```http
POST /v1/projects/{projectId}/chat/threads/{threadId}/messages
Authorization: Bearer <token>
Content-Type: application/json

{
  "content": "/plan sprint with 5 issues"
}
```

**Response:** `201 Created`
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440111",
  "threadId": "550e8400-e29b-41d4-a716-446655440100",
  "userId": "550e8400-e29b-41d4-a716-446655440001",
  "userName": "John Doe",
  "content": "/plan sprint with 5 issues",
  "role": "USER",
  "createdAt": "2024-01-15T10:31:00Z"
}
```

**Note:** Messages starting with "/" are processed as commands by the AI system

---

### AI Features

#### Start AI Request
```http
POST /v1/projects/{projectId}/ai/start
Authorization: Bearer <token>
Content-Type: application/json

{
  "requestType": "SPRINT_PLANNING",
  "parameters": {
    "issueCount": 10,
    "sprintLength": 14
  }
}
```

**Response:** `202 Accepted`
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440120",
  "correlationId": "corr_abc123xyz456",
  "projectId": "550e8400-e29b-41d4-a716-446655440010",
  "status": "pending",
  "requestType": "SPRINT_PLANNING",
  "parameters": {
    "issueCount": 10,
    "sprintLength": 14
  },
  "createdAt": "2024-01-15T10:30:00Z"
}
```

**Request Types:**
- `SPRINT_PLANNING` - AI-powered sprint planning
- `ISSUE_ESTIMATION` - Estimate story points for issues
- `RISK_ANALYSIS` - Identify project risks
- `BACKLOG_PRIORITIZATION` - Prioritize backlog items

---

#### Get AI Request Status
```http
GET /v1/ai/requests/{correlationId}
Authorization: Bearer <token>
```

**Response:** `200 OK`
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440120",
  "correlationId": "corr_abc123xyz456",
  "projectId": "550e8400-e29b-41d4-a716-446655440010",
  "status": "completed",
  "requestType": "SPRINT_PLANNING",
  "parameters": {
    "issueCount": 10,
    "sprintLength": 14
  },
  "result": {
    "sprints": [...],
    "recommendations": [...]
  },
  "createdAt": "2024-01-15T10:30:00Z",
  "completedAt": "2024-01-15T10:31:00Z"
}
```

**AI Request Statuses:**
- `pending` - Queued for processing
- `processing` - Currently being processed
- `completed` - Successfully completed
- `failed` - Processing failed

---

#### AI Callback (Internal)
```http
POST /v1/ai/callback
Content-Type: application/json

{
  "correlationId": "corr_abc123xyz456",
  "status": "completed",
  "result": {...}
}
```

**Response:** `204 No Content`

**Note:** This endpoint is for internal use by the Python AI worker. In production, should verify HMAC signature.

---

### Diagrams

#### Get Project Diagrams
```http
GET /v1/projects/{projectId}/diagrams
Authorization: Bearer <token>
```

**Response:** `200 OK`
```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440130",
    "projectId": "550e8400-e29b-41d4-a716-446655440010",
    "title": "System Architecture",
    "diagramType": "ARCHITECTURE",
    "mermaidCode": "graph TD\n  A[Client] --> B[API]\n  B --> C[Database]",
    "createdAt": "2024-01-15T10:30:00Z"
  }
]
```

---

#### Generate Diagram
```http
POST /v1/projects/{projectId}/diagrams/generate
Authorization: Bearer <token>
Content-Type: application/json

{
  "prompt": "Create a system architecture diagram showing client, API, and database",
  "diagramType": "ARCHITECTURE"
}
```

**Response:** `201 Created`
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440131",
  "projectId": "550e8400-e29b-41d4-a716-446655440010",
  "title": "System Architecture",
  "diagramType": "ARCHITECTURE",
  "mermaidCode": "graph TD\n  A[Client] --> B[API]\n  B --> C[Database]",
  "createdAt": "2024-01-15T10:30:00Z"
}
```

---

### Payments

**Note:** Payments endpoints are only available when `PAYMENTS_ENABLED=true`

#### Stripe Webhook
```http
POST /payments/stripe/webhook
Stripe-Signature: <stripe_signature>
Content-Type: application/json

<stripe_event_payload>
```

**Response:** `200 OK`

**Note:** Handles Stripe webhook events for subscription management

---

#### Get Billing Portal
```http
GET /payments/billing-portal?returnUrl=https://example.com/settings
Authorization: Bearer <token>
```

**Response:** `200 OK`
```json
{
  "url": "https://billing.stripe.com/session/abc123xyz456"
}
```

---

## Data Models

### User
```typescript
{
  id: UUID
  email: string | null
  name: string
  provider: "LOCAL" | "GOOGLE"
  userType: "REGISTERED" | "ANONYMOUS"
  plan: "FREE" | "DEMO" | "PRO"
}
```

### Workspace
```typescript
{
  id: UUID
  name: string
  slug: string
  description: string | null
  ownerId: UUID
  ownerName: string
  ownerEmail: string
  createdAt: ISO8601
  updatedAt: ISO8601
}
```

### Project
```typescript
{
  id: UUID
  workspaceId: UUID
  name: string
  key: string // 2-10 uppercase letters
  description: string | null
  ownerId: UUID
  ownerName: string
  defaultVelocity: number // 1-200
  sprintLengthDays: number // 1-30
  createdAt: ISO8601
  updatedAt: ISO8601
}
```

### Issue
```typescript
{
  id: UUID
  projectId: UUID
  epicId: UUID | null
  key: string // e.g., "MOBILE-1"
  type: "feature" | "task" | "bug"
  title: string
  description: string | null
  status: "backlog" | "todo" | "in-progress" | "review" | "done"
  priority: "low" | "medium" | "high" | "urgent"
  storyPoints: number | null // 0-100
  assigneeId: UUID | null
  reporterId: UUID
  labels: string[]
  originalEstimateHours: number | null
  remainingEstimateHours: number | null
  timeSpentHours: number
  orderIndex: number
  createdAt: ISO8601
  updatedAt: ISO8601
}
```

### Sprint
```typescript
{
  id: UUID
  projectId: UUID
  name: string
  goal: string | null
  startDate: "YYYY-MM-DD"
  endDate: "YYYY-MM-DD"
  status: "planning" | "active" | "completed"
  createdAt: ISO8601
  updatedAt: ISO8601
}
```

### Epic
```typescript
{
  id: UUID
  projectId: UUID
  name: string
  description: string | null
  color: string // Hex color code
  status: string
  createdAt: ISO8601
  updatedAt: ISO8601
}
```

---

## Error Handling

### Standard Error Response
```json
{
  "timestamp": "2024-01-15T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/v1/projects"
}
```

### HTTP Status Codes

- `200 OK` - Request succeeded
- `201 Created` - Resource created successfully
- `204 No Content` - Request succeeded with no response body
- `400 Bad Request` - Invalid request data
- `401 Unauthorized` - Missing or invalid authentication
- `403 Forbidden` - Insufficient permissions
- `404 Not Found` - Resource not found
- `409 Conflict` - Resource conflict (e.g., duplicate)
- `422 Unprocessable Entity` - Validation error
- `500 Internal Server Error` - Server error
- `503 Service Unavailable` - Service temporarily unavailable

### Common Error Scenarios

#### Authentication Error
```json
{
  "timestamp": "2024-01-15T10:30:00Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid or expired token",
  "path": "/v1/projects"
}
```

#### Validation Error
```json
{
  "timestamp": "2024-01-15T10:30:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Email is required",
  "path": "/auth/register"
}
```

#### Resource Not Found
```json
{
  "timestamp": "2024-01-15T10:30:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "Project not found",
  "path": "/v1/projects/550e8400-e29b-41d4-a716-446655440010"
}
```

#### Permission Denied
```json
{
  "timestamp": "2024-01-15T10:30:00Z",
  "status": 403,
  "error": "Forbidden",
  "message": "Insufficient permissions to access this resource",
  "path": "/v1/workspaces/550e8400-e29b-41d4-a716-446655440000"
}
```

---

## Configuration

### Environment Variables

**Database:**
- `DB_URL` - PostgreSQL connection URL (default: `jdbc:postgresql://localhost:5433/planmate`)
- `DB_USER` - Database username (default: `planmate`)
- `DB_PASS` - Database password (required)

**Redis:**
- `SPRING_DATA_REDIS_HOST` - Redis host (default: `localhost`)
- `SPRING_DATA_REDIS_PORT` - Redis port (default: `6379`)

**JWT:**
- `JWT_SECRET` - Secret key for JWT signing (required)
- `JWT_ACCESS_TOKEN_EXPIRATION` - Access token expiration in ms (default: 900000 / 15 min)
- `JWT_REFRESH_TOKEN_EXPIRATION` - Refresh token expiration in ms (default: 2592000000 / 30 days)

**Google OAuth:**
- `GOOGLE_CLIENT_ID` - Google OAuth client ID (required for OAuth)
- `GOOGLE_CLIENT_SECRET` - Google OAuth client secret (required for OAuth)
- `GOOGLE_REDIRECT_URI` - OAuth callback URL (default: `http://localhost:8080/auth/oauth2/google/callback`)
- `GOOGLE_FRONTEND_REDIRECT_URI` - Frontend redirect after OAuth (default: `http://localhost:5173/auth/callback`)

**Features:**
- `AI_ENABLED` - Enable AI features (default: `true`)
- `PAYMENTS_ENABLED` - Enable Stripe payments (default: `false`)
- `ARTIFACTS_ENABLED` - Enable S3 artifact uploads (default: `false`)
- `REDIS_ENABLED` - Enable Redis caching (default: `true`)
- `ENABLE_AUTH` - Enable JWT authentication (default: `true` in prod, `false` in dev)

**AI:**
- `AI_WORKER_TOKEN` - Token for AI worker authentication (required if AI enabled)
- `AI_SEMANTIC_CACHE_ENABLED` - Enable semantic caching (default: `true`)
- `OPENAI_API_KEY` - OpenAI API key for embeddings (optional)

**AWS (if artifacts enabled):**
- `S3_BUCKET` - S3 bucket name (default: `planmate-artifacts`)
- `AWS_REGION` - AWS region (default: `eu-central-1`)

**Stripe (if payments enabled):**
- `STRIPE_SECRET` - Stripe secret key (required)
- `STRIPE_WEBHOOK_SECRET` - Stripe webhook secret (required)

**CORS:**
- `CORS_ALLOWED_ORIGINS` - Comma-separated list of allowed origins (default: `http://localhost:3000,http://localhost:5173,http://localhost:8080,http://localhost:4200`)

---

## Rate Limiting

### AI Request Quotas

Daily AI request limits by plan:
- **FREE**: 20 requests/day
- **DEMO**: 1 request/day
- **PRO**: 200 requests/day

Check quota status: `GET /v1/me/plan`

---

## Webhooks

### AI Processing Complete
When an AI request completes, the system will update the request status. Poll `GET /v1/ai/requests/{correlationId}` to check completion.

### Stripe Webhooks
Configure Stripe webhooks to point to `POST /payments/stripe/webhook` for subscription events.

---

## Best Practices

1. **Always use HTTPS in production**
2. **Store JWT tokens securely** (httpOnly cookies for web, secure storage for mobile)
3. **Implement token refresh** before access token expires
4. **Handle errors gracefully** with proper user feedback
5. **Use UUIDs** for all resource identifiers
6. **Implement optimistic updates** in UI for better UX
7. **Poll AI request status** with exponential backoff
8. **Validate input** on client-side before API calls
9. **Use websockets** for real-time chat updates (if implemented)
10. **Cache responses** where appropriate to reduce API calls

---

## Support

For issues, questions, or feature requests, contact the development team or create an issue in the project repository.

---

**Last Updated:** 2024-01-15
**API Version:** 1.0
**Base URL:** `http://localhost:8080` (development)
