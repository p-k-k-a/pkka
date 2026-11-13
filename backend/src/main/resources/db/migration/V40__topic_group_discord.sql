ALTER TABLE topic_groups
    ADD COLUMN discord_role_id    VARCHAR(32),
    ADD COLUMN discord_channel_id VARCHAR(32);

ALTER TABLE discord_settings
    ADD COLUMN groups_category_id VARCHAR(32);
