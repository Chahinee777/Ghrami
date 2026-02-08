# 🎯 Ghrami Desktop

**Social learning platform connecting people through shared hobbies, skills, and education.**

---

## 📱 About

Ghrami is a JavaFX desktop application that helps users:
- Connect with people who share their interests
- Track personal progress and hobbies
- Exchange skills through meetups
- Book classes from verified instructors

---

## ✨ Modules

| Module | Status | Features |
|--------|--------|----------|
| **User Management** | ✅ Complete | Registration, authentication, profiles, friends, badges, admin dashboard |
| **Social Media** | 🔨 Planned | Posts, comments, feed |
| **Hobby Tracking** | 🔨 Planned | Hobby management, progress tracking, milestones |
| **Meetups** | 🔨 Planned | Matching algorithm, skill exchange, meeting scheduler |
| **Classes & Booking** | 🔨 Planned | Class marketplace, bookings, payments, reviews |

---

## 🛠️ Tech Stack

- **Java 24** + **JavaFX 21.0.1**
- **MySQL 8.3.0**
- **Maven** (build tool)
- **BCrypt** (password hashing)
- **MVC Architecture**

---

## 🚀 Quick Start

### Prerequisites
- Java JDK 24+
- MySQL 8.0+
- Maven 3.6+

### Installation

1. **Clone repository**
```bash
git clone https://github.com/yourusername/ghrami-desktop.git
cd ghrami-desktop
```

2. **Setup database**
```bash
mysql -u root -p < src/main/resources/init_db.sql
```

3. **Configure database connection**

Edit `src/main/resources/db.properties`:
```properties
db.url=jdbc:mysql://localhost:3306/ghrami_db
db.username=root
db.password=your_password
```

4. **Build and run**
```bash
mvn clean install
mvn javafx:run
```

### Default Admin Login
```
Email: chahine@ghrami.tn
Password: admin123
```

---

## 📁 Project Structure

```
src/main/java/opgg/ghrami/
├── controller/          # Business logic (UserController, FriendshipController, etc.)
├── model/              # Data models (User, Friendship, Badge, etc.)
├── view/               # JavaFX controllers (LoginViewController, ProfileViewController, etc.)
├── util/               # Utilities (DatabaseConnection, PasswordUtil, SessionManager)
└── GhramiApplication.java

src/main/resources/
├── opgg/ghrami/view/   # FXML files
├── images/             # Profile pictures and assets
├── init_db.sql         # Database schema
└── db.properties       # Database config
```

---

## 🗄️ Database Schema

**15 Tables across 5 modules:**

- **Module 1:** `users`, `friendships`, `badges`
- **Module 2:** `posts`, `comments`
- **Module 3:** `hobbies`, `progress`, `milestones`
- **Module 4:** `connections`, `meetings`, `meeting_participants`
- **Module 5:** `class_providers`, `classes`, `bookings`

Run `add_modules_2_3_4_5.sql` to add missing modules to existing database.

---

## 🤝 Contributing

1. Fork the repository
2. Create feature branch (`git checkout -b feature/NewFeature`)
3. Commit changes (`git commit -m 'Add NewFeature'`)
4. Push to branch (`git push origin feature/NewFeature`)
5. Open a Pull Request

---

## 📄 License

MIT License - see [LICENSE](LICENSE) file

---

**Made with ❤️ by OPGG Team**
