-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1
-- Generation Time: Feb 15, 2026 at 04:10 PM
-- Server version: 10.4.32-MariaDB
-- PHP Version: 8.2.12

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `ghrami_db`
--

-- --------------------------------------------------------

--
-- Table structure for table `badges`
--

CREATE TABLE `badges` (
  `badge_id` bigint(20) NOT NULL,
  `user_id` bigint(20) NOT NULL,
  `name` varchar(100) NOT NULL,
  `description` text DEFAULT NULL,
  `earned_date` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `badges`
--

INSERT INTO `badges` (`badge_id`, `user_id`, `name`, `description`, `earned_date`) VALUES
(1, 1, 'Pionnier Ghrami', 'Parmi les premiers utilisateurs de la plateforme', '2026-01-26 18:21:03'),
(2, 1, 'Social Actif', 'A créé plus de 10 connexion', '2026-01-26 18:21:03'),
(3, 2, 'Mentor Certifié', 'A aidé 5 personnes à atteindre leurs objectifs', '2026-01-26 18:21:03'),
(4, 3, 'Explorateur du Sahara', 'A partagé des expériences de randonnée dans le désert', '2026-01-26 18:21:03'),
(5, 3, 'Constance 30 Jours', 'A pratiqué un hobby pendant 30 jours consécutifs', '2026-01-26 18:21:03'),
(6, 5, 'Coach Inspiration', 'A inspiré la communauté avec ses conseils sportifs', '2026-01-26 18:21:03'),
(7, 1, 'Pro Footbaleur', 'Milieu de terrain offensif', '2026-02-03 18:14:35'),
(8, 8, '💎 Diamond Member', '1 year anniversary', '2026-02-10 08:04:51'),
(9, 12, '🥇 First Friend', 'Made their first friend on Ghrami', '2026-02-10 08:16:42'),
(10, 8, 'Booster', 'Boost ces amis', '2026-02-14 09:46:00'),
(13, 46, 'el fasa3 el mo2ases', 'yafsa3 barcha wdima re9d', '2026-02-15 13:41:19');

-- --------------------------------------------------------

--
-- Table structure for table `bookings`
--

CREATE TABLE `bookings` (
  `booking_id` bigint(20) NOT NULL,
  `class_id` bigint(20) NOT NULL,
  `user_id` bigint(20) NOT NULL,
  `booking_date` timestamp NOT NULL DEFAULT current_timestamp(),
  `status` varchar(20) DEFAULT 'scheduled',
  `payment_status` varchar(20) DEFAULT 'pending',
  `total_amount` double NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `bookings`
--

INSERT INTO `bookings` (`booking_id`, `class_id`, `user_id`, `booking_date`, `status`, `payment_status`, `total_amount`) VALUES
(1, 1, 1, '2024-02-01 09:00:00', 'completed', 'paid', 50),
(2, 3, 2, '2024-02-02 13:00:00', 'scheduled', 'paid', 150),
(3, 4, 4, '2024-02-03 08:00:00', 'scheduled', 'pending', 200),
(4, 5, 5, '2024-02-04 10:00:00', 'scheduled', 'paid', 120),
(5, 1, 8, '2026-02-11 19:59:04', 'scheduled', 'pending', 50),
(6, 6, 11, '2026-02-11 19:59:41', 'completed', 'pending', 100),
(7, 6, 15, '2026-02-12 12:42:28', 'cancelled', 'pending', 100),
(8, 6, 15, '2026-02-12 13:01:53', 'cancelled', 'pending', 100),
(17, 5, 8, '2026-02-15 12:22:47', 'cancelled', 'pending', 120),
(18, 9, 10, '2026-02-15 12:27:35', 'scheduled', 'pending', 200),
(19, 9, 46, '2026-02-15 13:44:04', 'scheduled', 'pending', 200),
(20, 11, 8, '2026-02-15 13:48:18', 'scheduled', 'pending', 50);

-- --------------------------------------------------------

--
-- Table structure for table `classes`
--

CREATE TABLE `classes` (
  `class_id` bigint(20) NOT NULL,
  `provider_id` bigint(20) NOT NULL,
  `title` varchar(200) NOT NULL,
  `description` text DEFAULT NULL,
  `category` varchar(50) DEFAULT NULL,
  `price` double NOT NULL,
  `duration` int(11) NOT NULL,
  `max_participants` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `classes`
--

INSERT INTO `classes` (`class_id`, `provider_id`, `title`, `description`, `category`, `price`, `duration`, `max_participants`) VALUES
(1, 1, 'Beginner Yoga Workshop', 'Introduction to yoga fundamentals', 'fitness', 50, 90, 15),
(2, 1, 'Advanced Pilates', 'Intensive pilates training', 'fitness', 75, 60, 10),
(3, 2, 'JavaFX Masterclass', 'Build modern desktop applications', 'tech', 150, 180, 20),
(4, 2, 'React Native Bootcamp', 'Mobile app development from scratch', 'tech', 200, 240, 25),
(5, 3, 'UX Design Fundamentals', 'Learn user experience design', 'design', 120, 120, 15),
(6, 4, 'DevOps', 'CI/CD', 'Cloud', 100, 20, 20),
(9, 4, 'Trading', 'Crypto Currencies', 'finance', 200, 200, 10),
(11, 11, 'TLA', 'Matiere esprit', 'education', 50, 100, 30);

-- --------------------------------------------------------

--
-- Table structure for table `class_providers`
--

CREATE TABLE `class_providers` (
  `provider_id` bigint(20) NOT NULL,
  `user_id` bigint(20) NOT NULL,
  `company_name` varchar(100) DEFAULT NULL,
  `expertise` text DEFAULT NULL,
  `rating` double DEFAULT 0,
  `is_verified` tinyint(1) DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `class_providers`
--

INSERT INTO `class_providers` (`provider_id`, `user_id`, `company_name`, `expertise`, `rating`, `is_verified`) VALUES
(1, 2, 'Fitness First Tunisia', 'Yoga, Pilates, Nutrition', 4.8, 1),
(2, 3, 'Code Academy TN', 'Web Development, Mobile Apps', 4.9, 1),
(3, 4, 'Creative Studio', 'Graphic Design, UI/UX', 4.7, 1),
(4, 8, 'Esprit', 'Coding', 0, 1),
(5, 15, 'Esprit', 'Un grand Footballeur', 0, 1),
(10, 10, 'Esprit', 'el fas3a', 0, 1),
(11, 46, 'Esprit', 'El fas3a', 0, 1);

-- --------------------------------------------------------

--
-- Table structure for table `comments`
--

CREATE TABLE `comments` (
  `comment_id` bigint(20) NOT NULL,
  `post_id` bigint(20) NOT NULL,
  `user_id` bigint(20) NOT NULL,
  `content` text NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `comments`
--

INSERT INTO `comments` (`comment_id`, `post_id`, `user_id`, `content`, `created_at`) VALUES
(1, 1, 2, 'Beautiful shot! What camera did you use?', '2024-02-01 18:00:00'),
(2, 1, 3, 'La Marsa beaches are the best! 😍', '2024-02-01 18:15:00'),
(3, 2, 1, 'Keep it up! Consistency is key 💪', '2024-02-02 06:30:00'),
(4, 3, 4, 'JavaFX is great! Need any help with UI/UX?', '2024-02-02 14:00:00'),
(5, 5, 3, 'Very insightful! Thanks for sharing', '2024-02-03 15:30:00'),
(6, 7, 8, 'Grand Equipe', '2026-02-13 15:38:21'),
(7, 6, 10, 'Bravo anas', '2026-02-13 15:42:27'),
(8, 9, 8, 'Merci OPGG', '2026-02-14 10:21:37'),
(22, 23, 11, 'klem ma39oul wmwzoun', '2026-02-15 13:30:07');

-- --------------------------------------------------------

--
-- Table structure for table `connections`
--

CREATE TABLE `connections` (
  `connection_id` varchar(36) NOT NULL,
  `initiator_id` bigint(20) NOT NULL,
  `receiver_id` bigint(20) NOT NULL,
  `connection_type` varchar(50) NOT NULL,
  `receiver_skill` varchar(100) DEFAULT NULL,
  `initiator_skill` varchar(100) DEFAULT NULL,
  `status` varchar(20) DEFAULT 'pending'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `connections`
--

INSERT INTO `connections` (`connection_id`, `initiator_id`, `receiver_id`, `connection_type`, `receiver_skill`, `initiator_skill`, `status`) VALUES
('186e3bea-3590-4a1b-a86e-90780f9d86be', 15, 8, 'skill', 'Baking', 'Art', 'accepted'),
('6705b3f5-cecb-446f-a2a3-90fa086d7033', 8, 11, 'hobby', 'arts', 'arts', 'rejected'),
('743831a1-fcac-472d-9581-9a289dd10101', 8, 10, 'general', 'cooking', 'football', 'rejected'),
('86db099b-6941-46e5-80a3-4878ae40a81f', 46, 11, 'hobby', 'music', 'music', 'accepted'),
('d0334e28-0510-11f1-8e75-047c163dbfbf', 1, 2, 'skill', 'Fitness Training', 'Photography', 'accepted'),
('d033a8ca-0510-11f1-8e75-047c163dbfbf', 3, 4, 'activity', 'Design', 'Programming', 'pending'),
('d033a9ee-0510-11f1-8e75-047c163dbfbf', 2, 5, 'skill', 'Marketing', 'Yoga', 'accepted');

-- --------------------------------------------------------

--
-- Table structure for table `friendships`
--

CREATE TABLE `friendships` (
  `friendship_id` bigint(20) NOT NULL,
  `user1_id` bigint(20) NOT NULL,
  `user2_id` bigint(20) NOT NULL,
  `status` enum('PENDING','ACCEPTED','REJECTED','BLOCKED') DEFAULT 'PENDING',
  `created_date` timestamp NOT NULL DEFAULT current_timestamp(),
  `accepted_date` timestamp NULL DEFAULT NULL
) ;

--
-- Dumping data for table `friendships`
--

INSERT INTO `friendships` (`friendship_id`, `user1_id`, `user2_id`, `status`, `created_date`, `accepted_date`) VALUES
(1, 1, 2, 'ACCEPTED', '2026-01-26 18:21:03', '2026-01-26 18:21:03'),
(2, 1, 3, 'ACCEPTED', '2026-01-26 18:21:03', '2026-01-26 18:21:03'),
(3, 2, 3, 'ACCEPTED', '2026-01-26 18:21:03', '2026-01-26 18:21:03'),
(4, 1, 4, 'PENDING', '2026-01-26 18:21:03', NULL),
(5, 3, 5, 'ACCEPTED', '2026-01-26 18:21:03', '2026-01-26 18:21:03'),
(6, 4, 5, 'PENDING', '2026-01-26 18:21:03', NULL),
(7, 8, 11, 'PENDING', '2026-02-08 09:48:27', NULL),
(8, 10, 8, 'ACCEPTED', '2026-02-08 09:50:08', '2026-02-08 09:50:42'),
(9, 12, 8, 'ACCEPTED', '2026-02-10 08:15:17', '2026-02-10 08:15:48'),
(10, 15, 8, 'ACCEPTED', '2026-02-12 12:41:30', '2026-02-12 12:45:15'),
(14, 46, 11, 'ACCEPTED', '2026-02-15 13:29:14', '2026-02-15 13:29:41');

-- --------------------------------------------------------

--
-- Table structure for table `hobbies`
--

CREATE TABLE `hobbies` (
  `hobby_id` bigint(20) NOT NULL,
  `user_id` bigint(20) NOT NULL,
  `name` varchar(100) NOT NULL,
  `category` varchar(50) DEFAULT NULL,
  `description` text DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `hobbies`
--

INSERT INTO `hobbies` (`hobby_id`, `user_id`, `name`, `category`, `description`) VALUES
(1, 1, 'Photography', 'art', 'Landscape and portrait photography'),
(2, 2, 'Yoga', 'fitness', 'Daily yoga practice and meditation'),
(3, 3, 'Programming', 'tech', 'Learning new frameworks and languages'),
(4, 4, 'Digital Art', 'art', 'Creating illustrations and designs'),
(5, 5, 'Content Creation', 'marketing', 'Social media content and copywriting'),
(6, 8, 'Coding', 'Technology', 'Developper applications web'),
(7, 8, 'Football', 'Sports & Fitness', 'Real Madrid'),
(9, 11, 'Football', 'Sports & Fitness', 'Barca'),
(10, 8, 'Baking', 'Cooking', 'Cookies'),
(14, 46, 'Piano', 'Music', 'Piano Tiles'),
(15, 46, 'Guitar', 'Music', 'Shawn Mendes');

-- --------------------------------------------------------

--
-- Table structure for table `meetings`
--

CREATE TABLE `meetings` (
  `meeting_id` varchar(36) NOT NULL,
  `connection_id` varchar(36) NOT NULL,
  `organizer_id` bigint(20) NOT NULL,
  `meeting_type` varchar(20) NOT NULL,
  `location` varchar(255) DEFAULT NULL,
  `scheduled_at` timestamp NOT NULL DEFAULT current_timestamp() ON UPDATE current_timestamp(),
  `duration` int(11) NOT NULL,
  `status` varchar(20) DEFAULT 'scheduled'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `meetings`
--

INSERT INTO `meetings` (`meeting_id`, `connection_id`, `organizer_id`, `meeting_type`, `location`, `scheduled_at`, `duration`, `status`) VALUES
('006c2d92-2d7a-4112-b60e-cd8a8afff914', '186e3bea-3590-4a1b-a86e-90780f9d86be', 8, 'physical', 'fahs', '2026-02-28 12:00:00', 60, 'scheduled'),
('11e5617e-1192-44f6-ae7d-c7347166286f', '186e3bea-3590-4a1b-a86e-90780f9d86be', 8, 'physical', 'sidi bousaid', '2026-02-12 15:03:58', 60, 'cancelled'),
('1fc0d686-0dc3-4278-b143-83bc396ac910', '86db099b-6941-46e5-80a3-4878ae40a81f', 11, 'physical', 'neb3do aala sidi hsine', '2026-02-21 12:00:00', 60, 'scheduled'),
('2612f31d-590a-4a44-b7dd-3cb020990e2e', '186e3bea-3590-4a1b-a86e-90780f9d86be', 8, 'physical', 'centre ville', '2026-02-14 12:27:32', 60, 'completed'),
('9ace2db6-4bc2-43a4-b22d-7b208ac40bfb', '186e3bea-3590-4a1b-a86e-90780f9d86be', 8, 'physical', '', '2026-02-12 14:56:00', 60, 'completed'),
('c8c1c3e1-3597-45ef-a294-104adf2cb5c5', '186e3bea-3590-4a1b-a86e-90780f9d86be', 8, 'physical', '', '2026-02-12 14:55:39', 60, 'cancelled'),
('d04020cb-0510-11f1-8e75-047c163dbfbf', 'd0334e28-0510-11f1-8e75-047c163dbfbf', 1, 'physical', 'Coffee Shop Tunis', '2024-02-15 13:00:00', 60, 'scheduled');

-- --------------------------------------------------------

--
-- Table structure for table `meeting_participants`
--

CREATE TABLE `meeting_participants` (
  `participant_id` varchar(36) NOT NULL,
  `meeting_id` varchar(36) NOT NULL,
  `user_id` bigint(20) NOT NULL,
  `is_active` tinyint(1) DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `meeting_participants`
--

INSERT INTO `meeting_participants` (`participant_id`, `meeting_id`, `user_id`, `is_active`) VALUES
('15cd9338-16ec-4dde-94f7-7d18e7371941', '11e5617e-1192-44f6-ae7d-c7347166286f', 8, 1),
('1d0c4b4f-9558-453e-9fa0-ed12a363869e', '9ace2db6-4bc2-43a4-b22d-7b208ac40bfb', 8, 1),
('20116c9e-28f0-4067-bd6a-d4461f4ec922', '2612f31d-590a-4a44-b7dd-3cb020990e2e', 8, 1),
('41c9ef8f-63e3-403c-b393-96553efe5cc9', '006c2d92-2d7a-4112-b60e-cd8a8afff914', 15, 1),
('75e1326b-cd1e-4100-b0b4-feb92691a23c', '1fc0d686-0dc3-4278-b143-83bc396ac910', 11, 1),
('77c51604-4f13-4156-8c28-519e86cf5931', '11e5617e-1192-44f6-ae7d-c7347166286f', 15, 1),
('8b741892-32d9-4e71-ac20-2dcdb5d5a016', 'c8c1c3e1-3597-45ef-a294-104adf2cb5c5', 8, 1),
('977a7735-2a37-483e-9510-532c5d78c630', '006c2d92-2d7a-4112-b60e-cd8a8afff914', 8, 1),
('d0445ff7-0510-11f1-8e75-047c163dbfbf', 'd04020cb-0510-11f1-8e75-047c163dbfbf', 1, 1),
('d044bc36-0510-11f1-8e75-047c163dbfbf', 'd04020cb-0510-11f1-8e75-047c163dbfbf', 2, 1),
('d0d0fa27-11d4-409e-ad77-ba9787e171f5', 'c8c1c3e1-3597-45ef-a294-104adf2cb5c5', 15, 1),
('d32cebbc-e030-43ef-9937-240292f33643', '2612f31d-590a-4a44-b7dd-3cb020990e2e', 15, 1),
('e4416a44-0ef7-46a1-b8d4-f64f92825924', '9ace2db6-4bc2-43a4-b22d-7b208ac40bfb', 15, 1),
('f9410b8f-1eb3-4f4a-b9b2-e7db76d59563', '1fc0d686-0dc3-4278-b143-83bc396ac910', 46, 1);

-- --------------------------------------------------------

--
-- Table structure for table `milestones`
--

CREATE TABLE `milestones` (
  `milestone_id` bigint(20) NOT NULL,
  `hobby_id` bigint(20) NOT NULL,
  `title` varchar(100) NOT NULL,
  `target_date` date DEFAULT NULL,
  `is_achieved` tinyint(1) DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `milestones`
--

INSERT INTO `milestones` (`milestone_id`, `hobby_id`, `title`, `target_date`, `is_achieved`) VALUES
(1, 1, 'First exhibition', '2024-06-01', 0),
(2, 2, 'Become certified instructor', '2024-08-01', 0),
(3, 3, 'Contribute to open source', '2024-05-01', 1),
(4, 4, 'Launch design agency', '2024-12-01', 0),
(5, 5, 'Reach 10K followers', '2024-07-01', 0),
(6, 9, 'Sa7e7t maa ljuve', '2026-02-26', 0),
(7, 6, 'Engeneer', '2026-02-10', 0),
(8, 10, 'Chef De Cuisine', '2026-02-28', 0),
(9, 10, 'Milk and cookies', '2026-02-08', 0),
(11, 14, 'Presenter mes skill infront ma famille', '2026-02-28', 0),
(12, 14, 'Test', '2026-02-14', 0);

-- --------------------------------------------------------

--
-- Table structure for table `posts`
--

CREATE TABLE `posts` (
  `post_id` bigint(20) NOT NULL,
  `user_id` bigint(20) NOT NULL,
  `content` text NOT NULL,
  `image_url` varchar(500) DEFAULT NULL,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `posts`
--

INSERT INTO `posts` (`post_id`, `user_id`, `content`, `image_url`, `created_at`) VALUES
(1, 1, 'Just captured an amazing sunset at La Marsa! 🌅 #Photography', NULL, '2024-02-01 17:30:00'),
(2, 2, 'Morning yoga session complete! Starting the day with positive energy 🧘‍♀️', NULL, '2024-02-02 06:15:00'),
(3, 3, 'Working on a new JavaFX project. Love the framework! 💻', NULL, '2024-02-02 13:20:00'),
(4, 4, 'New design project completed! Check out my portfolio 🎨', NULL, '2024-02-03 10:45:00'),
(5, 5, 'Marketing tip of the day: Know your audience! 📊', NULL, '2024-02-03 15:00:00'),
(6, 8, 'J\'aime le Football', NULL, '2026-02-13 15:29:15'),
(7, 10, 'Hala Madrid..Y nada mas', '10_1771000602829_8ece0.jpg', '2026-02-13 15:36:49'),
(9, 8, 'je suis heureux car je utilise Ghrami', '8_1771066256693_487744895_9596645127062608_1218059749256759297_n.jpg', '2026-02-14 09:50:58'),
(23, 46, 'ahsen haja heya lfas3a', NULL, '2026-02-15 13:28:52'),
(24, 11, 'astro burger for the win', '11_1771165890134_unnamed.jpg', '2026-02-15 13:31:32');

-- --------------------------------------------------------

--
-- Table structure for table `progress`
--

CREATE TABLE `progress` (
  `progress_id` bigint(20) NOT NULL,
  `hobby_id` bigint(20) NOT NULL,
  `hours_spent` double DEFAULT 0,
  `notes` text DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `progress`
--

INSERT INTO `progress` (`progress_id`, `hobby_id`, `hours_spent`, `notes`) VALUES
(1, 1, 150.5, 'Completed advanced lighting course'),
(2, 2, 200, 'Achieved intermediate level certification'),
(3, 3, 180, 'Built 5 complete projects'),
(4, 4, 120.5, 'Mastered Adobe Creative Suite'),
(5, 5, 95, 'Grew followers by 500%'),
(6, 6, 35, 'Lyoum t3alemt devops'),
(7, 7, 0, 'Started tracking'),
(9, 9, 2, 't3alemt el dribble'),
(10, 10, 1, 'Chocolate Ships'),
(12, 14, 2, '15 fev : first steps'),
(13, 15, 2, '');

-- --------------------------------------------------------

--
-- Table structure for table `users`
--

CREATE TABLE `users` (
  `user_id` bigint(20) NOT NULL,
  `username` varchar(50) NOT NULL,
  `full_name` varchar(100) DEFAULT NULL,
  `email` varchar(100) NOT NULL,
  `password` varchar(255) NOT NULL,
  `profile_picture` varchar(500) DEFAULT NULL,
  `bio` text DEFAULT NULL,
  `location` varchar(100) DEFAULT NULL,
  `is_online` tinyint(1) DEFAULT 0,
  `created_at` timestamp NOT NULL DEFAULT current_timestamp(),
  `last_login` timestamp NULL DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

--
-- Dumping data for table `users`
--

INSERT INTO `users` (`user_id`, `username`, `full_name`, `email`, `password`, `profile_picture`, `bio`, `location`, `is_online`, `created_at`, `last_login`) VALUES
(0, 'chahine', 'Chahine Admin', 'chahine@ghrami.tn', '$2a$12$8DyT3LJQewW0m6EU94dEKeQYdZAafj2rboJtmAtRFpBFWrIC2u6K2', '', 'Administrateur système', 'Tunis', 0, '2026-01-26 17:32:35', NULL),
(1, 'amine_ben_ali', 'Amine Ben Ali', 'amine@ghrami.tn', 'password123', NULL, 'Passionné par le développement personnel et la lecture', 'Tunis, Tunisie', 1, '2026-01-26 18:21:03', NULL),
(2, 'salma_trabelsi', 'Salma Trabelsi', 'salma@ghrami.tn', 'password123', NULL, 'Entrepreneur social et mentor pour les jeunes', 'Sfax, Tunisie', 0, '2026-01-26 18:21:03', NULL),
(3, 'youssef_chaouch', 'Youssef Chaouch', 'youssef@ghrami.tn', 'password123', NULL, 'Amateur de randonnée et photographie', 'Sousse, Tunisie', 1, '2026-01-26 18:21:03', NULL),
(4, 'lina_gharbi', 'Lina Gharbi', 'lina@ghrami.tn', 'password123', NULL, 'Artiste peintre et passionnée de calligraphie arabe', 'La Marsa, Tunisie', 0, '2026-01-26 18:21:03', NULL),
(5, 'mehdi_jebali', 'Mehdi Jebali', 'mehdi@ghrami.tn', 'password123', NULL, 'Coach sportif et nutritionniste', 'Bizerte, Tunisie', 1, '2026-01-26 18:21:03', NULL),
(8, 'anasBiggie', 'ANAS BIGGIE', 'anas@ghrami.tn', '$2a$12$ThDEhWRKuC4sxga6jPzJX.YQPBULUymrkMBvyiKMMK.WAsccpIZR2', '8_1770192277278.png', 'Waa', 'hay zouhour', 0, '2026-01-26 18:06:50', NULL),
(10, 'roua', 'roue hammemi', 'roua@ghrami.tn', '$2a$12$ry5aPTSulQU5.TOuP7n5kOjD/VoFcAfJj2m82rUHA9SBv9NSnLa.a', '', 'aaaaa', 'sidi hsine', 0, '2026-01-28 17:18:14', NULL),
(11, 'nourhen2004', 'Nourhen Dheker', 'nourhen@ghrami.tn', '$2a$12$09T6NioWegcjGIHv5gdemOQ1ixQQi11sXjwk4DjuoCu51JPg4K3zW', '11_1770039241022.png', 'violonist', 'tunis', 1, '2026-02-02 12:32:06', NULL),
(12, 'hamza_laz3er', 'Hamza Mnajja', 'hamza@ghrami.tn', '$2a$12$7CM4rE4eWmEG0OcJ6CJZoOjLpILNeaAseKkmiLqz2.OXBfy2PhwoO', '12_1770714864510.png', 'Nheb el mekla', 'Yssminet', 0, '2026-02-10 08:12:55', NULL),
(13, 'chahineGamerBoi', 'Chahine Aoa', 'acgamer35ca@gmail.com', '$2a$12$5jb44qpmHSvtVl3tDsZ5EOxYm7LgYRJVNYFMyOBM0SBTcs5XBlMzy', '', 'Gamer Boy', 'Medina', 0, '2026-02-11 15:59:53', NULL),
(14, 'astroNourhen', 'Nourhen Dhaker', 'nourhendhaker25@gmail.com', '$2a$12$7mU2tZhIEpNoWusPWg1sIOhrv/NYRSA8SJ16.4m/LyH2umuLDsDyW', '', 'Astro Burger', 'Ariana', 0, '2026-02-11 18:19:31', NULL),
(15, 'aymen_bavari', 'Aymen Le fils de Aziza', 'aymen.benaziza@icloud.com', '$2a$12$eAGbUjEQiycAxh0dhbuL2ubibczH9bnbMj/ZDJvkfyxqCMgSy9sRS', '15_1770903664598.jpeg', 'Nheb el denya wel mdina', 'Mdina Aarbi', 0, '2026-02-12 12:38:38', NULL),
(45, 'anasEsprit', 'Med Khelifi', 'dgxbigi@gmail.com', '$2a$12$/1xwRjyatb/thADazUxqNOBrBZKFjfHa86H7l563GYiTCrLUentIu', '', 'naturelle', 'Hay zouhour', 0, '2026-02-15 12:44:17', NULL),
(46, 'ptit_fille', 'la petite fille', 'roue.hamemi@esprit.tn', '$2a$12$JES25U3iho1dhBEiHaGTt.Lz6SVRMYpu3UgIHC8mqr2HYGQ15rwry', '', 'Jaime le fas3a et le 9oumen ma5er', 'Sidi hsine', 0, '2026-02-15 13:18:34', NULL);

--
-- Indexes for dumped tables
--

--
-- Indexes for table `badges`
--
ALTER TABLE `badges`
  ADD PRIMARY KEY (`badge_id`),
  ADD KEY `idx_user_id` (`user_id`),
  ADD KEY `idx_name` (`name`),
  ADD KEY `idx_earned_date` (`earned_date`),
  ADD KEY `idx_badges_user_id` (`user_id`);

--
-- Indexes for table `bookings`
--
ALTER TABLE `bookings`
  ADD PRIMARY KEY (`booking_id`),
  ADD KEY `idx_class_id` (`class_id`),
  ADD KEY `idx_user_id` (`user_id`),
  ADD KEY `idx_status` (`status`),
  ADD KEY `idx_payment_status` (`payment_status`);

--
-- Indexes for table `classes`
--
ALTER TABLE `classes`
  ADD PRIMARY KEY (`class_id`),
  ADD KEY `idx_provider_id` (`provider_id`),
  ADD KEY `idx_category` (`category`),
  ADD KEY `idx_price` (`price`);

--
-- Indexes for table `class_providers`
--
ALTER TABLE `class_providers`
  ADD PRIMARY KEY (`provider_id`),
  ADD UNIQUE KEY `user_id` (`user_id`),
  ADD KEY `idx_user_id` (`user_id`),
  ADD KEY `idx_is_verified` (`is_verified`),
  ADD KEY `idx_rating` (`rating`);

--
-- Indexes for table `comments`
--
ALTER TABLE `comments`
  ADD PRIMARY KEY (`comment_id`),
  ADD KEY `idx_post_id` (`post_id`),
  ADD KEY `idx_user_id` (`user_id`),
  ADD KEY `idx_created_at` (`created_at`);

--
-- Indexes for table `connections`
--
ALTER TABLE `connections`
  ADD PRIMARY KEY (`connection_id`),
  ADD KEY `idx_initiator_id` (`initiator_id`),
  ADD KEY `idx_receiver_id` (`receiver_id`),
  ADD KEY `idx_status` (`status`);

--
-- Indexes for table `friendships`
--
ALTER TABLE `friendships`
  ADD PRIMARY KEY (`friendship_id`),
  ADD UNIQUE KEY `unique_friendship` (`user1_id`,`user2_id`),
  ADD KEY `idx_user1` (`user1_id`),
  ADD KEY `idx_user2` (`user2_id`),
  ADD KEY `idx_status` (`status`),
  ADD KEY `idx_created_date` (`created_date`),
  ADD KEY `idx_friendships_user1_user2` (`user1_id`,`user2_id`),
  ADD KEY `idx_friendships_status` (`status`);

--
-- Indexes for table `hobbies`
--
ALTER TABLE `hobbies`
  ADD PRIMARY KEY (`hobby_id`),
  ADD KEY `idx_user_id` (`user_id`),
  ADD KEY `idx_category` (`category`);

--
-- Indexes for table `meetings`
--
ALTER TABLE `meetings`
  ADD PRIMARY KEY (`meeting_id`),
  ADD KEY `idx_connection_id` (`connection_id`),
  ADD KEY `idx_organizer_id` (`organizer_id`),
  ADD KEY `idx_scheduled_at` (`scheduled_at`),
  ADD KEY `idx_status` (`status`);

--
-- Indexes for table `meeting_participants`
--
ALTER TABLE `meeting_participants`
  ADD PRIMARY KEY (`participant_id`),
  ADD KEY `idx_meeting_id` (`meeting_id`),
  ADD KEY `idx_user_id` (`user_id`);

--
-- Indexes for table `milestones`
--
ALTER TABLE `milestones`
  ADD PRIMARY KEY (`milestone_id`),
  ADD KEY `idx_hobby_id` (`hobby_id`),
  ADD KEY `idx_is_achieved` (`is_achieved`);

--
-- Indexes for table `posts`
--
ALTER TABLE `posts`
  ADD PRIMARY KEY (`post_id`),
  ADD KEY `idx_user_id` (`user_id`),
  ADD KEY `idx_created_at` (`created_at`);

--
-- Indexes for table `progress`
--
ALTER TABLE `progress`
  ADD PRIMARY KEY (`progress_id`),
  ADD KEY `idx_hobby_id` (`hobby_id`);

--
-- Indexes for table `users`
--
ALTER TABLE `users`
  ADD PRIMARY KEY (`user_id`),
  ADD UNIQUE KEY `username` (`username`),
  ADD UNIQUE KEY `email` (`email`),
  ADD KEY `idx_username` (`username`),
  ADD KEY `idx_email` (`email`),
  ADD KEY `idx_is_online` (`is_online`),
  ADD KEY `idx_created_at` (`created_at`),
  ADD KEY `idx_users_email` (`email`),
  ADD KEY `idx_users_username` (`username`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `badges`
--
ALTER TABLE `badges`
  MODIFY `badge_id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=14;

--
-- AUTO_INCREMENT for table `bookings`
--
ALTER TABLE `bookings`
  MODIFY `booking_id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=21;

--
-- AUTO_INCREMENT for table `classes`
--
ALTER TABLE `classes`
  MODIFY `class_id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=12;

--
-- AUTO_INCREMENT for table `class_providers`
--
ALTER TABLE `class_providers`
  MODIFY `provider_id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=12;

--
-- AUTO_INCREMENT for table `comments`
--
ALTER TABLE `comments`
  MODIFY `comment_id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=23;

--
-- AUTO_INCREMENT for table `friendships`
--
ALTER TABLE `friendships`
  MODIFY `friendship_id` bigint(20) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `hobbies`
--
ALTER TABLE `hobbies`
  MODIFY `hobby_id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=17;

--
-- AUTO_INCREMENT for table `milestones`
--
ALTER TABLE `milestones`
  MODIFY `milestone_id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=13;

--
-- AUTO_INCREMENT for table `posts`
--
ALTER TABLE `posts`
  MODIFY `post_id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=27;

--
-- AUTO_INCREMENT for table `progress`
--
ALTER TABLE `progress`
  MODIFY `progress_id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=14;

--
-- AUTO_INCREMENT for table `users`
--
ALTER TABLE `users`
  MODIFY `user_id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=49;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `badges`
--
ALTER TABLE `badges`
  ADD CONSTRAINT `badges_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE;

--
-- Constraints for table `bookings`
--
ALTER TABLE `bookings`
  ADD CONSTRAINT `bookings_ibfk_1` FOREIGN KEY (`class_id`) REFERENCES `classes` (`class_id`) ON DELETE CASCADE,
  ADD CONSTRAINT `bookings_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE;

--
-- Constraints for table `classes`
--
ALTER TABLE `classes`
  ADD CONSTRAINT `classes_ibfk_1` FOREIGN KEY (`provider_id`) REFERENCES `class_providers` (`provider_id`) ON DELETE CASCADE;

--
-- Constraints for table `class_providers`
--
ALTER TABLE `class_providers`
  ADD CONSTRAINT `class_providers_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE;

--
-- Constraints for table `comments`
--
ALTER TABLE `comments`
  ADD CONSTRAINT `comments_ibfk_1` FOREIGN KEY (`post_id`) REFERENCES `posts` (`post_id`) ON DELETE CASCADE,
  ADD CONSTRAINT `comments_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE;

--
-- Constraints for table `connections`
--
ALTER TABLE `connections`
  ADD CONSTRAINT `connections_ibfk_1` FOREIGN KEY (`initiator_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE,
  ADD CONSTRAINT `connections_ibfk_2` FOREIGN KEY (`receiver_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE;

--
-- Constraints for table `friendships`
--
ALTER TABLE `friendships`
  ADD CONSTRAINT `friendships_ibfk_1` FOREIGN KEY (`user1_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE,
  ADD CONSTRAINT `friendships_ibfk_2` FOREIGN KEY (`user2_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE;

--
-- Constraints for table `hobbies`
--
ALTER TABLE `hobbies`
  ADD CONSTRAINT `hobbies_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE;

--
-- Constraints for table `meetings`
--
ALTER TABLE `meetings`
  ADD CONSTRAINT `meetings_ibfk_1` FOREIGN KEY (`connection_id`) REFERENCES `connections` (`connection_id`) ON DELETE CASCADE,
  ADD CONSTRAINT `meetings_ibfk_2` FOREIGN KEY (`organizer_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE;

--
-- Constraints for table `meeting_participants`
--
ALTER TABLE `meeting_participants`
  ADD CONSTRAINT `meeting_participants_ibfk_1` FOREIGN KEY (`meeting_id`) REFERENCES `meetings` (`meeting_id`) ON DELETE CASCADE,
  ADD CONSTRAINT `meeting_participants_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE;

--
-- Constraints for table `milestones`
--
ALTER TABLE `milestones`
  ADD CONSTRAINT `milestones_ibfk_1` FOREIGN KEY (`hobby_id`) REFERENCES `hobbies` (`hobby_id`) ON DELETE CASCADE;

--
-- Constraints for table `posts`
--
ALTER TABLE `posts`
  ADD CONSTRAINT `posts_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`) ON DELETE CASCADE;

--
-- Constraints for table `progress`
--
ALTER TABLE `progress`
  ADD CONSTRAINT `progress_ibfk_1` FOREIGN KEY (`hobby_id`) REFERENCES `hobbies` (`hobby_id`) ON DELETE CASCADE;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
