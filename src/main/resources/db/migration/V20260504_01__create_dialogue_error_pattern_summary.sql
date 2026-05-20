CREATE TABLE IF NOT EXISTS dialogue_error_pattern_summary (
    id                 UUID PRIMARY KEY,
    child_id           UUID         NOT NULL,
    theme              VARCHAR(100) NOT NULL,
    analyzed_turn_count INTEGER     NOT NULL DEFAULT 0,
    lecturing_rate     FLOAT        NOT NULL DEFAULT 0,
    criticism_rate     FLOAT        NOT NULL DEFAULT 0,
    topic_ignore_rate  FLOAT        NOT NULL DEFAULT 0,
    rejection_rate     FLOAT        NOT NULL DEFAULT 0,
    unclassified_rate  FLOAT        NOT NULL DEFAULT 0,
    reliability_rate   FLOAT        NOT NULL DEFAULT 0,
    model_name         VARCHAR(100),
    last_refreshed_at  TIMESTAMP,
    updated_at         TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_dialogue_error_pattern_child_theme UNIQUE (child_id, theme),
    CONSTRAINT fk_dialogue_error_pattern_child FOREIGN KEY (child_id) REFERENCES children(child_id)
);
