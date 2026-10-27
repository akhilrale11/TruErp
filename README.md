# TradeAI - Professional Real-Time Web Trading & AI Analysis Platform

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Java](https://img.shields.io/badge/Java-17-blue.svg)](https://www.oracle.com/java/)
[![Spring AI](https://img.shields.io/badge/Spring%20AI-2.0.1-orange.svg)](https://spring.io/projects/spring-ai)
[![License](https://img.shields.io/badge/License-MIT-purple.svg)](LICENSE)

An intelligent, real-time automated trading analysis and portfolio management platform built with **Spring Boot**, **Spring AI (OpenAI)**, **WebSocket**, and **Spring Data JPA**.

---

## 🌟 Key Features

- **Real-Time Market Data Stream**: WebSocket and live REST endpoints for streaming candle charts, price feeds, and order book movements.
- **AI-Powered Market Analysis**: Deep market analysis, automated trade signal generation, and sentiment reporting powered by Spring AI & OpenAI models.
- **Portfolio & Wallet Management**: Multi-instrument balance tracking, automated trade order executions, and transaction audit trails.
- **Interactive Web Interface**: Built-in responsive web dashboard with live charts, wallet control, and automated AI trade prompts.
- **In-Memory & Persistent Storage**: Fast H2 database with JPA/Hibernate transaction consistency and out-of-the-box zero-config execution.

---

## 🛠️ Technology Stack

- **Backend Framework**: Spring Boot 4.x / 3.x, Spring MVC, Spring Data JPA, Spring Security
- **AI Integration**: Spring AI (OpenAI Chat & Tool Execution)
- **Real-Time Communication**: Spring WebSocket / STOMP
- **Database**: H2 In-Memory Database (with H2 Web Console)
- **Build Tool**: Apache Maven (Maven Wrapper included)
- **Language**: Java 17+

---

## 🚀 Getting Started

### Prerequisites

- **Java 17** or later installed
- **Git**
- *(Optional)* OpenAI API Key for live AI market insights (`export OPENAI_API_KEY=your_key`)

### Quick Start

1. **Clone the repository:**
   ```bash
   git clone https://github.com/AkhilR111/TradeAI.git
   cd TradeAI
   ```

2. **Run the application:**
   - **Windows:**
     ```cmd
     .\mvnw.cmd spring-boot:run
     ```
   - **Linux / macOS:**
     ```bash
     ./mvnw spring-boot:run
     ```

3. **Access the application:**
   - Web Dashboard: `http://localhost:8080`
   - H2 Database Console: `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:tradeaidb`, User: `sa`, Password: *(empty)*)

---

## 📁 Project Architecture

```
TradeAI/
├── src/main/java/com/example/TradeAI/
│   ├── config/         # Security, WebSocket, AI, and CORS configurations
│   ├── controller/     # REST & WebSocket Endpoints (Market, Portfolio, AI Analysis)
│   ├── model/          # JPA Entities (Candle, PortfolioItem, WalletTransaction, etc.)
│   ├── repository/     # Spring Data JPA Repositories
│   ├── service/        # Business Logic, AI Trading Agent, Market Data Providers
│   └── TradeAiApplication.java
├── src/main/resources/
│   ├── application.properties
│   └── static/         # Frontend web assets (HTML, CSS, JS, Charts)
├── pom.xml
└── README.md
```

---

## 👨‍💻 Developer & Author

- **Developer**: Akhil Gitaram Rale
- **Email**: akhilrale5577@gmail.com
- **GitHub**: [@AkhilR111](https://github.com/AkhilR111)

---

## 📄 License

This project is licensed under the terms of the MIT License. See [LICENSE](LICENSE) for details.
