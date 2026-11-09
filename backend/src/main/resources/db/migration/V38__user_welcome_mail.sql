-- When the welcome mail went out. Accounts that exist before this column was added never get one: they are
-- not new, so they are marked as already welcomed.
ALTER TABLE users ADD COLUMN welcome_mail_sent_at TIMESTAMPTZ;

UPDATE users SET welcome_mail_sent_at = created_at;
