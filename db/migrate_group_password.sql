-- Migration: add an optional password to Group.
-- Run this ONCE on an existing madibets database. Fresh databases should use
-- madibets_schema.sql instead (which already includes the column).
--
-- The password is stored as a bcrypt HASH (never plaintext, POPI Act — see
-- DESIGN NOTE #5). A NULL password means the group is open (no password
-- required to join).

ALTER TABLE `Group` ADD COLUMN password VARCHAR(255) NULL AFTER description;
