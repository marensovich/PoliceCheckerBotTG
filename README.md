# 🚔 Police Checker Bot

A community-driven service for sharing and discovering **traffic police checkpoint locations** in real time. Users report DPS posts via a Telegram bot; others can query nearby checkpoints through the web interface or bot.

## ✨ Features

- 📍 Report traffic police checkpoint locations via Telegram bot
- 🗺️ Interactive map of active checkpoints
- 📡 Query checkpoints near your location
- 📜 Full history of reports
- 👤 User profiles and activity tracking
- 🛡️ Admin panel for moderation
- 🔆 Radar view for nearby posts

## 🛠️ Tech Stack

**Backend:**
- Java + Spring Boot
- Telegram Bot integration
- Docker containerization

**Frontend:**
- React + TypeScript + Vite
- Pages: Map, Radar, Nearby, History, Profile, Admin
- Telegram Web App integration

## 📁 Project Structure

```
backend/   # Spring Boot API + Telegram bot
frontend/  # Vite + React + TypeScript web app
```

## 🚀 Getting Started

```bash
# Backend
cd backend
./gradlew bootRun

# Frontend
cd frontend
npm install
npm run dev
```

Configure `.env` from `.env.example` before running.

## 👤 Author

[@marensovich](https://github.com/marensovich)
