-- Migration: turn a Task into a multi-question QUIZ (True/False + Multiple Choice).
-- Run this ONCE on an existing madibets database. Fresh databases should use
-- madibets_schema.sql instead (which already includes these tables).
--
-- A Task now owns up to 10 TaskQuestion rows; each question owns 2-4 TaskOption
-- rows with exactly one flagged isCorrect. A student submits the whole quiz once
-- (TaskSubmission, UNIQUE per task+user) and their per-question choices are
-- stored in TaskAnswer. The reward is a fixed 10 MadiBucks per correct answer,
-- computed and credited at submit time — nothing is revealed before then.
--
-- The legacy Task.correctAnswer / Task.amount columns are kept for back-compat;
-- new tasks route through the quiz tables below.

CREATE TABLE IF NOT EXISTS TaskQuestion (
  questionID  INT AUTO_INCREMENT PRIMARY KEY,
  taskID      INT NOT NULL,
  prompt      VARCHAR(1000) NOT NULL,
  type        ENUM('TRUE_FALSE','MULTIPLE_CHOICE') NOT NULL,
  position    INT NOT NULL DEFAULT 0,
  CONSTRAINT fk_tq_task FOREIGN KEY (taskID) REFERENCES Task(taskID) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS TaskOption (
  optionID    INT AUTO_INCREMENT PRIMARY KEY,
  questionID  INT NOT NULL,
  optionText  VARCHAR(500) NOT NULL,
  isCorrect   BOOLEAN NOT NULL DEFAULT 0,
  position    INT NOT NULL DEFAULT 0,
  CONSTRAINT fk_to_question FOREIGN KEY (questionID) REFERENCES TaskQuestion(questionID) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS TaskSubmission (
  submissionID     INT AUTO_INCREMENT PRIMARY KEY,
  taskID           INT NOT NULL,
  userID           INT NOT NULL,
  score            INT NOT NULL DEFAULT 0,          -- number of correct answers
  awardedMadibucks INT NOT NULL DEFAULT 0,          -- 10 * score
  submittedDate    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_ts_task FOREIGN KEY (taskID) REFERENCES Task(taskID) ON DELETE CASCADE,
  CONSTRAINT fk_ts_user FOREIGN KEY (userID) REFERENCES User(userID) ON DELETE CASCADE,
  CONSTRAINT uq_ts UNIQUE (taskID, userID)          -- one attempt per student
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS TaskAnswer (
  answerID       INT AUTO_INCREMENT PRIMARY KEY,
  submissionID   INT NOT NULL,
  questionID     INT NOT NULL,
  chosenOptionID INT NULL,
  isCorrect      BOOLEAN NOT NULL DEFAULT 0,
  CONSTRAINT fk_ta_submission FOREIGN KEY (submissionID) REFERENCES TaskSubmission(submissionID) ON DELETE CASCADE,
  CONSTRAINT fk_ta_question   FOREIGN KEY (questionID)   REFERENCES TaskQuestion(questionID)   ON DELETE CASCADE
) ENGINE=InnoDB;
