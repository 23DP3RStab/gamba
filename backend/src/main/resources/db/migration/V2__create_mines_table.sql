CREATE TABLE mines_games (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    bet_amount NUMERIC(15, 2) NOT NULL,
    mine_count INT NOT NULL,
    mine_positions VARCHAR(150) NOT NULL,
    revealed_tiles VARCHAR(150) NOT NULL DEFAULT '',
    current_multiplier NUMERIC(10, 4) NOT NULL DEFAULT 1.0000,
    payout NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_mines_user_status ON mines_games(user_id, status);