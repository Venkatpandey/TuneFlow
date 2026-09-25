package storage

import (
	"context"
	"database/sql"
	"errors"
	"path/filepath"
	"testing"
	"time"

	"github.com/Venkatpandey/TuneFlow/services/preferred-video/internal/model"
)

func TestPutReplacesMappingAndRefreshesBothTimestamps(t *testing.T) {
	store := openTestStore(t, filepath.Join(t.TempDir(), "videos.db"))
	firstTime := time.Date(2026, time.August, 30, 10, 0, 0, 0, time.UTC)
	secondTime := firstTime.Add(time.Hour)
	store.now = func() time.Time { return firstTime }

	first, err := store.Put(context.Background(), "track-1", videoInput("aaaaaaaaaaa"), nil)
	if err != nil {
		t.Fatalf("put first mapping: %v", err)
	}
	store.now = func() time.Time { return secondTime }
	secondInput := videoInput("bbbbbbbbbbb")
	secondInput.Title = "Replacement"
	second, err := store.Put(context.Background(), "track-1", secondInput, nil)
	if err != nil {
		t.Fatalf("replace mapping: %v", err)
	}

	if first.VideoID != "aaaaaaaaaaa" || second.VideoID != "bbbbbbbbbbb" {
		t.Fatalf("unexpected replacement values: first=%s second=%s", first.VideoID, second.VideoID)
	}
	if !second.MappingUpdatedAt.Equal(secondTime) || !second.LastPlayedAt.Equal(secondTime) {
		t.Fatalf("replacement timestamps were not refreshed: %+v", second)
	}
}

func TestGetAndDeleteMissingReturnNotFound(t *testing.T) {
	store := openTestStore(t, filepath.Join(t.TempDir(), "videos.db"))

	if _, err := store.Get(context.Background(), "missing"); !errors.Is(err, ErrNotFound) {
		t.Fatalf("get missing error = %v, want ErrNotFound", err)
	}
	if err := store.Delete(context.Background(), "missing"); !errors.Is(err, ErrNotFound) {
		t.Fatalf("delete missing error = %v, want ErrNotFound", err)
	}
	if _, err := store.MarkPlayed(context.Background(), "missing"); !errors.Is(err, ErrNotFound) {
		t.Fatalf("mark missing error = %v, want ErrNotFound", err)
	}
}

func TestRecentOrdersByPlaybackAndHonorsLimit(t *testing.T) {
	store := openTestStore(t, filepath.Join(t.TempDir(), "videos.db"))
	base := time.Date(2026, time.August, 30, 10, 0, 0, 0, time.UTC)
	for index, trackID := range []string{"track-1", "track-2", "track-3"} {
		store.now = func() time.Time { return base.Add(time.Duration(index) * time.Minute) }
		if _, err := store.Put(context.Background(), trackID, videoInput([]string{"aaaaaaaaaaa", "bbbbbbbbbbb", "ccccccccccc"}[index]), nil); err != nil {
			t.Fatalf("put %s: %v", trackID, err)
		}
	}
	store.now = func() time.Time { return base.Add(10 * time.Minute) }
	if _, err := store.MarkPlayed(context.Background(), "track-1"); err != nil {
		t.Fatalf("mark played: %v", err)
	}

	videos, err := store.Recent(context.Background(), 2, 0)
	if err != nil {
		t.Fatalf("recent: %v", err)
	}
	if len(videos) != 2 || videos[0].TrackID != "track-1" || videos[1].TrackID != "track-3" {
		t.Fatalf("unexpected recent order: %+v", videos)
	}
}

