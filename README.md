<div align="center">

# AYANAMI-LEARN

[![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Ktor](https://img.shields.io/badge/Ktor-087CFA?style=for-the-badge&logo=ktor&logoColor=white)](https://ktor.io/)
[![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)](https://developer.android.com/)
[![Firebase](https://img.shields.io/badge/Firebase-FFCA28?style=for-the-badge&logo=firebase&logoColor=black)](https://firebase.google.com/)
[![MongoDB](https://img.shields.io/badge/MongoDB-47A248?style=for-the-badge&logo=mongodb&logoColor=white)](https://www.mongodb.com/)
[![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)](https://www.docker.com/)
[![Render](https://img.shields.io/badge/Render-46E3B7?style=for-the-badge&logo=render&logoColor=black)](https://render.com/)

*A full-stack English learning platform — combining a Kotlin/Ktor backend, a native Android app, real-time WebSockets, and Firebase auth — developed as an academic group project for English class.*

[![Status](https://img.shields.io/badge/Status-In%20Development-orange?style=for-the-badge)]()
[![Release](https://img.shields.io/badge/Release-v1.0.0-6366f1?style=for-the-badge)](https://github.com/EduhxH/ayanami-learn/releases/tag/1.0.0)

🇺🇸 This project is documented and implemented entirely in English.

</div>

---

> [!WARNING]
> **This project is still under active development.**
> The app is functional, but known bugs are present and will be addressed in upcoming commits. A promotional commercial and a dedicated website are also planned and have not been released yet.

---

> [!NOTE]
> **Academic context:** AYANAMI-LEARN was built as a group project for English class. It marks a natural evolution in architecture and scope from previous projects, combining a production-grade Kotlin backend with a native Android client.

---

## Table of Contents

- [About](#-about)
- [Features](#-features)
- [Tech Stack](#️-tech-stack)
- [Prerequisites](#-prerequisites)
- [Getting Started](#-getting-started)
- [Configuration](#️-configuration)
- [Project Structure](#-project-structure)
- [Architecture Overview](#️-architecture-overview)
- [Roadmap](#-roadmap)
- [Known Limitations](#️-known-limitations)
- [What I Learned](#-what-i-learned)

---

## 🧩 About

AYANAMI-LEARN is a full-stack English learning platform built for an academic project. It consists of a **Kotlin/Ktor backend** serving a REST API with WebSocket support, and a **native Android app** that connects to it — offering an interactive and real-time learning experience.

The backend is deployed on Render via Docker and uses MongoDB as its primary database, with Firebase handling authentication. The Android client is built with Jetpack Compose, Hilt for dependency injection, and consumes the backend API and WebSocket channels directly.

The project is still in active development — the core app works end-to-end, but bug fixes, a promotional commercial, and a dedicated website are still on the way.

---

## ✨ Features

| Component / Feature            | Description                                                                                                                                 |
|--------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------|
| 📱 **Android App**              | Native Android client built with Kotlin and Jetpack Compose, consuming the backend REST API and WebSocket channels.                        |
| 🔌 **WebSocket Support**        | Real-time bidirectional communication between the Android client and the Ktor backend for live learning interactions.                       |
| 🔐 **Firebase Authentication**  | User sign-up and login handled via Firebase Auth, with the Firebase Admin SDK integrated on the backend for token verification.             |
| 🗄️ **MongoDB Storage**          | Flexible NoSQL data storage for user profiles, learning progress, and content, using the official Kotlin coroutine driver.                  |
| ⚡ **Ktor Backend**              | High-performance async server running on Netty, with content negotiation, call logging, and a full REST API.                               |
| 🐳 **Dockerised Deployment**    | The backend ships as a Docker image and is deployed on Render via `render.yaml`, making environment setup reproducible and portable.        |
| 💉 **Hilt Dependency Injection**| Android-side dependency injection with Dagger/Hilt for clean, testable, and modular client architecture.                                   |

---

## 🛠️ Tech Stack

| Technology              | Role                                              |
|-------------------------|---------------------------------------------------|
| Kotlin 1.9              | Core language for both backend and Android client |
| Ktor 2.3 (Netty)        | Backend API and WebSocket server framework        |
| Ktor Client             | HTTP + WebSocket client used in the Android app   |
| KotlinX Serialization   | JSON serialisation across server and client       |
| MongoDB (Kotlin driver) | NoSQL database with coroutine support             |
| Firebase Admin SDK      | Backend token verification and auth management    |
| Firebase Auth           | User authentication on the Android client         |
| Hilt / Dagger           | Dependency injection on Android                   |
| Docker                  | Backend containerisation                          |
| Render                  | Cloud backend deployment                          |
| Gradle (KTS)            | Build system for both backend and Android modules |

---

## 📦 Prerequisites

- JDK 17+
- Android Studio (Hedgehog or newer recommended)
- Docker (for running the backend locally in a container)
- MongoDB instance (local or Atlas)
- Firebase project with Authentication enabled
- `google-services.json` placed in the `app/` directory

---

## 🚀 Getting Started

**1. Clone the repository**

```bash
git clone https://github.com/EduhxH/ayanami-learn.git
cd ayanami-learn
```

**2. Backend setup (local)**

```bash
cp .env.example .env   # fill in your credentials
./gradlew run
```

**3. Backend setup (Docker)**

```bash
docker build -t ayanami-learn .
docker run --env-file .env -p 8080:8080 ayanami-learn
```

**4. Android app**

Open the `app/` directory in Android Studio, sync Gradle, place your `google-services.json` in `app/`, update the backend URL in your environment config, and run on a device or emulator.

**5. Run tests**

```bash
./gradlew test
```

---

## ⚙️ Configuration

The project includes a `.env.example` file. Copy and populate it before running the backend.

```env
# App
PORT=8080
ENVIRONMENT=development

# MongoDB
MONGODB_URI=mongodb+srv://user:password@cluster.mongodb.net/ayanami-learn

# Firebase
FIREBASE_PROJECT_ID=your-firebase-project-id
FIREBASE_SERVICE_ACCOUNT=path/to/serviceAccountKey.json

# JWT / Auth
SECRET_KEY=your_secret_key
```

---

## 📁 Project Structure

```
ayanami-learn/
├── app/                         # Android app module (Kotlin, Jetpack Compose, Hilt)
├── src/
│   └── main/
│       └── kotlin/
│           └── com/ayanami/     # Ktor backend source
│               ├── Application.kt
│               ├── plugins/     # Ktor plugins (routing, serialization, websockets)
│               ├── routes/      # API route definitions
│               ├── models/      # Data models
│               └── database/    # MongoDB connection and repositories
├── docs/                        # Documentation and assets
├── gradle/                      # Gradle wrapper
├── Dockerfile
├── render.yaml                  # Render deployment config
├── build.gradle.kts
├── settings.gradle.kts
├── .env.example
└── .gitignore
```

---

## 🏗️ Architecture Overview

The backend is a Ktor server running on Netty, exposing a REST API and WebSocket endpoints. Firebase Admin SDK verifies auth tokens on incoming requests before any protected route is reached. MongoDB stores all persistent data using the official Kotlin coroutine driver for non-blocking I/O throughout.

The Android client uses Ktor Client to consume the REST API and connect to WebSocket channels, with Hilt managing all dependencies across the app. Firebase Auth on the Android side handles user sign-in and supplies the token sent to the backend on every authenticated request.

```
Android App (Kotlin + Jetpack Compose)
      │
      ├── REST requests (Ktor Client)
      └── WebSocket connection (Ktor Client WS)
                    │
                    ▼
        Ktor Backend (Netty)
              │
              ├── Firebase Admin SDK  (token verification)
              ├── MongoDB             (persistent storage)
              └── WebSocket channels  (real-time interactions)
                    │
                    ▼
              Render (Docker)
```

---

## 🗺️ Roadmap

- [x] Ktor backend with REST API and WebSocket support
- [x] Firebase authentication (Admin SDK + Android client)
- [x] MongoDB integration with Kotlin coroutine driver
- [x] Android app with Jetpack Compose and Hilt
- [x] Docker containerisation and Render deployment
- [ ] Bug fixes across the Android client (ongoing)
- [ ] Promotional commercial
- [ ] Dedicated marketing website

---

## ⚠️ Known Limitations

- **Active development** — the app is functional end-to-end, but known bugs exist and are being tracked; future commits will address them.
- **No commercial or website yet** — both are planned deliverables still in progress.
- **Free-tier cold starts** — the backend is deployed on Render's free tier; the first request after a period of inactivity may take several seconds.
- **Single-developer effort** — despite being a group project, the implementation was carried out by a single team member; architectural decisions reflect a solo development pace rather than a split workload.
- **No offline mode** — the Android app requires an active connection to the backend; offline support has not been implemented.

---

## 🧠 What I Learned

- **Kotlin backend with Ktor** — building an async REST API and WebSocket server with Netty, structured routing, and plugin-based architecture in idiomatic Kotlin.
- **Firebase Admin SDK on the server** — verifying Firebase ID tokens server-side to protect routes, rather than relying solely on client-side auth.
- **MongoDB coroutine driver** — using the official Kotlin coroutine driver for fully non-blocking database operations throughout the backend.
- **WebSockets in Ktor** — setting up persistent bidirectional channels between the Kotlin backend and the Android client using Ktor's WebSocket plugin on both sides.
- **Hilt dependency injection on Android** — structuring an Android app with clean DI from the ground up, making components testable and lifecycle-aware.
- **Dockerised deployment to Render** — writing a production `Dockerfile` and `render.yaml` for fully reproducible backend deployments via a single push.
- **Gradle KTS for multi-module projects** — managing a project that spans a Ktor backend and an Android module from the same Gradle root build.

---

## 🤝 Contributing

Contributions are welcome. If you find a bug or want to propose a feature, open an issue first so we can discuss it before any code is written. When submitting a pull request, keep the scope focused — one fix or feature per PR.

---

## 📄 License

This project is licensed under the MIT License. See the [LICENSE](./LICENSE) file for details.

---

<div align="center">
  Made with 💜 by <a href="https://github.com/EduhxH">EduhxH</a>
</div>
