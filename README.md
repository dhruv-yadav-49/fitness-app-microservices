# 🏃‍♂️ AI-Powered Fitness Tracker Microservices

An enterprise-grade, event-driven microservices application for tracking fitness activities and generating personalized, AI-driven training recommendations. Built with **Spring Boot & Cloud**, **React**, **MongoDB**, **PostgreSQL**, **Keycloak** for OAuth2/OIDC security, **RabbitMQ** for event messaging, and the **Google Gemini API** for personalized AI recommendations.

---

## 🏗️ System Architecture

The following diagram illustrates the flow of a user logging in, saving an activity, automatically syncing user profiles, and receiving real-time AI training recommendations:

```mermaid
sequenceDiagram
    autonumber
    actor User as Athlete (Browser)
    participant Keycloak as Keycloak Auth Server (8181)
    participant Gateway as API Gateway (8080)
    participant Eureka as Eureka Registry (8761)
    participant UserSvc as User Service (8081)
    participant ActSvc as Activity Service (8082)
    participant AISvc as AI Recommendation Service (8083)
    participant RabbitMQ as RabbitMQ Broker (5672)
    database Postgres as PostgreSQL (Users DB)
    database Mongo as MongoDB (Activity & AI DB)
    participant Gemini as Google Gemini API

    %% Authentication Flow
    User->>Keycloak: Authenticate (OAuth2 PKCE)
    Keycloak-->>User: JWT Access Token (claims: sub, email, names)
    
    %% API Request & Lazy Sync Flow
    User->>Gateway: POST /api/activities (Header: Bearer Token)
    Note over Gateway: KeycloakUserSyncFilter intercepts request
    Gateway->>UserSvc: GET /api/users/{userId}/validate
    UserSvc-->>Gateway: Return false (User not in DB yet)
    
    Gateway->>UserSvc: POST /api/users/register (lazy sync from JWT claims)
    UserSvc->>Postgres: Insert user profile
    UserSvc-->>Gateway: User registered successfully
    
    %% Gateway Routing & Downstream Validation
    Gateway->>ActSvc: Forward POST /api/activities (injected X-User-ID)
    ActSvc->>UserSvc: WebClient: validateUser(userId)
    UserSvc-->>ActSvc: Return true (Valid User)
    ActSvc->>Mongo: Save Activity details
    ActSvc-->>Gateway: Return ActivityResponse
    Gateway-->>User: Return tracked activity response
    
    %% Async Event-Driven AI Generation
    ActSvc->>RabbitMQ: Publish SavedActivity (activity.exchange)
    RabbitMQ->>AISvc: Consume from activity.queue
    
    alt Gemini API Key Configured
        AISvc->>Gemini: Request structured fitness analysis
        Gemini-->>AISvc: Return JSON (recommendation, improvements, safety, etc.)
    else Fallback Mode
        Note over AISvc: Generate simulated recommendation based on activity type
    end
    
    AISvc->>Mongo: Save Recommendation in Database
    
    %% Fetching Recommendation
    User->>Gateway: GET /api/recommendations/activity/{id}
    Gateway->>AISvc: Forward GET /api/recommendations/activity/{id}
    AISvc->>Mongo: Retrieve Recommendation
    AISvc-->>Gateway: Return Recommendation details
    Gateway-->>User: Display AI tips, safety, and training suggestions
```

---

## 🛠️ Technology Stack & Service Matrix

### External Infrastructure (Dockerized)
* **Keycloak (26.6.2)**: User authentication, authorization, token issuer, and role management.
* **PostgreSQL (15)**: Relational database storing user profiles.
* **MongoDB (6.0)**: Document store for activity history and generated AI recommendations.
* **RabbitMQ (3-management)**: Message broker orchestrating async communication between services.

