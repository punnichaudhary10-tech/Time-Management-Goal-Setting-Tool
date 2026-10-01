-- ============================================
-- TIME MANAGEMENT AND GOAL SETTING TOOL
-- DATABASE SCHEMA
-- ============================================

CREATE DATABASE IF NOT EXISTS time_management_db;

USE time_management_db;


-- ============================================
-- 1. USERS TABLE
-- Stores Admin and User accounts
-- ============================================

CREATE TABLE IF NOT EXISTS users (
    user_id INT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role ENUM('ADMIN', 'USER') NOT NULL DEFAULT 'USER',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


-- ============================================
-- 2. GOAL PARAMETERS TABLE
-- Admin configures goal types and tracking metrics
-- ============================================

CREATE TABLE IF NOT EXISTS goal_parameters (
    parameter_id INT PRIMARY KEY AUTO_INCREMENT,
    goal_type VARCHAR(100) NOT NULL,
    tracking_metric VARCHAR(100) NOT NULL,
    target_unit VARCHAR(50),
    created_by INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (created_by)
        REFERENCES users(user_id)
        ON DELETE SET NULL
);


-- ============================================
-- 3. GOALS TABLE
-- Stores personal goals created by users
-- ============================================

CREATE TABLE IF NOT EXISTS goals (
    goal_id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    goal_name VARCHAR(150) NOT NULL,
    description TEXT,
    target DECIMAL(10,2) NOT NULL,
    deadline DATE NOT NULL,
    status ENUM('NOT_STARTED', 'IN_PROGRESS', 'COMPLETED')
           DEFAULT 'NOT_STARTED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id)
        REFERENCES users(user_id)
        ON DELETE CASCADE
);


-- ============================================
-- 4. TIME ENTRIES TABLE
-- Stores time spent by users on their goals
-- ============================================

CREATE TABLE IF NOT EXISTS time_entries (
    entry_id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    goal_id INT NOT NULL,
    start_time DATETIME NOT NULL,
    end_time DATETIME NOT NULL,
    duration_minutes INT,

    FOREIGN KEY (user_id)
        REFERENCES users(user_id)
        ON DELETE CASCADE,

    FOREIGN KEY (goal_id)
        REFERENCES goals(goal_id)
        ON DELETE CASCADE
);


-- ============================================
-- 5. PROGRESS TABLE
-- Stores progress of each goal
-- ============================================

CREATE TABLE IF NOT EXISTS progress (
    progress_id INT PRIMARY KEY AUTO_INCREMENT,
    goal_id INT NOT NULL,
    progress_percentage DECIMAL(5,2) DEFAULT 0.00,
    completion_status ENUM('NOT_STARTED', 'IN_PROGRESS', 'COMPLETED')
                      DEFAULT 'NOT_STARTED',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                ON UPDATE CURRENT_TIMESTAMP,

    FOREIGN KEY (goal_id)
        REFERENCES goals(goal_id)
        ON DELETE CASCADE
);


-- ============================================
-- 6. USAGE LOGS TABLE
-- Stores user activity for admin monitoring
-- ============================================

CREATE TABLE IF NOT EXISTS usage_logs (
    log_id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    activity VARCHAR(255) NOT NULL,
    activity_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (user_id)
        REFERENCES users(user_id)
        ON DELETE CASCADE
);