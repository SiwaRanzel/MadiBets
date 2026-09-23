-- Migration: add a per-quiz "results revealed" flag to Task.
-- Run this ONCE on an existing madibets database. Fresh databases should use
-- madibets_schema.sql instead (which already includes the column).
--
-- Quiz results (correct answers + score) stay hidden from students until the
-- lecturer who created the quiz reveals them. resultsRevealed = 0 means hidden;
-- 1 means students can now see which answers were right, exactly as they do
-- immediately after completing today. The creating lecturer (and admins) can
-- always see the correct answers regardless of this flag.

ALTER TABLE Task ADD COLUMN resultsRevealed BOOLEAN NOT NULL DEFAULT 0 AFTER correctAnswer;