### Backend Microservices (Spring Boot 3.x / Java 21 / Maven)
| Service Name | Port | Database / Broker | Key Technologies | Description |
| :--- | :--- | :--- | :--- | :--- |
| **`configserver`** | `8888` | None | Spring Cloud Config | Server configuration externalized locally in `classpath:/config`. |
| **`eureka`** | `8761` | None | Spring Cloud Netflix Eureka | Dynamic service registry and discovery server. |
| **`gateway`** | `8080` | None | Spring Cloud Gateway, Reactive Security, WebFlux | Central routing entry point, OAuth2 JWT token validator, CORS controller, and Keycloak user synchronizer. |
| **`userservice`** | `8081` | PostgreSQL | Spring Data JPA, Hibernate | Manages user registration, user metadata, and validation. |
| **`activityservice`** | `8082` | MongoDB, RabbitMQ | Spring Data MongoDB, WebClient | Tracks and saves fitness activities, publishes events to RabbitMQ, and queries the User Service. |
| **`aiservice`** | `8083` | MongoDB, RabbitMQ | Spring AMQP, RestTemplate | Listens to activity events, queries Google Gemini API for structured recommendations, and stores them. |

### Frontend Application (Vite / React 19)
* **`fitness-frontend`** (`http://localhost:5173`): Single-page React application using Material UI (MUI), Redux Toolkit for state management, React Router for navigation, and `react-oauth2-code-pkce` for authorization code flow with PKCE authentication.

---

## 🚀 Getting Started

### Prerequisites
1. **Java Development Kit (JDK) 21** installed.
2. **Node.js** (v18+) and **npm** installed.
3. **Docker** and **Docker Compose** installed.
4. **Google Gemini API Key** (Optional, fallback simulator included).

---

### Step 1: Start Infrastructure Containers
Bring up PostgreSQL, MongoDB, RabbitMQ, and Keycloak in the background:
```bash
docker-compose up -d
```
Check the status of the containers using:
```bash
docker-compose ps
```

---