func TestRecentDeduplicatesByVideoIDAndSupportsUnlimited(t *testing.T) {
	store := openTestStore(t, filepath.Join(t.TempDir(), "videos.db"))
	base := time.Date(2026, time.August, 30, 10, 0, 0, 0, time.UTC)
	// track-1 and track-2 share the same videoID "aaaaaaaaaaa"
	store.now = func() time.Time { return base }
	if _, err := store.Put(context.Background(), "track-1", videoInput("aaaaaaaaaaa"), nil); err != nil {
		t.Fatalf("put track-1: %v", err)
	}
	store.now = func() time.Time { return base.Add(time.Minute) }
	if _, err := store.Put(context.Background(), "track-2", videoInput("aaaaaaaaaaa"), nil); err != nil {
		t.Fatalf("put track-2: %v", err)
	}
	store.now = func() time.Time { return base.Add(2 * time.Minute) }
	if _, err := store.Put(context.Background(), "track-3", videoInput("bbbbbbbbbbb"), nil); err != nil {
		t.Fatalf("put track-3: %v", err)
	}

	// Limit 0 should return all unique videos deduplicated by video_id
	videos, err := store.Recent(context.Background(), 0, 0)
	if err != nil {
		t.Fatalf("recent: %v", err)
	}
	if len(videos) != 2 {
		t.Fatalf("recent count = %d, want 2 unique videos", len(videos))
	}
	if videos[0].VideoID != "bbbbbbbbbbb" || videos[1].VideoID != "aaaaaaaaaaa" {
		t.Fatalf("unexpected unique recent videos: %+v", videos)
	}
}

func TestResolveKeepsDirectMappingEvenIfIdentityMismatch(t *testing.T) {
	store := openTestStore(t, filepath.Join(t.TempDir(), "videos.db"))
	if _, err := store.Put(
		context.Background(),
		"track-1",
		videoInput("aaaaaaaaaaa"),
		trackIdentity("Song", "Artist", 180_000),
	); err != nil {
		t.Fatalf("put track-1: %v", err)
	}

	// When track-1 is resolved with a different duration (e.g. 260_000), it should still return the existing mapping!
	resolved, err := store.Resolve(
		context.Background(),
		"track-1",
		trackIdentity("Song", "Artist", 260_000),
	)
	if err != nil {
		t.Fatalf("resolve track-1 error = %v, want success", err)
	}
	if resolved.VideoID != "aaaaaaaaaaa" {
		t.Fatalf("resolved videoID = %s, want aaaaaaaaaaa", resolved.VideoID)
	}
}

func TestReopenKeepsDataAndDoesNotReapplyMigration(t *testing.T) {
	databasePath := filepath.Join(t.TempDir(), "videos.db")
	first := openTestStore(t, databasePath)
	if _, err := first.Put(context.Background(), "track-1", videoInput("aaaaaaaaaaa"), nil); err != nil {
		t.Fatalf("put mapping: %v", err)
	}
	if err := first.Close(); err != nil {
		t.Fatalf("close first store: %v", err)
	}

	second, err := Open(context.Background(), databasePath)
	if err != nil {
		t.Fatalf("reopen store: %v", err)
	}
	t.Cleanup(func() { second.Close() })
	video, err := second.Get(context.Background(), "track-1")
	if err != nil {
		t.Fatalf("get persisted mapping: %v", err)
	}
	if video.VideoID != "aaaaaaaaaaa" {
		t.Fatalf("persisted video ID = %s", video.VideoID)
	}

	var migrationCount int
	if err := second.db.QueryRow("SELECT COUNT(*) FROM schema_migrations").Scan(&migrationCount); err != nil {
		t.Fatalf("count migrations: %v", err)
	}
	if migrationCount != 2 {
		t.Fatalf("migration count = %d, want 2", migrationCount)
	}
	var tableCount int
	if err := second.db.QueryRow("SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = 'preferred_videos'").Scan(&tableCount); err != nil {
		t.Fatalf("count preferred_videos tables: %v", err)
	}
	if tableCount != 1 {
		t.Fatalf("preferred_videos table count = %d, want 1", tableCount)
	}
}

