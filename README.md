# 🎯 Ghrami Desktop

**Plateforme sociale d'apprentissage connectant les personnes à travers les loisirs partagés, les compétences et l'éducation.**

---

## 📱 À Propos

Ghrami est une application de bureau JavaFX moderne qui aide les utilisateurs à :
- Se connecter avec des personnes partageant leurs intérêts
- Suivre leurs progrès personnels et leurs loisirs
- Échanger des compétences via des rencontres
- Réserver des cours auprès d'instructeurs vérifiés
- Créer et partager du contenu social
- Gagner des badges pour leurs accomplissements

---

## ✨ Modules

| Module | Statut | Fonctionnalités |
|--------|--------|-----------------|
| **Gestion Utilisateurs** | ✅ Complet | Inscription, authentification, profils avec photos, amis, badges, tableau de bord admin |
| **Réseaux Sociaux** | ✅ Complet | Publications, commentaires, fil d'actualité, interactions sociales |
| **Suivi de Loisirs** | ✅ Complet | Gestion de loisirs, suivi de progrès, jalons, statistiques |
| **Mise en Correspondance** | ✅ Complet | Algorithme de matching intelligent, échange de compétences, planification de rendez-vous |
| **Cours & Réservations** | ✅ Complet | Marché de cours, tableau de bord instructeur, réservations, paiements, évaluations |

---

## 🎨 Caractéristiques UI/UX

- **Design Moderne** : Interface utilisateur cohérente avec cartes arrondies, ombres subtiles, et gradients
- **Typographie Propre** : Hiérarchie visuelle claire avec police système
- **Palette de Couleurs** :
  - Primary: `#667eea` (Violet)
  - Success: `#4CAF50` (Vert)
  - Warning: `#FF9800` (Orange)
  - Danger: `#f44336` (Rouge)
  - Background: `#f0f2f5` (Gris clair)
- **Composants Réutilisables** : Cartes, badges, boutons avec états de survol
- **Responsive** : Adaptation aux différentes tailles d'écran

---

## 🛠️ Stack Technique

- **Java 21+** + **JavaFX 21.0.1**
- **MySQL 8.3.0**
- **Maven** (outil de build)
- **BCrypt** (hachage de mots de passe)
- **Architecture MVC**
- **JavaFX CSS** (styling moderne)

---

## 🚀 Démarrage Rapide

### Prérequis
- Java JDK 21+
- MySQL 8.0+
- Maven 3.6+

### Installation

1. **Cloner le dépôt**
```bash
git clone https://github.com/yourusername/ghrami-desktop.git
cd ghrami-desktop
```

2. **Configurer la base de données**
```bash
mysql -u root -p < src/main/resources/init_db.sql
```

3. **Configuration de la connexion à la base de données**

Éditez `src/main/resources/db.properties`:
```properties
db.url=jdbc:mysql://localhost:3306/ghrami_db
db.username=root
db.password=votre_mot_de_passe
```

4. **Build et exécution**
```bash
mvn clean install
mvn javafx:run
```

### Connexion Admin par Défaut
```
Email: chahine@ghrami.tn
Mot de passe: admin123
```

---

## 📋 Structure du Projet

