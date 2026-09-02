-- Migration: add an optional maximum member cap to Group.
-- Run this ONCE on an existing madibets database. Fresh databases should use
-- madibets_schema.sql instead (which already includes the column).
--
-- maxMembers counts joining members only and EXCLUDES the owner/creator, so a
-- cap of 10 allows the owner plus 10 other members. NULL means no limit.

ALTER TABLE `Group` ADD COLUMN maxMembers INT NULL AFTER password;
