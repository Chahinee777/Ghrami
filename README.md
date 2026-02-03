# 🎯 Ghrami Desktop - Social Learning Platform

<div align="center">

![Java](https://img.shields.io/badge/Java-24-orange?style=for-the-badge&logo=java)
![JavaFX](https://img.shields.io/badge/JavaFX-21.0.1-blue?style=for-the-badge&logo=java)
![MySQL](https://img.shields.io/badge/MySQL-8.3.0-blue?style=for-the-badge&logo=mysql)
![License](https://img.shields.io/badge/License-MIT-green?style=for-the-badge)

**Connect. Learn. Grow. Together.**

[Features](#-features) • [Installation](#-installation) • [Usage](#-usage) • [Documentation](#-documentation) • [Contributing](#-contributing)

</div>

---

## 📱 About Ghrami

**Ghrami** is a comprehensive social learning and hobby-matching platform that empowers users to connect with like-minded individuals, track personal progress, exchange skills, and access educational content through a marketplace of verified instructors.

### 🎯 Key Objectives

- 🤝 **Connect** people with shared interests and complementary skills
- 📊 **Track** personal progress and achieve meaningful milestones
- 🎓 **Learn** from expert instructors through bookable classes
- 🤝 **Meet** others virtually or in-person for activities and skill exchanges

---

## ✨ Features

### 🔐 Module 1: User Management (✅ Complete)
- ✅ User registration and authentication (BCrypt password hashing)
- ✅ Profile management (edit info, upload photo, change password)
- ✅ Friend system (send/accept/reject requests)
- ✅ Badge & achievement system
- ✅ Admin dashboard (CRUD operations on users, badges)
- ✅ Session management with JWT tokens

### 📱 Module 2: Social Media (⏳ In Development)
- Create and share posts with images
- Like and comment on content
- Activity feed with friends' updates
- Content filtering and discovery

### 🎨 Module 3: Hobby Management (⏳ Planned)
- Create and manage multiple hobbies
- Track progress with detailed statistics
- Set and achieve personal milestones
- Share hobby achievements

### 🤝 Module 4: Meetups (⏳ Planned)
- AI-powered matching based on interests
- Skill exchange connections
- Schedule virtual or physical meetings
- Meeting history and feedback system

### 🎓 Module 5: Classes & Booking (⏳ Planned)
- Browse classes from verified instructors
- Secure booking and payment system
- Rating and review system
- Provider registration and class management

---

## 🛠️ Technology Stack

### Backend
- **Language**: Java 24
- **Framework**: JavaFX 21.0.1
- **Database**: MySQL 8.3.0
- **Architecture**: MVC Pattern (Model-View-Controller)

### Security
- **Password Hashing**: BCrypt (jbcrypt 0.4)
- **Authentication**: JWT Tokens (io.jsonwebtoken 0.11.5)
- **Session Management**: Singleton pattern

### Build & Dependencies
- **Build Tool**: Maven
- **MySQL Connector**: 8.3.0
- **Jackson**: 2.12.6 (JSON processing)

---

## 📋 Prerequisites

Before running Ghrami Desktop, ensure you have:

- ☕ **Java Development Kit (JDK)** 24 or higher
- 🗄️ **MySQL Server** 8.0 or higher
- 🔧 **Maven** 3.6+ (or use IDE's built-in Maven)
- 💻 **IDE**: IntelliJ IDEA / Eclipse / NetBeans (recommended)

---

## 🚀 Installation

### 1. Clone the Repository

```bash
git clone https://github.com/yourusername/ghrami-desktop.git
cd ghrami-desktop
```

### 2. Database Setup

#### Create Database
```sql
CREATE DATABASE ghrami_db;
USE ghrami_db;
```

#### Run Initialization Script
```bash
mysql -u root -p ghrami_db < src/main/resources/init_db.sql
```

#### Configure Database Connection

Create or update `src/main/resources/db.properties`:

```properties
db.url=jdbc:mysql://localhost:3306/ghrami_db?useSSL=false&serverTimezone=UTC
db.username=root
db.password=your_password
```

### 3. Build the Project

```bash
mvn clean install
```

### 4. Run the Application

```bash
mvn javafx:run
```

Or run directly from your IDE:
- **Main Class**: `opgg.ghrami.GhramiApplication`

---

## 📖 Usage

### Default Admin Account

After database initialization, you can log in as admin:

```
Email: chahine@ghrami.tn
Password: admin123
```

⚠️ **Important**: Change the admin password immediately after first login!

### User Roles

| Role | Access Level | Features |
|------|-------------|----------|
| **User** | Standard member | Profile, friends, posts, hobbies, meetups, book classes |
| **Class Provider** | Instructor | User features + create/manage classes |
| **Admin** | System manager | Full system control, user management, badge assignment |

---

## 📁 Project Structure

```
Ghrami-Desktop/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── opgg/
│   │   │       └── ghrami/
│   │   │           ├── controller/         # Business logic
│   │   │           │   ├── UserController.java
│   │   │           │   ├── FriendshipController.java
│   │   │           │   └── BadgeController.java
│   │   │           ├── model/              # Data models
│   │   │           │   ├── User.java
│   │   │           │   ├── Friendship.java
│   │   │           │   └── Badge.java
│   │   │           ├── view/               # JavaFX controllers
│   │   │           │   ├── LoginViewController.java
│   │   │           │   ├── RegisterViewController.java
│   │   │           │   ├── UserFeedController.java
│   │   │           │   ├── ProfileViewController.java
│   │   │           │   └── AdminDashboardController.java
│   │   │           ├── util/               # Utilities
│   │   │           │   ├── DatabaseConnection.java
│   │   │           │   ├── PasswordUtil.java
│   │   │           │   ├── JWTUtil.java
│   │   │           │   └── SessionManager.java
│   │   │           └── GhramiApplication.java
│   │   └── resources/
│   │       ├── opgg/ghrami/view/          # FXML files
│   │       │   ├── LoginView.fxml
│   │       │   ├── RegisterView.fxml
│   │       │   ├── UserFeed.fxml
│   │       │   ├── ProfileView.fxml
│   │       │   └── AdminDashboard.fxml
│   │       ├── css/                        # Stylesheets
│   │       │   └── social-style.css
│   │       ├── images/                     # Assets
│   │       │   ├── profile_pictures/
│   │       │   └── logo.png
│   │       ├── init_db.sql                # Database schema
│   │       └── db.properties              # DB config
│   └── test/
│       └── java/                          # Unit tests
├── database_cleanup.sql                   # DB optimization script
├── pom.xml                                # Maven dependencies
└── README.md
```

---

## 🗄️ Database Schema

### Main Tables

#### `users`
- user_id (BIGINT, PK, AUTO_INCREMENT)
- username (VARCHAR, UNIQUE)
- email (VARCHAR, UNIQUE)
- password (VARCHAR) - BCrypt hashed
- full_name (VARCHAR)
- profile_picture (VARCHAR)
- bio (TEXT)
- location (VARCHAR)
- is_online (BOOLEAN)
- created_at, last_login (TIMESTAMP)

#### `friendships`
- friendship_id (BIGINT, PK, AUTO_INCREMENT)
- user1_id, user2_id (BIGINT, FK → users)
- status (ENUM: PENDING, ACCEPTED, REJECTED, BLOCKED)
- created_date, accepted_date (TIMESTAMP)

#### `badges`
- badge_id (BIGINT, PK, AUTO_INCREMENT)
- user_id (BIGINT, FK → users)
- name (VARCHAR)
- description (TEXT)
- earned_date (TIMESTAMP)

---

## 🔧 Configuration

### Database Optimization

Run the optimization script for better performance:

```bash
mysql -u root -p ghrami_db < database_cleanup.sql
```

This script:
- ✅ Creates indexes for faster queries
- ✅ Adds foreign key constraints with CASCADE
- ✅ Removes duplicate/invalid data
- ✅ Optimizes query performance

### Environment Variables (Production)

For production deployment, use environment variables instead of `db.properties`:

```bash
export DB_URL="jdbc:mysql://your-server:3306/ghrami_db"
export DB_USERNAME="your_username"
export DB_PASSWORD="your_secure_password"
```

---

## 🐛 Troubleshooting

### Common Issues

#### 1. **Database Connection Error**
```
❌ Failed to connect to database!
```
**Solution**: Check MySQL is running and credentials in `db.properties` are correct.

#### 2. **JavaFX Runtime Error**
```
Error: JavaFX runtime components are missing
```
**Solution**: Ensure JavaFX SDK is properly configured in your IDE or use Maven.

#### 3. **BCrypt Invalid Salt Error**
```
IllegalArgumentException: Invalid salt version
```
**Solution**: User password not properly hashed. Delete user and recreate with hashed password.

#### 4. **Image Upload Fails**
```
IOException: File not found
```
**Solution**: Ensure `src/main/resources/images/profile_pictures/` directory exists.

---

## 📊 Current Status

| Module | Status | Progress |
|--------|--------|----------|
| User Management | ✅ Complete | 100% |
| Social Media | ⏳ In Development | 0% |
| Hobby Management | ⏳ Planned | 0% |
| Meetups | ⏳ Planned | 0% |
| Classes & Booking | ⏳ Planned | 0% |

**Last Updated**: January 28, 2026

---

## 🎯 Roadmap

### Phase 1: MVP (✅ Complete)
- ✅ User authentication & profiles
- ✅ Friend system
- ✅ Badge system
- ✅ Admin dashboard

### Phase 2: Social Features (Q1 2026)
- 📱 Feed with posts, likes, comments
- 📸 Media sharing
- 🔔 Notifications

### Phase 3: Hobby Tracking (Q2 2026)
- 🎨 Hobby management
- 📊 Progress tracking
- 🎯 Milestone system

### Phase 4: Meetups (Q2-Q3 2026)
- 🤝 Smart matching algorithm
- 📅 Meeting scheduler
- 💬 In-app messaging

### Phase 5: Marketplace (Q3-Q4 2026)
- 🎓 Class creation & management
- 💳 Payment integration
- ⭐ Rating & review system

---

## 🤝 Contributing

We welcome contributions! Here's how you can help:

1. **Fork** the repository
2. **Create** a feature branch (`git checkout -b feature/AmazingFeature`)
3. **Commit** your changes (`git commit -m 'Add some AmazingFeature'`)
4. **Push** to the branch (`git push origin feature/AmazingFeature`)
5. **Open** a Pull Request

### Code Style Guidelines

- Follow **Java naming conventions**
- Use **meaningful variable names**
- Add **JavaDoc comments** for public methods
- Write **unit tests** for new features
- Keep **methods small and focused** (< 50 lines)

---

## 📝 Documentation

### Additional Resources

- [Class Diagram](docs/class-diagram.puml) - UML class diagram
- [Use Case Diagram](docs/usecase-diagram.puml) - User interactions
- [Product Backlog](docs/product-backlog.md) - Complete feature list
- [Database Schema](docs/database-schema.md) - Detailed DB structure
- [API Documentation](docs/api-docs.md) - Controller methods

---

## 🔒 Security

### Best Practices Implemented

- ✅ **Password Hashing**: BCrypt with salt
- ✅ **SQL Injection Prevention**: PreparedStatements
- ✅ **Session Management**: JWT tokens
- ✅ **Input Validation**: Server-side checks
- ✅ **File Upload Security**: Type & size validation

### Security Considerations

⚠️ **Before Production:**
- Move database credentials to environment variables
- Enable HTTPS/SSL
- Implement rate limiting
- Add CSRF protection
- Regular security audits

---

## 📄 License

This project is licensed under the **MIT License** - see the [LICENSE](LICENSE) file for details.

---

## 👥 Team

**Project by**: OPGG Team

- **Project Lead**: [Your Name]
- **Backend Developer**: [Name]
- **Frontend Developer**: [Name]
- **Database Administrator**: [Name]

---

## 📧 Contact & Support

- **Email**: support@ghrami.tn
- **Website**: [www.ghrami.tn](https://www.ghrami.tn)
- **GitHub Issues**: [Report a bug](https://github.com/yourusername/ghrami-desktop/issues)
- **Discord**: [Join our community](https://discord.gg/ghrami)

---

## 🙏 Acknowledgments

- **JavaFX Community** for excellent UI framework
- **BCrypt** for secure password hashing
- **MySQL** for reliable database management
- **All contributors** who helped shape Ghrami

---

<div align="center">

**Made with ❤️ by OPGG Team**

⭐ **Star us on GitHub** if you find this project useful!

[⬆ Back to Top](#-ghrami-desktop---social-learning-platform)

</div>
