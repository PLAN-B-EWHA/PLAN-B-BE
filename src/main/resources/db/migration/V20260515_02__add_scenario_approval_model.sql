ALTER TABLE scenarios
    ADD COLUMN IF NOT EXISTS source VARCHAR(30) NOT NULL DEFAULT 'SERVER_MANUAL',
    ADD COLUMN IF NOT EXISTS approval_status VARCHAR(30) NOT NULL DEFAULT 'PUBLISHED',
    ADD COLUMN IF NOT EXISTS generated_by_user_id UUID,
    ADD COLUMN IF NOT EXISTS reviewed_by_user_id UUID,
    ADD COLUMN IF NOT EXISTS review_note TEXT,
    ADD COLUMN IF NOT EXISTS llm_prompt TEXT,
    ADD COLUMN IF NOT EXISTS llm_model VARCHAR(100),
    ADD COLUMN IF NOT EXISTS published_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS archived_at TIMESTAMP;

UPDATE scenarios
SET published_at = COALESCE(published_at, updated_at, created_at, CURRENT_TIMESTAMP)
WHERE approval_status = 'PUBLISHED';

CREATE INDEX IF NOT EXISTS idx_scenarios_source_status
    ON scenarios(source, approval_status);

CREATE INDEX IF NOT EXISTS idx_scenarios_week_status
    ON scenarios(week, approval_status);
