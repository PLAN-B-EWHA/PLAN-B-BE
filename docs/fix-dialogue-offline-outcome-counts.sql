-- Run the UPDATE statements first. They fix existing production rows that can
-- still contain NULL even though the application now treats NULL as zero.
UPDATE dialogue_stat_summary
SET offline_reviewed_count = 0
WHERE offline_reviewed_count IS NULL;

UPDATE dialogue_stat_summary
SET offline_spontaneous_count = 0
WHERE offline_spontaneous_count IS NULL;

-- This is quick and prevents new inserts from getting NULL when the column is
-- omitted. It does not rewrite the table.
ALTER TABLE dialogue_stat_summary
    ALTER COLUMN offline_reviewed_count SET DEFAULT 0,
    ALTER COLUMN offline_spontaneous_count SET DEFAULT 0;

-- Optional hardening for a maintenance window:
-- SET NOT NULL needs a stronger table lock and can time out while the service is
-- receiving traffic. Run these later after checking there are no NULL values.
--
-- SELECT COUNT(*) FROM dialogue_stat_summary
-- WHERE offline_reviewed_count IS NULL OR offline_spontaneous_count IS NULL;
--
-- ALTER TABLE dialogue_stat_summary
--     ALTER COLUMN offline_reviewed_count SET NOT NULL,
--     ALTER COLUMN offline_spontaneous_count SET NOT NULL;
