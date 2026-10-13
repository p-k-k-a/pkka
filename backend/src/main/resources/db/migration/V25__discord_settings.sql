CREATE TABLE discord_settings (
    guild_id                 VARCHAR(32) PRIMARY KEY,
    announcements_channel_id VARCHAR(32),
    alumn_role_id            VARCHAR(32)
);

-- At most one row: the bot serves a single server.
CREATE UNIQUE INDEX discord_settings_one_guild ON discord_settings ((true));
