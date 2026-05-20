ALTER TABLE dialogue_sessions
    ADD COLUMN IF NOT EXISTS scenario_source VARCHAR(30) NOT NULL DEFAULT 'UNITY_LOCAL';

CREATE INDEX IF NOT EXISTS idx_dialogue_sessions_scenario_source
    ON dialogue_sessions(scenario_source);