func TestUpgradeAddsTrackIdentityWithoutLosingLegacyMapping(t *testing.T) {
	databasePath := filepath.Join(t.TempDir(), "videos.db")
	db, err := sql.Open("sqlite", databasePath)
	if err != nil {
		t.Fatalf("open legacy database: %v", err)
	}
	if _, err := db.Exec(`CREATE TABLE schema_migrations (name TEXT PRIMARY KEY, applied_at TEXT NOT NULL)`); err != nil {
		t.Fatalf("create legacy migration table: %v", err)
	}
	initialMigration, err := migrationFiles.ReadFile("migrations/001_initial.sql")
	if err != nil {
		t.Fatalf("read initial migration: %v", err)
	}
	if _, err := db.Exec(string(initialMigration)); err != nil {
		t.Fatalf("apply initial migration: %v", err)
	}
	if _, err := db.Exec(`INSERT INTO schema_migrations(name, applied_at) VALUES ('migrations/001_initial.sql', '2026-09-01T00:00:00Z')`); err != nil {
		t.Fatalf("record initial migration: %v", err)
	}
	if _, err := db.Exec(`
		INSERT INTO preferred_videos (
			track_id, provider, video_id, title, publisher, duration_ms, view_count,
			mapping_updated_at, last_played_at
		) VALUES ('legacy-id', 'youtube', 'aaaaaaaaaaa', 'Video', 'Artist', 180000, 42,
		          '2026-09-01T00:00:00Z', '2026-09-01T00:00:00Z')`); err != nil {
		t.Fatalf("insert legacy mapping: %v", err)
	}
	if err := db.Close(); err != nil {
		t.Fatalf("close legacy database: %v", err)
	}

	store := openTestStore(t, databasePath)
	identity := trackIdentity("Song", "Artist", 180_000)
	if _, err := store.Resolve(context.Background(), "legacy-id", identity); err != nil {
		t.Fatalf("resolve upgraded legacy mapping: %v", err)
	}
	duplicate, err := store.Resolve(context.Background(), "duplicate-id", identity)
	if err != nil {
		t.Fatalf("resolve duplicate after upgrade: %v", err)
	}
	if duplicate.VideoID != "aaaaaaaaaaa" {
		t.Fatalf("duplicate video ID = %s, want legacy mapping", duplicate.VideoID)
	}
}

func TestResolveReusesMappingForDuplicateTrackIdentity(t *testing.T) {
	store := openTestStore(t, filepath.Join(t.TempDir(), "videos.db"))
	identity := trackIdentity("  Makeba ", "JAIN", 219_000)
	if _, err := store.Put(context.Background(), "original-id", videoInput("aaaaaaaaaaa"), nil); err != nil {
		t.Fatalf("put original mapping: %v", err)
	}
	if _, err := store.Resolve(context.Background(), "original-id", identity); err != nil {
		t.Fatalf("backfill original identity: %v", err)
	}

	resolved, err := store.Resolve(
		context.Background(),
		"duplicate-id",
		trackIdentity("makeba", "  Jain  ", 224_000),
	)
	if err != nil {
		t.Fatalf("resolve duplicate mapping: %v", err)
	}
	if resolved.TrackID != "duplicate-id" || resolved.VideoID != "aaaaaaaaaaa" {
		t.Fatalf("unexpected resolved mapping: %+v", resolved)
	}
	if _, err := store.MarkPlayed(context.Background(), "duplicate-id"); err != nil {
		t.Fatalf("resolved alias was not persisted: %v", err)
	}
}

func TestResolveDoesNotReuseDifferentDuration(t *testing.T) {
	store := openTestStore(t, filepath.Join(t.TempDir(), "videos.db"))
	if _, err := store.Put(
		context.Background(),
		"studio-id",
		videoInput("aaaaaaaaaaa"),
		trackIdentity("Song", "Artist", 180_000),
	); err != nil {
		t.Fatalf("put studio mapping: %v", err)
	}

	_, err := store.Resolve(
		context.Background(),
		"live-id",
		trackIdentity("Song", "Artist", 240_000),
	)
	if !errors.Is(err, ErrNotFound) {
		t.Fatalf("resolve different duration error = %v, want ErrNotFound", err)
	}
}

func TestReplacingDuplicateMappingUpdatesIdentityGroup(t *testing.T) {
	store := openTestStore(t, filepath.Join(t.TempDir(), "videos.db"))
	identity := trackIdentity("Song", "Artist", 180_000)
	if _, err := store.Put(context.Background(), "track-1", videoInput("aaaaaaaaaaa"), identity); err != nil {
		t.Fatalf("put original mapping: %v", err)
	}
	if _, err := store.Resolve(context.Background(), "track-2", identity); err != nil {
		t.Fatalf("resolve duplicate mapping: %v", err)
	}
	if _, err := store.Put(context.Background(), "track-2", videoInput("bbbbbbbbbbb"), identity); err != nil {
		t.Fatalf("replace duplicate mapping: %v", err)
	}

	original, err := store.Get(context.Background(), "track-1")
	if err != nil {
		t.Fatalf("get original mapping: %v", err)
	}
	if original.VideoID != "bbbbbbbbbbb" {
		t.Fatalf("original video ID = %s, want propagated replacement", original.VideoID)
	}
}

