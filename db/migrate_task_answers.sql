-- Migration: add correctAnswer column to Task for True/False task answering.
-- Run this ONCE on an existing madibets database. Fresh databases should use
-- madibets_schema.sql instead (update it to include the column).

ALTER TABLE Task ADD COLUMN correctAnswer BOOLEAN NULL AFTER createdBy;