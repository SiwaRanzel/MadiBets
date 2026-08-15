-- Migration: multi-outcome bets + admin-set deadlines.
-- Run ONCE on an existing madibets database that already has the Wager table
-- (db/migrate_multi_wager.sql). Fresh databases should use madibets_schema.sql.

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
  ADD COLUMN deadline DATETIME NULL AFTER status,
  ADD COLUMN winningOutcomeID INT NULL AFTER deadline;

-- Every existing bet was implicitly Yes/No: "Yes" carries the old single odds,
-- "No" stays unpriced (there was never a No side to wager on).
INSERT INTO BetOutcome (betID, label, odds, position)
SELECT betID, 'Yes', odds, 0 FROM Bet;
INSERT INTO BetOutcome (betID, label, odds, position)
SELECT betID, 'No', NULL, 1 FROM Bet;

-- Existing wagers all rode the implicit Yes side.
ALTER TABLE Wager ADD COLUMN outcomeID INT NULL AFTER betID;
UPDATE Wager w
JOIN BetOutcome o ON o.betID = w.betID AND o.label = 'Yes'
SET w.outcomeID = o.outcomeID;
ALTER TABLE Wager
  MODIFY outcomeID INT NOT NULL,
  ADD CONSTRAINT fk_wager_outcome FOREIGN KEY (outcomeID) REFERENCES BetOutcome(outcomeID);

-- Old gradings: YES meant the Yes side won, NO meant it lost (No side "won").
UPDATE Bet b JOIN BetOutcome o ON o.betID = b.betID AND o.label = 'Yes'
SET b.winningOutcomeID = o.outcomeID WHERE b.outcome = 'YES';
UPDATE Bet b JOIN BetOutcome o ON o.betID = b.betID AND o.label = 'No'
SET b.winningOutcomeID = o.outcomeID WHERE b.outcome = 'NO';

-- Collapse the outcome enum: which outcome won now lives in winningOutcomeID.
ALTER TABLE Bet MODIFY outcome
  ENUM('PENDING','YES','NO','CANCELLED','DECIDED') NOT NULL DEFAULT 'PENDING';
UPDATE Bet SET outcome = 'DECIDED' WHERE outcome IN ('YES','NO');
ALTER TABLE Bet MODIFY outcome
  ENUM('PENDING','DECIDED','CANCELLED') NOT NULL DEFAULT 'PENDING';

ALTER TABLE Bet
  ADD CONSTRAINT fk_bet_winner FOREIGN KEY (winningOutcomeID) REFERENCES BetOutcome(outcomeID);

-- The single odds column moves to BetOutcome.
ALTER TABLE Bet DROP COLUMN odds;
