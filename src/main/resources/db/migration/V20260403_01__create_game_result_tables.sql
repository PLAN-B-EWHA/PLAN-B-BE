-- dialogue_sessions
CREATE TABLE IF NOT EXISTS dialogue_sessions (
    session_id   UUID         PRIMARY KEY,
    child_id     UUID         NOT NULL,
    scenario_id  VARCHAR(50)  NOT NULL,
    theme        VARCHAR(100) NOT NULL,
    total_score  INTEGER      NOT NULL,
    max_score    INTEGER      NOT NULL,
    score_rate   FLOAT        NOT NULL,
    started_at   TIMESTAMPTZ  NOT NULL,
    ended_at     TIMESTAMPTZ  NOT NULL,
    created_at   TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_dialogue_sessions_child FOREIGN KEY (child_id) REFERENCES children(child_id)
);

CREATE INDEX IF NOT EXISTS idx_dialogue_sessions_child ON dialogue_sessions(child_id);
CREATE INDEX IF NOT EXISTS idx_dialogue_sessions_theme ON dialogue_sessions(theme);

-- dialogue_turns
CREATE TABLE IF NOT EXISTS dialogue_turns (
    turn_id                  UUID        PRIMARY KEY,
    session_id               UUID        NOT NULL,
    child_id                 UUID        NOT NULL,
    turn_number              INTEGER     NOT NULL,
    selected_option_order    INTEGER     NOT NULL,
    selected_score           INTEGER     NOT NULL,
    npc_reaction_expression  VARCHAR(30),
    created_at               TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_dialogue_turns_session FOREIGN KEY (session_id) REFERENCES dialogue_sessions(session_id),
    CONSTRAINT fk_dialogue_turns_child   FOREIGN KEY (child_id)   REFERENCES children(child_id)
);

CREATE INDEX IF NOT EXISTS idx_dialogue_turns_session ON dialogue_turns(session_id);
CREATE INDEX IF NOT EXISTS idx_dialogue_turns_child   ON dialogue_turns(child_id);

-- expression_sessions
CREATE TABLE IF NOT EXISTS expression_sessions (
    session_id     UUID         PRIMARY KEY,
    child_id       UUID         NOT NULL,
    emotion_target VARCHAR(50)  NOT NULL,
    final_accuracy FLOAT        NOT NULL,
    is_success     BOOLEAN      NOT NULL,
    total_tries    INTEGER      NOT NULL,
    started_at     TIMESTAMPTZ  NOT NULL,
    ended_at       TIMESTAMPTZ  NOT NULL,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_expression_sessions_child FOREIGN KEY (child_id) REFERENCES children(child_id)
);

CREATE INDEX IF NOT EXISTS idx_expression_sessions_child   ON expression_sessions(child_id);
CREATE INDEX IF NOT EXISTS idx_expression_sessions_emotion ON expression_sessions(emotion_target);

-- expression_tries
CREATE TABLE IF NOT EXISTS expression_tries (
    try_id         UUID      PRIMARY KEY,
    session_id     UUID      NOT NULL,
    child_id       UUID      NOT NULL,
    try_number     INTEGER   NOT NULL,
    accuracy_score FLOAT     NOT NULL,
    duration_ms    INTEGER   NOT NULL,
    is_success     BOOLEAN   NOT NULL,
    created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_expression_tries_session FOREIGN KEY (session_id) REFERENCES expression_sessions(session_id),
    CONSTRAINT fk_expression_tries_child   FOREIGN KEY (child_id)   REFERENCES children(child_id)
);

CREATE INDEX IF NOT EXISTS idx_expression_tries_session ON expression_tries(session_id);
CREATE INDEX IF NOT EXISTS idx_expression_tries_child   ON expression_tries(child_id);
