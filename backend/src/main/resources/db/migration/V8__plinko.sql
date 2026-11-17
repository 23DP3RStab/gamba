-- Plinko: one row per ball dropped. The server draws the 12 bounces; `path` keeps them as L and R
-- (top to bottom) so a drop can be replayed exactly as it was animated.
CREATE TABLE plinko_drops (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    bet_amount NUMERIC(15, 2) NOT NULL,
    risk VARCHAR(10) NOT NULL,
    path VARCHAR(20) NOT NULL,
    slot INT NOT NULL,
    multiplier NUMERIC(10, 2) NOT NULL,
    payout NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_plinko_user_created ON plinko_drops(user_id, created_at);
