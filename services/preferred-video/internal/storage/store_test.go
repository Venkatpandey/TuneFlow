package storage

import (
	"context"
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

	first, err := store.Put(context.Background(), "track-1", videoInput("aaaaaaaaaaa"))
	if err != nil {
		t.Fatalf("put first mapping: %v", err)
	}
	store.now = func() time.Time { return secondTime }
	secondInput := videoInput("bbbbbbbbbbb")
	secondInput.Title = "Replacement"
	second, err := store.Put(context.Background(), "track-1", secondInput)
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
		if _, err := store.Put(context.Background(), trackID, videoInput([]string{"aaaaaaaaaaa", "bbbbbbbbbbb", "ccccccccccc"}[index])); err != nil {
			t.Fatalf("put %s: %v", trackID, err)
		}
	}
	store.now = func() time.Time { return base.Add(10 * time.Minute) }
	if _, err := store.MarkPlayed(context.Background(), "track-1"); err != nil {
		t.Fatalf("mark played: %v", err)
	}

	videos, err := store.Recent(context.Background(), 2)
	if err != nil {
		t.Fatalf("recent: %v", err)
	}
	if len(videos) != 2 || videos[0].TrackID != "track-1" || videos[1].TrackID != "track-3" {
		t.Fatalf("unexpected recent order: %+v", videos)
	}
}

func TestReopenKeepsDataAndDoesNotReapplyMigration(t *testing.T) {
	databasePath := filepath.Join(t.TempDir(), "videos.db")
	first := openTestStore(t, databasePath)
	if _, err := first.Put(context.Background(), "track-1", videoInput("aaaaaaaaaaa")); err != nil {
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
	if migrationCount != 1 {
		t.Fatalf("migration count = %d, want 1", migrationCount)
	}
	var tableCount int
	if err := second.db.QueryRow("SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = 'preferred_videos'").Scan(&tableCount); err != nil {
		t.Fatalf("count preferred_videos tables: %v", err)
	}
	if tableCount != 1 {
		t.Fatalf("preferred_videos table count = %d, want 1", tableCount)
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
