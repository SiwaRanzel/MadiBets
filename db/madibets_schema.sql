-- ============================================================
-- MadiBets - Database Schema (MySQL 8.0)
-- The Bloodline | WRRV301
-- Derived from Section 3.1 "List of data and attributes".
-- Read the DESIGN NOTES at the bottom before you build on this.
-- ============================================================

DROP DATABASE IF EXISTS madibets;
CREATE DATABASE madibets
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
USE madibets;

-- ------------------------------------------------------------
-- Core identity (User + subtypes)
-- Subtype tables share the User primary key (1:1 with User).
-- ------------------------------------------------------------
CREATE TABLE User (
  userID    INT AUTO_INCREMENT PRIMARY KEY,
  name      VARCHAR(100) NOT NULL,
  surname   VARCHAR(100) NOT NULL,
  email     VARCHAR(255) NOT NULL UNIQUE,
  password  VARCHAR(255) NOT NULL,                 -- store a HASH, never plaintext (see notes)
  userType  ENUM('STUDENT','LECTURER','ADMIN') NOT NULL
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

-- ------------------------------------------------------------
-- Accounting (simulated - no real money; MadiBucks only)
-- ------------------------------------------------------------
CREATE TABLE Account (
  accountID INT AUTO_INCREMENT PRIMARY KEY,         -- 3.1 wrote "acountID" (typo, corrected here)
  balance   DECIMAL(12,2) NOT NULL DEFAULT 100.00,  -- 100 MadiBucks granted (business rule)
  userID    INT NOT NULL UNIQUE,
  CONSTRAINT fk_account_user FOREIGN KEY (userID) REFERENCES User(userID) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE `Transaction` (                        -- backticked: TRANSACTION is a MySQL keyword
  transactionID INT AUTO_INCREMENT PRIMARY KEY,
  payoutAmount  DECIMAL(12,2) NOT NULL,
  description   VARCHAR(255),
  `date`        DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
  -- NOTE: 3.1 gives no link to an Account/User. See DESIGN NOTES #2.
  -- , accountID INT NOT NULL,
  -- , CONSTRAINT fk_txn_account FOREIGN KEY (accountID) REFERENCES Account(accountID)
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- Betting
-- ------------------------------------------------------------
CREATE TABLE Event (
  eventID          INT AUTO_INCREMENT PRIMARY KEY,
  eventDescription VARCHAR(255) NOT NULL,
  `date`           DATE,
  startTime        TIME,
  endTime          TIME,
  status           ENUM('UPCOMING','LIVE','ENDED','CANCELLED') NOT NULL DEFAULT 'UPCOMING'
) ENGINE=InnoDB;

CREATE TABLE Bet (
  betID         INT AUTO_INCREMENT PRIMARY KEY,
  userID        INT NOT NULL,           -- proposer/placer (see DESIGN NOTES #1: market vs wager)
  eventID       INT,
  description   VARCHAR(255) NOT NULL,
  odds          DECIMAL(6,2),
  amountToBeWon DECIMAL(12,2),
  outcome       ENUM('PENDING','YES','NO','CANCELLED') NOT NULL DEFAULT 'PENDING',
  proposedDate  DATETIME,
  placedDate    DATETIME,
  gradedDate    DATETIME,
  gradedBy      INT,                     -- admin userID
  CONSTRAINT fk_bet_user   FOREIGN KEY (userID)   REFERENCES User(userID),
  CONSTRAINT fk_bet_event  FOREIGN KEY (eventID)  REFERENCES Event(eventID),
  CONSTRAINT fk_bet_grader FOREIGN KEY (gradedBy) REFERENCES User(userID)
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- Social (friends)
-- ------------------------------------------------------------
CREATE TABLE Friendship (
  friendshipID INT AUTO_INCREMENT PRIMARY KEY,
  requesterID  INT NOT NULL,
  addresseID   INT NOT NULL,             -- spec spelling kept (intended: "addressee")
  status       ENUM('PENDING','ACCEPTED','REJECTED') NOT NULL DEFAULT 'PENDING',
  CONSTRAINT fk_friend_requester FOREIGN KEY (requesterID) REFERENCES User(userID) ON DELETE CASCADE,
  CONSTRAINT fk_friend_addressee FOREIGN KEY (addresseID)  REFERENCES User(userID) ON DELETE CASCADE,
  CONSTRAINT uq_friend_pair UNIQUE (requesterID, addresseID)
) ENGINE=InnoDB;

-- ------------------------------------------------------------
-- Groups / Leagues
-- ------------------------------------------------------------
CREATE TABLE `Group` (                    -- backticked: GROUP is a reserved word in MySQL
  groupID     INT AUTO_INCREMENT PRIMARY KEY,
  groupName   VARCHAR(100) NOT NULL,
  description VARCHAR(255),
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

-- ------------------------------------------------------------
-- Tasks (lecturer-set, reward MadiBucks)
-- ------------------------------------------------------------
CREATE TABLE Task (
  taskID    INT AUTO_INCREMENT PRIMARY KEY,
  userID    INT,                          -- assignee per 3.1; see DESIGN NOTES #3 (group linkage)
  amount    DECIMAL(12,2) NOT NULL,        -- MadiBucks reward
  createdBy INT NOT NULL,                  -- lecturer userID
  CONSTRAINT fk_task_user    FOREIGN KEY (userID)    REFERENCES User(userID),
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

-- ------------------------------------------------------------
-- Support queries
-- ------------------------------------------------------------
CREATE TABLE Query (
  queryID        INT AUTO_INCREMENT PRIMARY KEY,
  description    VARCHAR(1000) NOT NULL,
  queryDate      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  userID         INT NOT NULL,
  resolvedStatus ENUM('OPEN','RESOLVED') NOT NULL DEFAULT 'OPEN',
  CONSTRAINT fk_query_user FOREIGN KEY (userID) REFERENCES User(userID) ON DELETE CASCADE
) ENGINE=InnoDB;

-- ============================================================
-- DESIGN NOTES (discuss as a team before building on this)
-- ============================================================
-- 1. BET = MARKET + WAGER. The Bet table mixes the proposed market
--    (odds, eventID, proposedDate) with one user's wager (placedDate,
--    amountToBeWon, userID). Multiple students cannot bet on one market
--    under this shape. Consider splitting into Bet (market) + Wager
--    (wagerID, betID, userID, amountStaked, sidePicked, placedDate).
--    This affects B100, B400, B600, C400.
-- 2. TRANSACTION has no owner in 3.1. B600/C400 need it. Uncomment the
--    accountID FK above (or add userID) once the team agrees.
-- 3. TASK has no groupID, but D400's narrative says tasks belong to a
--    group. If tasks are group-scoped, add groupID + FK to `Group`.
-- 4. BET TYPE (Academic/Sport/Social) appears in every bets UI but is not
--    in 3.1. Add e.g. category ENUM('ACADEMIC','SPORT','SOCIAL') to Bet
--    (or Event) if the demo needs the Type column.
-- 5. PASSWORDS: never store plaintext. Hash with bcrypt/Argon2 at register
--    (A100) and compare the hash at login (A200). POPI Act compliance.
-- 6. LEAGUES (business rule) are not modelled. Decide if in scope.
-- ============================================================
