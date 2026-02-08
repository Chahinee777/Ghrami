-- =====================================================
-- Ghrami Platform - Complete Database Schema
-- All 5 Modules: User Management, Social Media, Hobbies, Meetups, Classes
-- =====================================================

DROP DATABASE IF EXISTS ghrami_db;
CREATE DATABASE ghrami_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ghrami_db;

-- =====================================================
-- MODULE 1: USER MANAGEMENT
-- =====================================================

-- Users Table
CREATE TABLE users (
    user_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(100),
    profile_picture VARCHAR(500),
    bio TEXT,
    location VARCHAR(100),
    is_online BOOLEAN DEFAULT FALSE,
    
    INDEX idx_username (username),
    INDEX idx_email (email),
    INDEX idx_is_online (is_online)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Friendships Table
CREATE TABLE friendships (
    friendship_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user1_id BIGINT NOT NULL,
    user2_id BIGINT NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING',
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user1_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (user2_id) REFERENCES users(user_id) ON DELETE CASCADE,
    CHECK (user1_id != user2_id),
    UNIQUE KEY unique_friendship (user1_id, user2_id),
    
    INDEX idx_user1 (user1_id),
    INDEX idx_user2 (user2_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Badges Table
CREATE TABLE badges (
    badge_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    category VARCHAR(50),
    
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    
    INDEX idx_user_id (user_id),
    INDEX idx_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- MODULE 2: SOCIAL MEDIA
-- =====================================================

-- Posts Table
CREATE TABLE posts (
    post_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    image_url VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Comments Table
CREATE TABLE comments (
    comment_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    post_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    FOREIGN KEY (post_id) REFERENCES posts(post_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    
    INDEX idx_post_id (post_id),
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- MODULE 3: HOBBY MANAGEMENT
-- =====================================================

-- Hobbies Table
CREATE TABLE hobbies (
    hobby_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    category VARCHAR(50),
    description TEXT,
    
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    
    INDEX idx_user_id (user_id),
    INDEX idx_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Progress Table
CREATE TABLE progress (
    progress_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    hobby_id BIGINT NOT NULL,
    hours_spent DOUBLE DEFAULT 0.0,
    notes TEXT,
    
    FOREIGN KEY (hobby_id) REFERENCES hobbies(hobby_id) ON DELETE CASCADE,
    
    INDEX idx_hobby_id (hobby_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Milestones Table
CREATE TABLE milestones (
    milestone_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    hobby_id BIGINT NOT NULL,
    title VARCHAR(100) NOT NULL,
    target_date DATE,
    is_achieved BOOLEAN DEFAULT FALSE,
    
    FOREIGN KEY (hobby_id) REFERENCES hobbies(hobby_id) ON DELETE CASCADE,
    
    INDEX idx_hobby_id (hobby_id),
    INDEX idx_is_achieved (is_achieved)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- MODULE 4: MEETUPS
-- =====================================================

-- Connections Table
CREATE TABLE connections (
    connection_id VARCHAR(36) PRIMARY KEY,
    initiator_id BIGINT NOT NULL,
    receiver_id BIGINT NOT NULL,
    connection_type VARCHAR(50) NOT NULL,
    receiver_skill VARCHAR(100),
    initiator_skill VARCHAR(100),
    status VARCHAR(20) DEFAULT 'pending',
    
    FOREIGN KEY (initiator_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (receiver_id) REFERENCES users(user_id) ON DELETE CASCADE,
    
    INDEX idx_initiator_id (initiator_id),
    INDEX idx_receiver_id (receiver_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Meetings Table
CREATE TABLE meetings (
    meeting_id VARCHAR(36) PRIMARY KEY,
    connection_id VARCHAR(36) NOT NULL,
    organizer_id BIGINT NOT NULL,
    meeting_type VARCHAR(20) NOT NULL,
    location VARCHAR(255),
    scheduled_at TIMESTAMP NOT NULL,
    duration INT NOT NULL,
    status VARCHAR(20) DEFAULT 'scheduled',
    
    FOREIGN KEY (connection_id) REFERENCES connections(connection_id) ON DELETE CASCADE,
    FOREIGN KEY (organizer_id) REFERENCES users(user_id) ON DELETE CASCADE,
    
    INDEX idx_connection_id (connection_id),
    INDEX idx_organizer_id (organizer_id),
    INDEX idx_scheduled_at (scheduled_at),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Meeting Participants Table
CREATE TABLE meeting_participants (
    participant_id VARCHAR(36) PRIMARY KEY,
    meeting_id VARCHAR(36) NOT NULL,
    user_id BIGINT NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    
    FOREIGN KEY (meeting_id) REFERENCES meetings(meeting_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    
    INDEX idx_meeting_id (meeting_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- MODULE 5: CLASSES & BOOKING
-- =====================================================

-- Class Providers Table
CREATE TABLE class_providers (
    provider_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    company_name VARCHAR(100),
    expertise TEXT,
    rating DOUBLE DEFAULT 0.0,
    is_verified BOOLEAN DEFAULT FALSE,
    
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    
    INDEX idx_user_id (user_id),
    INDEX idx_is_verified (is_verified),
    INDEX idx_rating (rating)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Classes Table
CREATE TABLE classes (
    class_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    provider_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    category VARCHAR(50),
    price DOUBLE NOT NULL,
    duration INT NOT NULL,
    max_participants INT NOT NULL,
    
    FOREIGN KEY (provider_id) REFERENCES class_providers(provider_id) ON DELETE CASCADE,
    
    INDEX idx_provider_id (provider_id),
    INDEX idx_category (category),
    INDEX idx_price (price)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Bookings Table
CREATE TABLE bookings (
    booking_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    class_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    booking_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(20) DEFAULT 'scheduled',
    payment_status VARCHAR(20) DEFAULT 'pending',
    total_amount DOUBLE NOT NULL,
    
    FOREIGN KEY (class_id) REFERENCES classes(class_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    
    INDEX idx_class_id (class_id),
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_payment_status (payment_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- SAMPLE DATA FOR ALL MODULES
-- =====================================================

-- Insert Sample Users
INSERT INTO users (username, email, password, full_name, profile_picture, bio, location, is_online) VALUES
('amine_ben_ali', 'amine@ghrami.tn', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 
 'Amine Ben Ali', 'biggie.png', 'Passionate photographer and tech enthusiast', 'Tunis, Tunisia', TRUE),
('salma_trabelsi', 'salma@ghrami.tn', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 
 'Salma Trabelsi', '8_1769543694518.png', 'Fitness coach and yoga instructor', 'Sousse, Tunisia', FALSE),
('youssef_chaouch', 'youssef@ghrami.tn', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 
 'Youssef Chaouch', '8_1769595951598.png', 'Software developer and music lover', 'Sfax, Tunisia', TRUE),
('lina_gharbi', 'lina@ghrami.tn', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 
 'Lina Gharbi', '8_1769759494205.png', 'Graphic designer and artist', 'Tunis, Tunisia', TRUE),
('mehdi_jebali', 'mehdi@ghrami.tn', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 
 'Mehdi Jebali', '11_1770039241022.png', 'Marketing specialist and content creator', 'Monastir, Tunisia', FALSE);

-- Insert Sample Friendships
INSERT INTO friendships (user1_id, user2_id, status, created_date) VALUES
(1, 2, 'ACCEPTED', '2024-01-15 10:30:00'),
(1, 3, 'PENDING', '2024-01-20 14:20:00'),
(2, 3, 'ACCEPTED', '2024-01-18 09:15:00'),
(3, 4, 'ACCEPTED', '2024-01-22 16:45:00'),
(4, 5, 'PENDING', '2024-01-25 11:00:00');

-- Insert Sample Badges
INSERT INTO badges (user_id, name, description, category) VALUES
(1, 'Early Adopter', 'Joined Ghrami in its first month', 'achievement'),
(1, 'Social Butterfly', 'Connected with 10+ friends', 'social'),
(2, 'Fitness Guru', 'Completed 50 workout sessions', 'fitness'),
(3, 'Code Master', 'Shared 20+ programming tips', 'tech'),
(4, 'Creative Mind', 'Posted 30+ artistic content', 'creativity');

-- Insert Sample Posts
INSERT INTO posts (user_id, content, image_url, created_at) VALUES
(1, 'Just captured an amazing sunset at La Marsa! 🌅 #Photography', NULL, '2024-02-01 18:30:00'),
(2, 'Morning yoga session complete! Starting the day with positive energy 🧘‍♀️', NULL, '2024-02-02 07:15:00'),
(3, 'Working on a new JavaFX project. Love the framework! 💻', NULL, '2024-02-02 14:20:00'),
(4, 'New design project completed! Check out my portfolio 🎨', NULL, '2024-02-03 11:45:00'),
(5, 'Marketing tip of the day: Know your audience! 📊', NULL, '2024-02-03 16:00:00');

-- Insert Sample Comments
INSERT INTO comments (post_id, user_id, content, created_at) VALUES
(1, 2, 'Beautiful shot! What camera did you use?', '2024-02-01 19:00:00'),
(1, 3, 'La Marsa beaches are the best! 😍', '2024-02-01 19:15:00'),
(2, 1, 'Keep it up! Consistency is key 💪', '2024-02-02 07:30:00'),
(3, 4, 'JavaFX is great! Need any help with UI/UX?', '2024-02-02 15:00:00'),
(5, 3, 'Very insightful! Thanks for sharing', '2024-02-03 16:30:00');

-- Insert Sample Hobbies
INSERT INTO hobbies (user_id, name, category, description) VALUES
(1, 'Photography', 'art', 'Landscape and portrait photography'),
(2, 'Yoga', 'fitness', 'Daily yoga practice and meditation'),
(3, 'Programming', 'tech', 'Learning new frameworks and languages'),
(4, 'Digital Art', 'art', 'Creating illustrations and designs'),
(5, 'Content Creation', 'marketing', 'Social media content and copywriting');

-- Insert Sample Progress
INSERT INTO progress (hobby_id, hours_spent, notes) VALUES
(1, 150.5, 'Completed advanced lighting course'),
(2, 200.0, 'Achieved intermediate level certification'),
(3, 180.0, 'Built 5 complete projects'),
(4, 120.5, 'Mastered Adobe Creative Suite'),
(5, 95.0, 'Grew followers by 500%');

-- Insert Sample Milestones
INSERT INTO milestones (hobby_id, title, target_date, is_achieved) VALUES
(1, 'First exhibition', '2024-06-01', FALSE),
(2, 'Become certified instructor', '2024-08-01', FALSE),
(3, 'Contribute to open source', '2024-05-01', TRUE),
(4, 'Launch design agency', '2024-12-01', FALSE),
(5, 'Reach 10K followers', '2024-07-01', FALSE);

-- Insert Sample Connections
INSERT INTO connections (connection_id, initiator_id, receiver_id, connection_type, receiver_skill, initiator_skill, status) VALUES
(UUID(), 1, 2, 'skill', 'Fitness Training', 'Photography', 'accepted'),
(UUID(), 3, 4, 'activity', 'Design', 'Programming', 'pending'),
(UUID(), 2, 5, 'skill', 'Marketing', 'Yoga', 'accepted');

-- Insert Sample Meetings (using first connection)
SET @conn_id = (SELECT connection_id FROM connections LIMIT 1);
INSERT INTO meetings (meeting_id, connection_id, organizer_id, meeting_type, location, scheduled_at, duration, status) VALUES
(UUID(), @conn_id, 1, 'physical', 'Coffee Shop Tunis', '2024-02-15 14:00:00', 60, 'scheduled');

-- Insert Sample Meeting Participants
SET @meet_id = (SELECT meeting_id FROM meetings LIMIT 1);
INSERT INTO meeting_participants (participant_id, meeting_id, user_id, is_active) VALUES
(UUID(), @meet_id, 1, TRUE),
(UUID(), @meet_id, 2, TRUE);

-- Insert Sample Class Providers
INSERT INTO class_providers (user_id, company_name, expertise, rating, is_verified) VALUES
(2, 'Fitness First Tunisia', 'Yoga, Pilates, Nutrition', 4.8, TRUE),
(3, 'Code Academy TN', 'Web Development, Mobile Apps', 4.9, TRUE),
(4, 'Creative Studio', 'Graphic Design, UI/UX', 4.7, TRUE);

-- Insert Sample Classes
INSERT INTO classes (provider_id, title, description, category, price, duration, max_participants) VALUES
(1, 'Beginner Yoga Workshop', 'Introduction to yoga fundamentals', 'fitness', 50.0, 90, 15),
(1, 'Advanced Pilates', 'Intensive pilates training', 'fitness', 75.0, 60, 10),
(2, 'JavaFX Masterclass', 'Build modern desktop applications', 'tech', 150.0, 180, 20),
(2, 'React Native Bootcamp', 'Mobile app development from scratch', 'tech', 200.0, 240, 25),
(3, 'UX Design Fundamentals', 'Learn user experience design', 'design', 120.0, 120, 15);

-- Insert Sample Bookings
INSERT INTO bookings (class_id, user_id, booking_date, status, payment_status, total_amount) VALUES
(1, 1, '2024-02-01 10:00:00', 'completed', 'paid', 50.0),
(3, 2, '2024-02-02 14:00:00', 'scheduled', 'paid', 150.0),
(4, 4, '2024-02-03 09:00:00', 'scheduled', 'pending', 200.0),
(5, 5, '2024-02-04 11:00:00', 'scheduled', 'paid', 120.0);

-- =====================================================
-- DATABASE STATISTICS
-- =====================================================
SELECT 'Database created successfully!' AS Status;
SELECT COUNT(*) AS TotalUsers FROM users;
SELECT COUNT(*) AS TotalFriendships FROM friendships;
SELECT COUNT(*) AS TotalBadges FROM badges;
SELECT COUNT(*) AS TotalPosts FROM posts;
SELECT COUNT(*) AS TotalComments FROM comments;
SELECT COUNT(*) AS TotalHobbies FROM hobbies;
SELECT COUNT(*) AS TotalConnections FROM connections;
SELECT COUNT(*) AS TotalMeetings FROM meetings;
SELECT COUNT(*) AS TotalClasses FROM classes;
SELECT COUNT(*) AS TotalBookings FROM bookings;
