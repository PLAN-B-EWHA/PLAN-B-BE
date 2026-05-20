-- Remove therapist memo feature tables
-- Safe to run even if table was never created (e.g. different env history)

DROP TABLE IF EXISTS therapist_memo CASCADE;
