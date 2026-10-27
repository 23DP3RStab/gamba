CREATE TABLE roulette_spins (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    total_bet NUMERIC(15, 2) NOT NULL,
    payout NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    winning_number INT NOT NULL,
    bets VARCHAR(8000) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE piggies_games (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    bet_amount NUMERIC(15, 2) NOT NULL,
    houses_down INT NOT NULL DEFAULT 0,
    payout NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_piggies_user_status ON piggies_games(user_id, status);

-- Stats, history and the daily loss limit read each player's rounds by date
CREATE INDEX idx_roulette_user_created ON roulette_spins(user_id, created_at);
CREATE INDEX idx_piggies_user_created ON piggies_games(user_id, created_at);
CREATE INDEX idx_mines_user_created ON mines_games(user_id, created_at);
