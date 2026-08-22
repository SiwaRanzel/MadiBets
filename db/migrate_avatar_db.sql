-- Migration to add avatar data columns directly to User table.
-- Safe to run after madibets_schema.sql — skips if columns already exist.

SET @dbname = 'madibets';
SET @tablename = 'User';

-- Add avatarData column if it doesn't exist
SET @col = 'avatarData';
SET @q = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = @col) = 0,
    'ALTER TABLE User ADD COLUMN avatarData LONGBLOB',
    'SELECT ''avatarData column already exists'' AS info'
);
PREPARE stmt FROM @q;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Add avatarType column if it doesn't exist
SET @col = 'avatarType';
SET @q = IF(
    (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
     WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = @col) = 0,
    'ALTER TABLE User ADD COLUMN avatarType VARCHAR(50)',
    'SELECT ''avatarType column already exists'' AS info'
);
PREPARE stmt FROM @q;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
