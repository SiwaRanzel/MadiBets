-- Migration: login-time notifications (bet settlement messages).
-- Run ONCE on an existing madibets database. Fresh databases should use
-- madibets_schema.sql instead, which already has this table.

CREATE TABLE Notification (
  notificationID INT AUTO_INCREMENT PRIMARY KEY,
  userID         INT NOT NULL,
  message        VARCHAR(255) NOT NULL,
  createdDate    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  seen           TINYINT(1) NOT NULL DEFAULT 0,
  CONSTRAINT fk_notif_user FOREIGN KEY (userID) REFERENCES User(userID) ON DELETE CASCADE
) ENGINE=InnoDB;
