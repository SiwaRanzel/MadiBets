-- ============================================================
-- MadiBets - Complete Database Schema (MySQL 8.0)
-- The Bloodline | WRRV301
--
-- This is the single canonical script. Run it once on a fresh
-- MySQL instance — it creates the database, all tables (with
-- every migration already applied), and seeds the initial
-- admin account.
--
-- Usage:
--   mysql -u root -p < db/madibets_schema.sql
-- ============================================================

DROP DATABASE IF EXISTS madibets;
CREATE DATABASE madibets
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
USE madibets;

-- ============================================================
-- SECTION 1: Core Identity  (User + subtypes)
-- Subtype tables share the User primary key (1:1 with User).
-- avatarPath / avatarData / avatarType added by migrate_avatar_db.
-- ============================================================
CREATE TABLE User (
  userID      INT AUTO_INCREMENT PRIMARY KEY,
  name        VARCHAR(100)  NOT NULL,
  surname     VARCHAR(100)  NOT NULL,
  email       VARCHAR(255)  NOT NULL UNIQUE,
  password    VARCHAR(255)  NOT NULL,                 -- store a HASH, never plaintext (POPI Act)
  userType    ENUM('STUDENT','LECTURER','ADMIN') NOT NULL,
  avatarPath  VARCHAR(255),
  avatarData  LONGBLOB,
  avatarType  VARCHAR(50),
  createdDate DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE Student (
  userID     INT PRIMARY KEY,
  studentNo  VARCHAR(20) NOT NULL UNIQUE,
  CONSTRAINT fk_student_user FOREIGN KEY (userID) REFERENCES User(userID) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE Lecturer (
  userID   INT PRIMARY KEY,
  staffNo  VARCHAR(20) NOT NULL UNIQUE,
  CONSTRAINT fk_lecturer_user FOREIGN KEY (userID) REFERENCES User(userID) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE Admin (
  userID INT PRIMARY KEY,
  CONSTRAINT fk_admin_user FOREIGN KEY (userID) REFERENCES User(userID) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ============================================================
-- SECTION 2: Accounting  (simulated — MadiBucks only, no real money)
-- ============================================================
CREATE TABLE Account (
  accountID INT AUTO_INCREMENT PRIMARY KEY,
  balance   DECIMAL(12,2) NOT NULL DEFAULT 100.00,   -- 100 MadiBucks granted on registration
  userID    INT NOT NULL UNIQUE,
  CONSTRAINT fk_account_user FOREIGN KEY (userID) REFERENCES User(userID) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE `Transaction` (                          -- backticked: TRANSACTION is a MySQL keyword
  transactionID INT AUTO_INCREMENT PRIMARY KEY,
  payoutAmount  DECIMAL(12,2) NOT NULL,
  description   VARCHAR(255),
  `date`        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  accountID     INT NOT NULL,
  CONSTRAINT fk_txn_account FOREIGN KEY (accountID) REFERENCES Account(accountID)
) ENGINE=InnoDB;

-- ============================================================
-- SECTION 3: Betting
-- A Bet is a market; a Wager is one student's stake on one outcome.
-- BetOutcome stores the 2-4 possible outcomes and their odds.
-- (Design note #1 resolved 2026-08-10; migrate_multi_wager +
--  migrate_multi_outcome are already baked in here.)
-- ============================================================
CREATE TABLE Event (
  eventID          INT AUTO_INCREMENT PRIMARY KEY,
  eventDescription VARCHAR(255) NOT NULL,
  `date`           DATE,
  startTime        TIME,
  endTime          TIME,
  status           ENUM('UPCOMING','LIVE','ENDED','CANCELLED') NOT NULL DEFAULT 'UPCOMING'
) ENGINE=InnoDB;

CREATE TABLE Bet (
  betID            INT AUTO_INCREMENT PRIMARY KEY,
  userID           INT NOT NULL,        -- proposer
  eventID          INT,
  description      VARCHAR(255) NOT NULL,
  outcome          ENUM('PENDING','DECIDED','CANCELLED') NOT NULL DEFAULT 'PENDING',
  status           ENUM('PROPOSED','ACTIVE','GRADED','DELETED') NOT NULL DEFAULT 'PROPOSED',
  deadline         DATETIME,            -- admin-set at approval; wagering closes when it passes
  winningOutcomeID INT,                 -- set when graded DECIDED (FK added after BetOutcome)
  proposedDate     DATETIME,
  gradedDate       DATETIME,
  gradedBy         INT,                 -- admin userID
  CONSTRAINT fk_bet_user   FOREIGN KEY (userID)   REFERENCES User(userID),
  CONSTRAINT fk_bet_event  FOREIGN KEY (eventID)  REFERENCES Event(eventID),
  CONSTRAINT fk_bet_grader FOREIGN KEY (gradedBy) REFERENCES User(userID)
) ENGINE=InnoDB;

-- 2-4 possible outcomes per bet (e.g. "Madibaz win" / "Wits win" / "Draw").
-- The proposer names them; the admin sets odds at approval (odds stays NULL while PROPOSED).
CREATE TABLE BetOutcome (
  outcomeID INT AUTO_INCREMENT PRIMARY KEY,
  betID     INT NOT NULL,
  label     VARCHAR(100) NOT NULL,
  odds      DECIMAL(6,2),
  position  INT NOT NULL DEFAULT 0,
  CONSTRAINT fk_outcome_bet FOREIGN KEY (betID) REFERENCES Bet(betID) ON DELETE CASCADE,
  CONSTRAINT uq_outcome UNIQUE (betID, label)
) ENGINE=InnoDB;

ALTER TABLE Bet
  ADD CONSTRAINT fk_bet_winner FOREIGN KEY (winningOutcomeID) REFERENCES BetOutcome(outcomeID);

-- One student's stake on one outcome of one market.
-- stake is stored explicitly so refunds don't need to be derived.
CREATE TABLE Wager (
  wagerID       INT AUTO_INCREMENT PRIMARY KEY,
  betID         INT NOT NULL,
  outcomeID     INT NOT NULL,            -- the outcome this student backed
  userID        INT NOT NULL,
  stake         DECIMAL(12,2) NOT NULL,
  amountToBeWon DECIMAL(12,2) NOT NULL,  -- stake × odds, frozen at placement
  placedDate    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_wager_bet     FOREIGN KEY (betID)     REFERENCES Bet(betID) ON DELETE CASCADE,
  CONSTRAINT fk_wager_outcome FOREIGN KEY (outcomeID) REFERENCES BetOutcome(outcomeID),
  CONSTRAINT fk_wager_user    FOREIGN KEY (userID)    REFERENCES User(userID),
  CONSTRAINT uq_wager UNIQUE (betID, userID)           -- one wager per student per bet
) ENGINE=InnoDB;

-- ============================================================
-- SECTION 4: Social  (friends)
-- ============================================================
CREATE TABLE Friendship (
  friendshipID INT AUTO_INCREMENT PRIMARY KEY,
  requesterID  INT NOT NULL,
  addresseID   INT NOT NULL,             -- spec spelling kept (intended: "addressee")
  status       ENUM('PENDING','ACCEPTED','REJECTED') NOT NULL DEFAULT 'PENDING',
  CONSTRAINT fk_friend_requester FOREIGN KEY (requesterID) REFERENCES User(userID) ON DELETE CASCADE,
  CONSTRAINT fk_friend_addressee FOREIGN KEY (addresseID)  REFERENCES User(userID) ON DELETE CASCADE,
  CONSTRAINT uq_friend_pair UNIQUE (requesterID, addresseID)
) ENGINE=InnoDB;

-- ============================================================
-- SECTION 5: Groups / Leagues
-- password (bcrypt HASH; NULL = open group) added by migrate_group_password.
-- maxMembers (excludes owner; NULL = unlimited) added by migrate_group_max_members.
-- ============================================================
CREATE TABLE `Group` (                    -- backticked: GROUP is a reserved word in MySQL
  groupID     INT AUTO_INCREMENT PRIMARY KEY,
  groupName   VARCHAR(100) NOT NULL,
  description VARCHAR(255),
  password    VARCHAR(255) NULL,          -- bcrypt HASH; NULL means the group is open
  maxMembers  INT NULL,                   -- max joining members (EXCLUDES owner); NULL = unlimited
  createdBy   INT NOT NULL,
  createdDate DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_group_creator FOREIGN KEY (createdBy) REFERENCES User(userID)
) ENGINE=InnoDB;

CREATE TABLE GroupMember (
  groupMemberID INT AUTO_INCREMENT PRIMARY KEY,
  groupID       INT NOT NULL,
  userID        INT NOT NULL,
  role          ENUM('OWNER','MEMBER') NOT NULL DEFAULT 'MEMBER',
  CONSTRAINT fk_gm_group FOREIGN KEY (groupID) REFERENCES `Group`(groupID) ON DELETE CASCADE,
  CONSTRAINT fk_gm_user  FOREIGN KEY (userID)  REFERENCES User(userID)     ON DELETE CASCADE,
  CONSTRAINT uq_gm UNIQUE (groupID, userID)
) ENGINE=InnoDB;

-- ============================================================
-- SECTION 6: Tasks / Quizzes  (lecturer-set, reward MadiBucks)
-- correctAnswer added by migrate_task_answers.
-- resultsRevealed added by migrate_quiz_reveal.
-- TaskQuestion, TaskOption, TaskSubmission, TaskAnswer added by migrate_quiz_tasks.
-- ============================================================
CREATE TABLE Task (
  taskID          INT AUTO_INCREMENT PRIMARY KEY,
  title           VARCHAR(255) NOT NULL,
  description     VARCHAR(1000),
  groupID         INT NOT NULL,                  -- tasks are scoped to a group
  userID          INT,                           -- optional specific assignee
  amount          DECIMAL(12,2) NOT NULL,        -- MadiBucks reward
  createdBy       INT NOT NULL,                  -- lecturer userID
  correctAnswer   BOOLEAN NULL,                  -- True/False answer for legacy single-answer tasks
  resultsRevealed BOOLEAN NOT NULL DEFAULT 0,    -- lecturer reveals quiz results to students
  CONSTRAINT fk_task_group   FOREIGN KEY (groupID)   REFERENCES `Group`(groupID) ON DELETE CASCADE,
  CONSTRAINT fk_task_user    FOREIGN KEY (userID)    REFERENCES User(userID) ON DELETE SET NULL,
  CONSTRAINT fk_task_creator FOREIGN KEY (createdBy) REFERENCES User(userID)
) ENGINE=InnoDB;

CREATE TABLE TaskCompletion (
  completionID     INT AUTO_INCREMENT PRIMARY KEY,
  taskID           INT NOT NULL,
  userID           INT NOT NULL,
  completionStatus ENUM('PENDING','COMPLETED','REJECTED') NOT NULL DEFAULT 'PENDING',
  completionDate   DATETIME,
  CONSTRAINT fk_tc_task FOREIGN KEY (taskID) REFERENCES Task(taskID) ON DELETE CASCADE,
  CONSTRAINT fk_tc_user FOREIGN KEY (userID) REFERENCES User(userID) ON DELETE CASCADE
) ENGINE=InnoDB;

-- A Task is a quiz of up to 10 questions.
-- Each question is TRUE_FALSE or MULTIPLE_CHOICE and owns 2-4 options
-- with exactly one flagged isCorrect.
CREATE TABLE TaskQuestion (
  questionID INT AUTO_INCREMENT PRIMARY KEY,
  taskID     INT NOT NULL,
  prompt     VARCHAR(1000) NOT NULL,
  type       ENUM('TRUE_FALSE','MULTIPLE_CHOICE') NOT NULL,
  position   INT NOT NULL DEFAULT 0,
  CONSTRAINT fk_tq_task FOREIGN KEY (taskID) REFERENCES Task(taskID) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE TaskOption (
  optionID   INT AUTO_INCREMENT PRIMARY KEY,
  questionID INT NOT NULL,
  optionText VARCHAR(500) NOT NULL,
  isCorrect  BOOLEAN NOT NULL DEFAULT 0,
  position   INT NOT NULL DEFAULT 0,
  CONSTRAINT fk_to_question FOREIGN KEY (questionID) REFERENCES TaskQuestion(questionID) ON DELETE CASCADE
) ENGINE=InnoDB;

-- One submission per student per quiz (UNIQUE).
-- Score and award are frozen at submit time; results stay hidden until
-- the lecturer sets resultsRevealed = 1 on the parent Task.
-- Reward: 10 MadiBucks per correct answer, credited at submit time.
CREATE TABLE TaskSubmission (
  submissionID     INT AUTO_INCREMENT PRIMARY KEY,
  taskID           INT NOT NULL,
  userID           INT NOT NULL,
  score            INT NOT NULL DEFAULT 0,           -- number of correct answers
  awardedMadibucks INT NOT NULL DEFAULT 0,           -- 10 × score
  submittedDate    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_ts_task FOREIGN KEY (taskID) REFERENCES Task(taskID) ON DELETE CASCADE,
  CONSTRAINT fk_ts_user FOREIGN KEY (userID) REFERENCES User(userID) ON DELETE CASCADE,
  CONSTRAINT uq_ts UNIQUE (taskID, userID)           -- one attempt per student
) ENGINE=InnoDB;

CREATE TABLE TaskAnswer (
  answerID       INT AUTO_INCREMENT PRIMARY KEY,
  submissionID   INT NOT NULL,
  questionID     INT NOT NULL,
  chosenOptionID INT NULL,
  isCorrect      BOOLEAN NOT NULL DEFAULT 0,
  CONSTRAINT fk_ta_submission FOREIGN KEY (submissionID) REFERENCES TaskSubmission(submissionID) ON DELETE CASCADE,
  CONSTRAINT fk_ta_question   FOREIGN KEY (questionID)   REFERENCES TaskQuestion(questionID)     ON DELETE CASCADE
) ENGINE=InnoDB;

-- ============================================================
-- SECTION 7: Support
-- ============================================================
CREATE TABLE Query (
  queryID        INT AUTO_INCREMENT PRIMARY KEY,
  title          VARCHAR(255)  NOT NULL,
  description    VARCHAR(1000) NOT NULL,
  queryDate      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  userID         INT NOT NULL,
  resolvedStatus ENUM('OPEN','RESOLVED') NOT NULL DEFAULT 'OPEN',
  CONSTRAINT fk_query_user FOREIGN KEY (userID) REFERENCES User(userID) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE AccountDeletionRequest (
  requestID   INT AUTO_INCREMENT PRIMARY KEY,
  userID      INT NOT NULL,
  requestDate DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  status      ENUM('NEW','DONE','REJOIN') NOT NULL DEFAULT 'NEW',
  CONSTRAINT fk_delreq_user FOREIGN KEY (userID) REFERENCES User(userID) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Login-time messages for students (e.g. how their wager settled).
-- Written at settlement time; shown once at next login, then flagged seen.
-- Added by migrate_notifications.
CREATE TABLE Notification (
  notificationID INT AUTO_INCREMENT PRIMARY KEY,
  userID         INT NOT NULL,
  message        VARCHAR(255) NOT NULL,
  createdDate    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  seen           TINYINT(1) NOT NULL DEFAULT 0,
  CONSTRAINT fk_notif_user FOREIGN KEY (userID) REFERENCES User(userID) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ============================================================
-- SECTION 8: Seed Data
-- Initial admin account (password: "12345", BCrypt hashed).
-- ============================================================
INSERT INTO User (name, surname, email, password, userType)
VALUES ('System', 'Admin', 'admin@mandela.ac.za',
        '$2a$10$zDCw63WppOURRN5/s0LAReSBFNPW9yXHF4SphSH3IzPY1hRalzWi2', 'ADMIN');

SET @adminId = LAST_INSERT_ID();

INSERT INTO Admin (userID)
VALUES (@adminId);

-- ============================================================
-- DESIGN NOTES
-- ============================================================
-- 1. BET = MARKET + WAGER. Resolved 2026-08-10: split into Bet (market)
--    + Wager (one row per student stake, UNIQUE per bet+user). Multiple
--    students can wager on the same market; settlement pays every winning wager.
-- 2. TRANSACTION has no owner in spec 3.1. B600/C400 need it. The accountID
--    FK is already present above; add userID if the team prefers a direct link.
-- 3. PASSWORDS: never store plaintext. Hash with BCrypt at registration (A100)
--    and compare the hash at login (A200). POPI Act compliance.
-- 4. LEAGUES (business rule) are not modelled. Decide if in scope.
-- ============================================================
