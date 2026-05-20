CREATE TABLE IF NOT EXISTS game_player_selections (
    selection_id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    child_id UUID NOT NULL,
    selected_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_game_player_selections_user UNIQUE (user_id),
    CONSTRAINT fk_game_player_selections_user FOREIGN KEY (user_id) REFERENCES users(user_id),
    CONSTRAINT fk_game_player_selections_child FOREIGN KEY (child_id) REFERENCES children(child_id)
);

CREATE INDEX IF NOT EXISTS idx_game_player_selections_child ON game_player_selections(child_id);