```
src/main/java/opgg/ghrami/
├── controller/          # Logique métier
│   ├── UserController.java
│   ├── FriendshipController.java
│   ├── BadgeController.java
│   ├── PostController.java
│   ├── CommentController.java
│   ├── HobbyController.java
│   ├── ProgressController.java
│   ├── ConnectionController.java
│   ├── MeetingController.java
│   ├── ClassController.java
│   ├── BookingController.java
│   └── ClassProviderController.java
│
├── model/              # Modèles de données
│   ├── User.java
│   ├── Friendship.java
│   ├── Badge.java
│   ├── Post.java
│   ├── Comment.java
│   ├── Hobby.java
│   ├── Progress.java
│   ├── Connection.java
│   ├── Meeting.java
│   ├── ClassEntity.java
│   └── Booking.java
│
├── view/               # Contrôleurs JavaFX
│   ├── LoginViewController.java
│   ├── RegisterViewController.java
│   ├── UserFeedController.java
│   ├── ProfileViewController.java
│   ├── FriendsViewController.java
│   ├── BadgesViewController.java
│   ├── HobbiesViewController.java
│   ├── MeetingsViewController.java
│   ├── ClassMarketplaceController.java
│   ├── InstructorDashboardController.java
│   └── AdminDashboardController.java
│
├── util/               # Utilitaires
│   ├── DatabaseConnection.java
│   ├── PasswordUtil.java
│   └── SessionManager.java
│
├── GhramiApplication.java  # Point d'entrée principal
└── Main.java

src/main/resources/
├── opgg/ghrami/view/   # Fichiers FXML
│   ├── LoginView.fxml
│   ├── RegisterView.fxml
│   ├──Schéma de Base de Données

**15 Tables à travers 5 modules :**

### Module 1 : Gestion Utilisateurs
- `users` - Informations utilisateur, profils
- `friendships` - Relations d'amitié entre utilisateurs
- `badges` - Réalisations et badges gagnés

### Module 2 : Réseaux Sociaux
- `posts` - Publications des utilisateurs
- `comments` - Commentaires sur les publications

### Module 3 : Suivi de Loisirs
- `hobbies` - Loon

1. Forkez le dépôt
2. Créez une branche de fonctionnalité (`git checkout -b feature/NouvelleFonctionnalite`)
3. Commitez vos changements (`git commit -m 'Ajout NouvelleFonctionnalite'`)
4. Pushez vers la branche (`git push origin feature/NouvelleFonctionnalite`)
5. Ouvrez une Pull Request

---


## 📄 Licence

MIT License - voir le fichier [LICENSE](LICENSE)

---

## 👥 Équipe

**Développé avec ❤️ par l'équipe OPGG**

Pour toute question ou support, contactez-nous à: support@ghrami.tn

---

## 🙏 Remerciements

- JavaFX pour le framework UI
- MySQL pour la base de données
- BCrypt pour la sécurité des mots de passe
- La communauté open-source pour l'inspiration et le support

---

**Version:** 1.0.0  
**Dernière mise à jour:** Février 2026

## 🎨 Guide de Style

### Couleurs
- **Primary**: `#667eea` - Actions principales, liens
- **Secondary**: `#764ba2` - Accents, gradients
- **Success**: `#4CAF50` - Confirmations, états positifs
- **Warning**: `#FF9800` - Avertissements, actions en attente
- **Danger**: `#f44336` - Erreurs, suppressions
- **Text Primary**: `#1c1e21` - Texte principal
- **Text Secondary**: `#65676b` - Texte secondaire
- **Background**: `#f0f2f5` - Fond de page

### Composants
- **Border Radius**: 15-25px pour cartes, 20px pour boutons
- **Shadows**: `dropshadow(gaussian, rgba(0,0,0,0.08-0.15), 10-20, 0, 0, 3-10)`
- **Padding**: 20-30px pour cartes, 10-20px pour boutons
- **Font Sizes**: 
  - Titres: 28-32px bold
  - Sous-titres: 16-18px bold
  - Texte: 13-14px
  - Petits: 11-12px

---

## 🚧 Améliorations Futures

- [ ] Notifications en temps réel
- [ ] Chat en direct entre utilisateurs
- [ ] Système de recommandation de cours
- [ ] Export de données utilisateur
- [ ] Mode sombre
- [ ] Application mobile (Android/iOS)
- [ ] Intégration de paiement en ligne
- [ ] Système de visioconférence pour cours en ligne
│   ├── style.css           # Styles globaux
│   └── social-style.css    # Styles des composants sociaux
│
├── images/
│   ├── profile_pictures/   # Photos des utilisateurs
│   ├── posts/              # Images des publications
│   └── assets/             # Ressources UI
│
├── init_db.sql         # Schéma de base de données
└── db.properties       # Configuration database
### 🎯 Loisirs
- Ajouter et gérer des loisirs
- Suivre les progrès avec jalons
- Statistiques de progression
- Visualisation des accomplissements

### 💫 Mise en Correspondance
- Découverte d'utilisateurs basée sur les intérêts
- Algorithme de matching intelligent
- Envoi de demandes de connexion
- Planification de rendez-vous
- Gestion des connexions actives

### 📚 Marché de Cours
- Parcourir les cours disponibles
- Réserver des sessions avec instructeurs
- Gestion des paiements
- Système d'évaluation
- Tableau de bord instructeur avec statistiques de revenus

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
