-- Profile details the user can fill in on the profile page; empty until they do
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS first_name VARCHAR(30),
    ADD COLUMN IF NOT EXISTS last_name VARCHAR(30),
    ADD COLUMN IF NOT EXISTS birth_date DATE,
    ADD COLUMN IF NOT EXISTS phone VARCHAR(8);
