CREATE TABLE IF NOT EXISTS expression_stat_summary (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    child_id        UUID NOT NULL,
    emotion_target  VARCHAR(50) NOT NULL,
    success_rate    FLOAT NOT NULL DEFAULT 0,
    fluency_index   FLOAT NOT NULL DEFAULT 0,
    avg_retry       FLOAT NOT NULL DEFAULT 0,
    session_count   INTEGER NOT NULL DEFAULT 0,
    duration_decrease_rate FLOAT NOT NULL DEFAULT 0,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_expression_stat_child_emotion UNIQUE (child_id, emotion_target),
    CONSTRAINT fk_expression_stat_child FOREIGN KEY (child_id) REFERENCES children(child_id)
);

CREATE TABLE IF NOT EXISTS dialogue_stat_summary (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    child_id        UUID NOT NULL,
    theme           VARCHAR(100) NOT NULL,
    score_rate      FLOAT NOT NULL DEFAULT 0,
    rapport_index   FLOAT NOT NULL DEFAULT 0,
    turn_fatigue    FLOAT NOT NULL DEFAULT 0,
    score0_rate     FLOAT NOT NULL DEFAULT 0,
    score1_rate     FLOAT NOT NULL DEFAULT 0,
    score2_rate     FLOAT NOT NULL DEFAULT 0,
    session_count   INTEGER NOT NULL DEFAULT 0,
    updated_at      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_dialogue_stat_child_theme UNIQUE (child_id, theme),
    CONSTRAINT fk_dialogue_stat_child FOREIGN KEY (child_id) REFERENCES children(child_id)
);
