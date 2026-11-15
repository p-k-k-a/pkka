-- Who may see a material that has no linked event (a linked event decides instead). Materials were only
-- reachable by verified alumni so far, so that is what the existing ones keep.
ALTER TABLE materials
    ADD COLUMN audience VARCHAR(32) NOT NULL DEFAULT 'ALL_ALUMNI';
