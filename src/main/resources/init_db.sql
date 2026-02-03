-- ============================================
-- Ghrami User Management Module - Database Schema
-- Created: 2024
-- Description: Automatically creates the database and tables
--              for User Management module with proper constraints
-- ============================================

-- Create database if not exists
CREATE DATABASE IF NOT EXISTS ghrami_db;
USE ghrami_db;

-- ============================================
-- Table: users
-- Description: Stores user account information
-- ============================================
CREATE TABLE IF NOT EXISTS users (
    user_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    full_name VARCHAR(100),
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    profile_picture VARCHAR(500),
    bio TEXT,
    location VARCHAR(100),
    is_online BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_login TIMESTAMP NULL,
    
    INDEX idx_username (username),
    INDEX idx_email (email),
    INDEX idx_is_online (is_online),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- Table: friendships
-- Description: Stores friendship relationships between users
-- ============================================
CREATE TABLE IF NOT EXISTS friendships (
    friendship_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user1_id BIGINT NOT NULL,
    user2_id BIGINT NOT NULL,
    status ENUM('PENDING', 'ACCEPTED', 'REJECTED', 'BLOCKED') DEFAULT 'PENDING',
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    accepted_date TIMESTAMP NULL,
    
    FOREIGN KEY (user1_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (user2_id) REFERENCES users(user_id) ON DELETE CASCADE,
    
    -- Ensure a user cannot send friend request to themselves
    CHECK (user1_id != user2_id),
    
    -- Ensure unique friendship (no duplicate requests between same users)
    UNIQUE KEY unique_friendship (user1_id, user2_id),
    
    INDEX idx_user1 (user1_id),
    INDEX idx_user2 (user2_id),
    INDEX idx_status (status),
    INDEX idx_created_date (created_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- Table: badges
-- Description: Stores achievement badges earned by users
-- ============================================
CREATE TABLE IF NOT EXISTS badges (
    badge_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    earned_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    
    INDEX idx_user_id (user_id),
    INDEX idx_name (name),
    INDEX idx_earned_date (earned_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- Insert Sample Data (Optional - for testing)
-- ============================================

-- Sample Users
INSERT INTO users (username, full_name, email, password, bio, location, is_online) VALUES
('amine_ben_ali', 'Amine Ben Ali', 'amine@ghrami.tn', 'password123', 'Passionné par le développement personnel et la lecture', 'Tunis, Tunisie', TRUE),
('salma_trabelsi', 'Salma Trabelsi', 'salma@ghrami.tn', 'password123', 'Entrepreneur social et mentor pour les jeunes', 'Sfax, Tunisie', FALSE),
('youssef_chaouch', 'Youssef Chaouch', 'youssef@ghrami.tn', 'password123', 'Amateur de randonnée et photographie', 'Sousse, Tunisie', TRUE),
('lina_gharbi', 'Lina Gharbi', 'lina@ghrami.tn', 'password123', 'Artiste peintre et passionnée de calligraphie arabe', 'La Marsa, Tunisie', FALSE),
('mehdi_jebali', 'Mehdi Jebali', 'mehdi@ghrami.tn', 'password123', 'Coach sportif et nutritionniste', 'Bizerte, Tunisie', TRUE);

-- Sample Friendships
INSERT INTO friendships (user1_id, user2_id, status, accepted_date) VALUES
(1, 2, 'ACCEPTED', NOW()),
(1, 3, 'ACCEPTED', NOW()),
(2, 3, 'ACCEPTED', NOW()),
(1, 4, 'PENDING', NULL),
(3, 5, 'ACCEPTED', NOW()),
(4, 5, 'PENDING', NULL);

-- Sample Badges
INSERT INTO badges (user_id, name, description) VALUES
(1, 'Pionnier Ghrami', 'Parmi les premiers utilisateurs de la plateforme'),
(1, 'Social Actif', 'A créé plus de 10 connexions'),
(2, 'Mentor Certifié', 'A aidé 5 personnes à atteindre leurs objectifs'),
(3, 'Explorateur du Sahara', 'A partagé des expériences de randonnée dans le désert'),
(3, 'Constance 30 Jours', 'A pratiqué un hobby pendant 30 jours consécutifs'),
(5, 'Coach Inspiration', 'A inspiré la communauté avec ses conseils sportifs');

-- ============================================
-- Success Message
-- ============================================
SELECT 'Database and tables created successfully!' AS Status;
SELECT COUNT(*) AS TotalUsers FROM users;
SELECT COUNT(*) AS TotalFriendships FROM friendships;
SELECT COUNT(*) AS TotalBadges FROM badges;
