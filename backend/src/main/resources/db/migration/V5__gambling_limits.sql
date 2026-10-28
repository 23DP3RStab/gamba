-- Responsible gambling settings; empty means no limit / not locked out.
-- A raised or removed loss limit waits in pending_* until pending_limit_from.
ALTER TABLE users
    ADD COLUMN daily_loss_limit NUMERIC(15, 2),
    ADD COLUMN pending_loss_limit NUMERIC(15, 2),
    ADD COLUMN pending_limit_from TIMESTAMP WITH TIME ZONE,
    ADD COLUMN locked_until TIMESTAMP WITH TIME ZONE;
