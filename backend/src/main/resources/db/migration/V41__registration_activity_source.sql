-- Only sign-ups carry a source; cancellations and rows recorded before this column have none.
ALTER TABLE event_registration_activities ADD COLUMN source VARCHAR(16);
