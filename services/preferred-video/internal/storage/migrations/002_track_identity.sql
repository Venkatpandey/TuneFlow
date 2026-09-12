ALTER TABLE preferred_videos ADD COLUMN track_identity_key TEXT;
ALTER TABLE preferred_videos ADD COLUMN track_title TEXT;
ALTER TABLE preferred_videos ADD COLUMN track_artist TEXT;
ALTER TABLE preferred_videos ADD COLUMN track_duration_ms INTEGER CHECK (track_duration_ms IS NULL OR track_duration_ms > 0);

CREATE INDEX IF NOT EXISTS preferred_videos_identity_idx
    ON preferred_videos(track_identity_key, track_duration_ms, mapping_updated_at DESC);
