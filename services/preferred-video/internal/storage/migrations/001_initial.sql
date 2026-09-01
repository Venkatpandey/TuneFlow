CREATE TABLE IF NOT EXISTS preferred_videos (
    track_id TEXT PRIMARY KEY,
    provider TEXT NOT NULL CHECK (provider = 'youtube'),
    video_id TEXT NOT NULL,
    title TEXT NOT NULL,
    publisher TEXT NOT NULL,
    thumbnail_url TEXT,
    duration_ms INTEGER NOT NULL CHECK (duration_ms >= 0),
    view_count INTEGER NOT NULL CHECK (view_count >= 0),
    mapping_updated_at TEXT NOT NULL,
    last_played_at TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS preferred_videos_recent_idx
    ON preferred_videos(last_played_at DESC, track_id ASC);
