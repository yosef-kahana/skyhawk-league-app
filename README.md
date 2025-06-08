# 🏆 League Statistics Application

A lightweight, Java-based REST API application for managing sport leagues, teams, players, games, statistics, and events. The app is built using Java 24 and the built-in `HttpServer` library — no external frameworks like Spring Boot.

---

## 🧩 Architecture

- **Java 24**, `com.sun.net.httpserver.HttpServer`
- **PostgreSQL** for persistent storage
- **Docker** & **Docker Compose** for local and cloud deployment
- **AWS ECS + RDS** for scalable, production-ready infrastructure

---

## 📦 Domain Overview

- A **League** contains **Teams**
- A **Team** contains **Players**
- **Games** are played between two teams and emit **Events**
- **Players** accumulate **Statistics** during events
- **Statistic Types** define custom metrics (e.g., points, assists)

---

## 🌐 REST API Endpoints + 📦 Data Models

### 🏆 League

#### `GET /league`
- List all leagues

#### `GET /league/{name}`
- Get a league by name

#### `POST /league`
```json
{
  "name": "National League",
  "description": "Top level league"
}
```

---

### 👥 Team (under a league)

#### `GET /league/{leagueName}/teams`
- List all teams in a league

#### `GET /league/{leagueName}/teams/{teamName}`
- Get a team by name

#### `POST /league/{leagueName}/teams`
```json
{
  "name": "Warriors",
  "description": "Basketball team"
}
```

#### `GET /league/{leagueName}/teams/{teamName}/season-statistics`
- Aggregated team statistics

---

### 🧍 Player

#### `GET /league/{leagueName}/teams/{teamName}/players`
- List all players in a team

#### `GET /league/{leagueName}/teams/{teamName}/players/{playerName}`
- Get player info

#### `POST /league/{leagueName}/teams/{teamName}/players`
```json
{
  "name": "John Doe",
  "number": 10,
  "description": "Forward"
}
```

#### `GET /.../players/{playerName}/season-statistics`
- Aggregated season statistics

#### `GET /.../players/{playerName}/games/{gameName}/statistics`
- Game-specific statistics

#### `GET /.../players/{playerName}/games/current/statistics`
- Current game statistics

---

### 🕹️ Game

#### `GET /league/{leagueName}/games`
- List games in a league

#### `GET /league/{leagueName}/games/{gameName}`
- Get game by name

#### `POST /league/{leagueName}/games`
```json
{
  "name": "Final Match",
  "description": "Championship",
  "homeTeamId": 1,
  "visitorTeamId": 2,
  "date": "2024-06-01"
}
```

---

### 📊 Statistic Type

#### `GET /league/{leagueName}/statistic-types`
- All statistic types

#### `GET /league/{leagueName}/statistic-types/{typeName}`
- Specific type by name

#### `POST /league/{leagueName}/statistic-types`
```json
{
  "name": "points",
  "description": "Player scored points",
  "type": "NUMBER",
  "minValue": 0,
  "maxValue": 100
}
```

---

### 📍 Event

#### `POST /events`
Submit a game or player event (e.g. GAME_START, PLAYER_ACTION, PLAYER_END)

```json
{
  "gameId": 5,
  "type": "PLAYER_ACTION",
  "eventTime": "14:22:00",
  "playerId": 17,
  "statisticTypeId": 3
}
```

---

## 🧪 Running Locally

### Prerequisites

- Java 24
- Maven 3.x
- Docker

### Build the Project

```bash
mvn clean package
```

### Run with Docker Compose

```bash
docker-compose up --build
```

Access the app at: [http://localhost:8080](http://localhost:8080)

---

## ☁️ Deploying on AWS (ECS + RDS)

1. **Push image to ECR**

```bash
docker build -t league-app .
docker tag league-app:latest <your-ecr-url>
docker push <your-ecr-url>
```

2. **Provision infrastructure**

Use CloudFormation to deploy:
- ECS Cluster
- Load Balancer
- RDS PostgreSQL

3. **Launch service**

Use ECS Fargate to run `league-app` container using the uploaded image.

---

## 🔧 Configuration (via env)

| Variable     | Description                      |
|--------------|----------------------------------|
| `DB_URL`     | JDBC connection string           |
| `DB_USER`    | DB username                      |
| `DB_PASSWORD`| DB password                      |

---

## ✅ Health Check

- `GET /health` — Returns `200 OK`

---

## 📁 Project Structure

```
src/
 └── main/
     ├── java/com/skyhawk/league/
     │    ├── ApplicationMain.java
     │    ├── controller/
     │    ├── model/
     │    └── repository/
     └── resources/
         └── application.properties
```

---

## 🧱 Build Artifacts

Output:
```
target/league-app.jar
```

---

## 📜 License

MIT License — free to use, modify, and distribute.
