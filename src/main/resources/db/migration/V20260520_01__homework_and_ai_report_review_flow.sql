ALTER TABLE homework_reports
    ADD COLUMN IF NOT EXISTS reviewed_by UUID,
    ADD COLUMN IF NOT EXISTS therapist_review_comment TEXT,
    ADD COLUMN IF NOT EXISTS reviewed_at TIMESTAMP;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_homework_reports_reviewed_by'
    ) THEN
        ALTER TABLE homework_reports
            ADD CONSTRAINT fk_homework_reports_reviewed_by
            FOREIGN KEY (reviewed_by) REFERENCES users(user_id);
    END IF;
END $$;

DO $$
DECLARE
    constraint_row RECORD;
BEGIN
    FOR constraint_row IN
        SELECT c.conname
        FROM pg_constraint c
        JOIN pg_class t ON t.oid = c.conrelid
        WHERE t.relname = 'homework_assignments'
          AND c.contype = 'c'
          AND pg_get_constraintdef(c.oid) LIKE '%status%'
    LOOP
        EXECUTE format('ALTER TABLE homework_assignments DROP CONSTRAINT IF EXISTS %I', constraint_row.conname);
    END LOOP;
END $$;

ALTER TABLE homework_assignments
    ADD CONSTRAINT homework_assignments_status_check
    CHECK (status IN ('PENDING', 'SUBMITTED', 'REVIEWED', 'CANCELED'));

CREATE TABLE IF NOT EXISTS ai_reports (
    report_id UUID PRIMARY KEY,
    child_id UUID NOT NULL REFERENCES children(child_id),
    generated_by UUID NOT NULL REFERENCES users(user_id),
    reviewed_by UUID REFERENCES users(user_id),
    report_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('DRAFT', 'REVIEWED', 'PUBLISHED', 'ARCHIVED')),
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    rag_context_snapshot TEXT,
    prompt_snapshot TEXT,
    model VARCHAR(100),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    reviewed_at TIMESTAMP,
    published_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ai_report_child_created ON ai_reports(child_id, created_at);
CREATE INDEX IF NOT EXISTS idx_ai_report_child_status ON ai_reports(child_id, status);