### Step 2: Configure Keycloak Realm & Clients
1. Open the Keycloak Admin Console at [http://localhost:8181](http://localhost:8181).
2. Log in using the bootstrap credentials:
   * **Username**: `admin`
   * **Password**: `admin`
3. Hover over the top-left dropdown showing **Master** and click **Create Realm**.
   * **Realm name**: `fitness-app`
   * Click **Create**.
4. Go to **Clients** from the left-hand navigation and click **Create client**:
   * **Client type**: `OpenID Connect`
   * **Client ID**: `fitness-app`
   * Click **Next**.
   * Ensure **Standard Flow** and **Direct Access Grants** are checked.
   * Click **Next**.
   * Configure the client redirect settings:
     * **Root URL**: `http://localhost:5173`
     * **Valid redirect URIs**: `http://localhost:5173/*`
     * **Web origins**: `http://localhost:5173`
   * Click **Save**.
5. Go to **Users** -> **Add user** to create a test account:
   * **Username**: `fituser`
   * **Email**: `user@fitness.com`
   * **First Name**: `Fit`
   * **Last Name**: `User`
   * Click **Create**.
   * Go to the **Credentials** tab, click **Set password**:
     * Enter a password (e.g. `password`).
     * Turn **Temporary** to `Off`.
     * Click **Save**.

---

### Step 3: Start the Backend Services
Services must be started in a specific order so configuration and registry parameters are available downstream.

#### 1. Config Server (Port 8888)
```bash
cd configserver
./mvnw spring-boot:run
```

#### 2. Eureka Service Registry (Port 8761)
Ensure `configserver` is up, then start:
```bash
cd ../eureka
./mvnw spring-boot:run
```
*Verify eureka status by visiting: [http://localhost:8761](http://localhost:8761)*

#### 3. Core Downstream Services
Open separate terminals for each service:
* **User Service** (Port 8081):
  ```bash
  cd userservice
  ./mvnw spring-boot:run
  ```
* **Activity Service** (Port 8082):
  ```bash
  cd activityservice
  ./mvnw spring-boot:run
  ```
* **AI Service** (Port 8083):
  Set your Gemini API Key in the environment to connect to Google's generative models:
  ```bash
  # Windows CMD
  set GEMINI_API_KEY=your_actual_gemini_api_key_here

  # Windows PowerShell
  $env:GEMINI_API_KEY="your_actual_gemini_api_key_here"

  # Linux / macOS
  export GEMINI_API_KEY="your_actual_gemini_api_key_here"
  ```
  Now run the service:
  ```bash
  cd aiservice
  ./mvnw spring-boot:run
  ```
  *Note: If no API key is specified, the system will log a warning and fallback gracefully to an offline simulated recommendation engine based on activity types.*

#### 4. API Gateway (Port 8080)
Ensure all previous services are running and registered in the Eureka console before running the gateway:
```bash
cd gateway
./mvnw spring-boot:run
```

---

### Step 4: Run the Frontend App
1. Go to the frontend directory:
   ```bash
   cd fitness-frontend
   ```
2. Install client-side dependencies:
   ```bash
   npm install
   ```
3. Launch the development server:
   ```bash
   npm run dev
   ```
4. Access the web app in your browser at [http://localhost:5173](http://localhost:5173).

---

## 🔒 Security & User Sync Flow
The system utilizes a central **reactive WebFilter** in the API Gateway (`KeycloakUserSyncFilter`) to implement lazy user synchronization:

1. A request is received with an OAuth2 JWT token.
2. The Gateway extracts claims (`sub` as `keycloakId`, `email`, `given_name`, `family_name`).
3. The Gateway queries the User Service (`/api/users/{userId}/validate`) to check if the user profile already exists in the PostgreSQL database.
4. If not found, the Gateway immediately fires a background registration request (`POST /api/users/register`) using the extracted claims, initializing their user profile.
5. The Gateway mutates the request header to include `X-User-ID` containing the Keycloak user UUID (`sub`), forwarding it downstream.
6. Downstream microservices only need to read the `X-User-ID` header to identify the context user.

---

## 🔌 API Endpoint Reference

All endpoints (except Eureka and Config Server) are accessed through the API Gateway at `http://localhost:8080` and require an `Authorization` header containing the JWT token.

### 👤 User Service (`/api/users/**`)
* `GET /api/users/{userId}`: Fetch the user profile.
* `POST /api/users/register`: Manually save a user profile.
* `GET /api/users/{userId}/validate`: Returns a boolean indicating if the user exists.

### 🚴‍♂️ Activity Service (`/api/activities/**`)
* `POST /api/activities`: Track a new activity.
  * Headers: `X-User-ID: <user-uuid>`
  * JSON Request Body:
    ```json
    {
      "type": "Running",
      "duration": 45,
      "caloriesBurned": 480,
      "startTime": "2026-06-08T08:30:00",
      "additionalMetrics": {
        "distance": "8.2 km",
        "averageHeartRate": "152 bpm"
      }
    }
    ```
* `GET /api/activities`: Fetch all logged activities for the logged-in user.
  * Headers: `X-User-ID: <user-uuid>`
* `GET /api/activities/{activityId}`: Fetch details for a specific activity.

### 🤖 AI Service (`/api/recommendations/**`)
* `GET /api/recommendations/user/{userId}`: Fetch all generated recommendations for a user.
* `GET /api/recommendations/activity/{activityId}`: Fetch the recommendation linked to a specific activity.
  * Returns JSON structure:
    ```json
    {
      "id": "rec-mongo-uuid",
      "activityId": "activity-uuid",
      "userId": "user-uuid",
      "recommendation": "Great endurance run. You kept a stable cardiovascular output...",
      "improvements": [
        "Focus on lifting knees slightly higher to improve stride efficiency.",
        "Gradually decrease pace over final 5 minutes for a better cooldown."
      ],
      "suggestions": [
        "Include dynamic leg swings before starting the run.",
        "Incorporate a protein-rich recovery shake within 45 minutes."
      ],
      "safety": [
        "Keep hydrated - target 300ml of fluids before long cardio runs.",
        "Listen to your joints; reduce intensity if knee pain is felt."
      ],
      "createdAt": "2026-06-08T08:31:05.123"
    }
    ```
