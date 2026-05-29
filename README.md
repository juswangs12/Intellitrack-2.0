# IntelliTrack - Capstone Submission Tracking System

[![Java Version](https://img.shields.io/badge/Java-21-blue.svg)](https://adoptopenjdk.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-green.svg)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18.2.0-blue.svg)](https://reactjs.org/)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](LICENSE)

A modern, AI-powered capstone project submission tracking system with analytics, group management, and smart reminders.

---

## *🚀 Features*

- ✨ *AI-Powered Analytics* - Gemini AI integration for smart insights
- 👥 *Group Management* - Create, edit, and delete project groups
- 📊 *Real-time Analytics* - Dashboard with charts and visualizations
- 📋 *Deliverable Tracking* - Monitor submission statuses
- 🔐 *Role-based Access* - Admin, Coordinator, Adviser, and Student roles
- 🔄 *Google OAuth2* - Easy login with Google
- 📱 *Responsive UI* - Works on all web browser
- 🛡️ *Production Ready* - Secure, scalable, and maintainable

---

##*🛠️ Tech Stack**

### *Backend*
| Component | Version |
|-----------|---------|
| Java | 21 |
| Spring Boot | 3.2.0 |
| Spring Data JPA | 3.2.0 |
| Spring Security | 3.2.0 |
| MySQL | 8.0+ |
| PostgreSQL | 14+ |
| Maven | 3.8+ |

### *Frontend*
| Component | Version |
|-----------|---------|
| React | 18.2.0 |
| React Router DOM | 6.8.0 |
| Axios | 1.16.0 |
| Lucide React | 1.14.0 |
| Node.js | 18+ |

### *AI Integration*
- Google Gemini 2.5 Flash

---

🎯 Quick Start
Prerequisites
Java 21+
Node.js 18+
MySQL 8.0+ or PostgreSQL
Maven 3.8+
Git
1. Clone the Repository
2. Backend Setup
Step 1: Configure Environment
Edit .env with your credentials:

Step 2: Create Database
Step 3: Build & Run
Backend starts on http://localhost:8080

3. Frontend Setup
Step 1: Configure Environment
Step 2: Install & Run
Frontend starts on http://localhost:3000

👤 Sample/Dummy Credentials
Role	Email	Password
Administrator	admin@intellitrack.com	IntelliTrack@2026!
Coordinator	coordinator@intellitrack.com	Coord@2026!
Adviser	adviser@intellitrack.com	Adviser@2026!
Student	student@intellitrack.com	Student@2026!

##*🐳 Docker Deployment**

### *Run with Docker Compose*
docker-compose up -d

This will start:
- MySQL database
- Backend API
- Frontend

Access:
- Frontend: http://localhost
- Backend: http://localhost:8080

---

##*📁 Project Structure**

intellitrack/
├── Backend/
│   ├── src/main/java/com/intellitrack/
│   │   ├── config/
│   │   ├── controller/
│   │   ├── dto/
│   │   ├── entity/
│   │   ├── repository/
│   │   └── service/
│   └── pom.xml
├── Frontend/
│   ├── src/
│   │   ├── components/
│   │   ├── pages/
│   │   ├── services/
│   │   └── App.js
│   └── package.json
└── README.md

---

##*🔧 Configuration**

See detailed configuration in:
- [Backend README](Backend/README.md)
- [Application Properties](Backend/src/main/resources/application.properties)

---

##*🛡️ Security**

- JWT Authentication
- Google OAuth2 Login
- Role-based Access Control
- Input Validation
- SQL Injection Protection
- XSS Protection

---

##*🤝 Contributing**

1. Fork the repository
2. Create your feature branch (git checkout -b feature/AmazingFeature)
3. Commit your changes (git commit -m 'Add some AmazingFeature')
4. Push to the branch (git push origin feature/AmazingFeature)
5. Open a Pull Request

---

##*📄 License**

Distributed under the MIT License. See LICENSE for more information.

---

##*📞 Support**

For support, email jlo07391@gmail.com or open an issue.

---

*Made with ❤️ for Capstone Projects*

---
