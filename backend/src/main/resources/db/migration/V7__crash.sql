-- Crash: one shared round at a time. The crash point is drawn when the round is created and is
-- only shown to players once the round has crashed.
CREATE TABLE crash_rounds (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    crash_point NUMERIC(10, 2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'BETTING',
    started_at TIMESTAMP WITH TIME ZONE,
    crashed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE crash_bets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    round_id UUID NOT NULL REFERENCES crash_rounds(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    bet_amount NUMERIC(15, 2) NOT NULL,
    auto_cashout NUMERIC(10, 2),
    cashout_multiplier NUMERIC(10, 2),
    payout NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    -- One bet per player per round
    UNIQUE (round_id, user_id)
);

CREATE INDEX idx_crash_rounds_status_created ON crash_rounds(status, created_at);
CREATE INDEX idx_crash_bets_user_created ON crash_bets(user_id, created_at);
