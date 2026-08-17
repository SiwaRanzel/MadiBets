-- Migration: split Bet (market) from Wager (student stakes) — design note #1.
-- Run this ONCE on an existing madibets database. Fresh databases should use
-- madibets_schema.sql instead, which already has the new shape.

CREATE TABLE Wager (
  wagerID       INT AUTO_INCREMENT PRIMARY KEY,
  betID         INT NOT NULL,
  userID        INT NOT NULL,
  stake         DECIMAL(12,2) NOT NULL,
  amountToBeWon DECIMAL(12,2) NOT NULL,
  placedDate    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT fk_wager_bet  FOREIGN KEY (betID)  REFERENCES Bet(betID) ON DELETE CASCADE,
  CONSTRAINT fk_wager_user FOREIGN KEY (userID) REFERENCES User(userID),
  CONSTRAINT uq_wager UNIQUE (betID, userID)
) ENGINE=InnoDB;

-- Carry existing single wagers over. The old shape stored no stake, so it is
-- derived one last time as payout / odds (the same maths B600 used to refund).
INSERT INTO Wager (betID, userID, stake, amountToBeWon, placedDate)
SELECT betID, userID, ROUND(amountToBeWon / odds, 2), amountToBeWon, placedDate
FROM Bet
WHERE placedDate IS NOT NULL AND amountToBeWon IS NOT NULL AND odds IS NOT NULL;

-- Bet.userID goes back to meaning "proposer" only; the wager columns move out.
ALTER TABLE Bet DROP COLUMN placedDate, DROP COLUMN amountToBeWon;