func TestResolveReconcilesLegacyMappingWithNewestIdentityChoice(t *testing.T) {
	store := openTestStore(t, filepath.Join(t.TempDir(), "videos.db"))
	firstTime := time.Date(2026, time.September, 1, 10, 0, 0, 0, time.UTC)
	store.now = func() time.Time { return firstTime }
	if _, err := store.Put(context.Background(), "legacy-id", videoInput("aaaaaaaaaaa"), nil); err != nil {
		t.Fatalf("put legacy mapping: %v", err)
	}

	identity := trackIdentity("Song", "Artist", 180_000)
	store.now = func() time.Time { return firstTime.Add(time.Hour) }
	if _, err := store.Put(context.Background(), "duplicate-id", videoInput("bbbbbbbbbbb"), identity); err != nil {
		t.Fatalf("put newer duplicate mapping: %v", err)
	}

	resolved, err := store.Resolve(context.Background(), "legacy-id", identity)
	if err != nil {
		t.Fatalf("resolve legacy identity: %v", err)
	}
	if resolved.VideoID != "bbbbbbbbbbb" {
		t.Fatalf("resolved video ID = %s, want newest identity mapping", resolved.VideoID)
	}
}

func TestDeleteRemovesDuplicateIdentityGroup(t *testing.T) {
	store := openTestStore(t, filepath.Join(t.TempDir(), "videos.db"))
	identity := trackIdentity("Song", "Artist", 180_000)
	if _, err := store.Put(context.Background(), "track-1", videoInput("aaaaaaaaaaa"), identity); err != nil {
		t.Fatalf("put original mapping: %v", err)
	}
	if _, err := store.Resolve(context.Background(), "track-2", identity); err != nil {
		t.Fatalf("resolve duplicate mapping: %v", err)
	}

	if err := store.Delete(context.Background(), "track-2"); err != nil {
		t.Fatalf("delete duplicate mapping: %v", err)
	}
	if _, err := store.Get(context.Background(), "track-1"); !errors.Is(err, ErrNotFound) {
		t.Fatalf("original mapping remains after group delete: %v", err)
	}
}

func openTestStore(t *testing.T, databasePath string) *Store {
	t.Helper()
	store, err := Open(context.Background(), databasePath)
	if err != nil {
		t.Fatalf("open store: %v", err)
	}
	t.Cleanup(func() { store.Close() })
	return store
}

func videoInput(videoID string) model.UpsertPreferredVideo {
	thumbnail := "https://i.ytimg.com/vi/" + videoID + "/hqdefault.jpg"
	return model.UpsertPreferredVideo{
		Provider:     model.ProviderYouTube,
		VideoID:      videoID,
		Title:        "Video " + videoID,
		Publisher:    "Artist",
		ThumbnailURL: &thumbnail,
		DurationMS:   180_000,
		ViewCount:    42,
	}
}

func trackIdentity(title, artist string, durationMS int64) *model.TrackIdentity {
	return &model.TrackIdentity{Title: title, Artist: artist, DurationMS: durationMS}
}

func TestRecentPagesAfterDeduplication(t *testing.T) {
	store := openTestStore(t, filepath.Join(t.TempDir(), "videos.db"))
	store.now = func() time.Time { return time.Date(2026, time.September, 21, 10, 0, 0, 0, time.UTC) }
	for _, item := range []struct{ trackID, videoID string }{
		{"track-1", "aaaaaaaaaaa"},
		{"track-2", "aaaaaaaaaaa"},
		{"track-3", "bbbbbbbbbbb"},
		{"track-4", "ccccccccccc"},
	} {
		if _, err := store.Put(context.Background(), item.trackID, videoInput(item.videoID), nil); err != nil {
			t.Fatal(err)
		}
	}
	first, err := store.Recent(context.Background(), 2, 0)
	if err != nil {
		t.Fatal(err)
	}
	second, err := store.Recent(context.Background(), 2, 2)
	if err != nil {
		t.Fatal(err)
	}
	if len(first) != 2 || len(second) != 1 || first[0].VideoID != "aaaaaaaaaaa" || first[1].VideoID != "bbbbbbbbbbb" || second[0].VideoID != "ccccccccccc" {
		t.Fatalf("unexpected pages: %+v / %+v", first, second)
	}
	end, err := store.Recent(context.Background(), 2, 3)
	if err != nil || len(end) != 0 {
		t.Fatalf("end = %+v, error = %v", end, err)
	}
}
