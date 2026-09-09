-- NULL lead time means the event sends no reminder at all; there is no global default.
ALTER TABLE events
    ADD COLUMN reminder_lead_time_minutes INTEGER;

ALTER TABLE event_registrations
    ADD COLUMN reminder_sent_at TIMESTAMPTZ;
