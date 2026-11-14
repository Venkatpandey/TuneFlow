package storage

import (
	"context"
	"crypto/sha256"
	"database/sql"
	"embed"
	"encoding/hex"
	"errors"
	"fmt"
	"io/fs"
	"os"
	"path/filepath"
	"regexp"
	"sort"
	"strings"
	"time"

	"github.com/Venkatpandey/TuneFlow/services/preferred-video/internal/model"
	_ "modernc.org/sqlite"
)

var ErrNotFound = errors.New("preferred video not found")

const trackDurationToleranceMS int64 = 30_000

//go:embed migrations/*.sql
var migrationFiles embed.FS

type Store struct {
	db  *sql.DB
	now func() time.Time
}

func Open(ctx context.Context, databasePath string) (*Store, error) {
	if strings.TrimSpace(databasePath) == "" {
		return nil, errors.New("database path is required")
	}
	if databasePath != ":memory:" {
		if err := os.MkdirAll(filepath.Dir(databasePath), 0o750); err != nil {
			return nil, fmt.Errorf("create database directory: %w", err)
		}
	}

	db, err := sql.Open("sqlite", databasePath)
	if err != nil {
		return nil, fmt.Errorf("open database: %w", err)
	}
	db.SetMaxOpenConns(1)
	db.SetMaxIdleConns(1)

	store := &Store{db: db, now: time.Now}
	if err := store.configure(ctx); err != nil {
		db.Close()
		return nil, err
	}
	if err := store.migrate(ctx); err != nil {
		db.Close()
		return nil, err
	}
	return store, nil
}

func (s *Store) configure(ctx context.Context) error {
	statements := []string{
		"PRAGMA busy_timeout = 5000",
		"PRAGMA journal_mode = WAL",
		"PRAGMA synchronous = NORMAL",
		"PRAGMA foreign_keys = ON",
	}
	for _, statement := range statements {
		if _, err := s.db.ExecContext(ctx, statement); err != nil {
			return fmt.Errorf("configure database: %w", err)
		}
	}
	return nil
}

func (s *Store) migrate(ctx context.Context) error {
	if _, err := s.db.ExecContext(ctx, `
		CREATE TABLE IF NOT EXISTS schema_migrations (
			name TEXT PRIMARY KEY,
			applied_at TEXT NOT NULL
		)`); err != nil {
		return fmt.Errorf("create migration table: %w", err)
	}

	names, err := fs.Glob(migrationFiles, "migrations/*.sql")
	if err != nil {
		return fmt.Errorf("list migrations: %w", err)
	}
	sort.Strings(names)
	for _, name := range names {
		if err := s.applyMigration(ctx, name); err != nil {
			return err
		}
	}
	return nil
}

func (s *Store) applyMigration(ctx context.Context, name string) error {
	tx, err := s.db.BeginTx(ctx, nil)
	if err != nil {
		return fmt.Errorf("begin migration %s: %w", name, err)
	}
	defer tx.Rollback()

	var applied int
	if err := tx.QueryRowContext(ctx, "SELECT COUNT(*) FROM schema_migrations WHERE name = ?", name).Scan(&applied); err != nil {
		return fmt.Errorf("check migration %s: %w", name, err)
	}
	if applied > 0 {
		return tx.Commit()
	}

	contents, err := migrationFiles.ReadFile(name)
	if err != nil {
		return fmt.Errorf("read migration %s: %w", name, err)
	}
	if _, err := tx.ExecContext(ctx, string(contents)); err != nil {
		return fmt.Errorf("apply migration %s: %w", name, err)
	}
	if _, err := tx.ExecContext(
		ctx,
		"INSERT INTO schema_migrations(name, applied_at) VALUES (?, ?)",
		name,
		time.Now().UTC().Format(time.RFC3339Nano),
	); err != nil {
		return fmt.Errorf("record migration %s: %w", name, err)
	}
	if err := tx.Commit(); err != nil {
		return fmt.Errorf("commit migration %s: %w", name, err)
	}
	return nil
}

func (s *Store) Close() error {
	return s.db.Close()
}

func (s *Store) Health(ctx context.Context) error {
	return s.db.PingContext(ctx)
}

func (s *Store) Get(ctx context.Context, trackID string) (model.PreferredVideo, error) {
	row := s.db.QueryRowContext(ctx, `
		SELECT track_id, provider, video_id, title, publisher, thumbnail_url,
		       duration_ms, view_count, mapping_updated_at, last_played_at
		FROM preferred_videos
		WHERE track_id = ?`, trackID)
	return scanVideo(row)
}

// Resolve first checks the exact Navidrome track ID. When that misses, it
// reuses the newest mapping for the same normalized recording.
func (s *Store) Resolve(
	ctx context.Context,
	trackID string,
	identity *model.TrackIdentity,
) (model.PreferredVideo, error) {
	video, err := s.Get(ctx, trackID)
	identityKey, hasIdentity := canonicalTrackIdentity(identity)
	if err == nil {
		if hasIdentity {
			_ = s.rememberTrackIdentity(ctx, trackID, identityKey, *identity)
			latest, err := s.getByTrackIdentity(ctx, identityKey, identity.DurationMS)
			if err == nil {
				_ = s.synchronizeTrackIdentity(ctx, identityKey, identity.DurationMS, latest)
				if updated, err := s.Get(ctx, trackID); err == nil {
					return updated, nil
				}
			}
		}
		return video, nil
	}
	if !errors.Is(err, ErrNotFound) || !hasIdentity {
		return model.PreferredVideo{}, err
	}

	source, err := s.getByTrackIdentity(ctx, identityKey, identity.DurationMS)
	if err != nil {
		return model.PreferredVideo{}, err
	}
	return s.copyMappingToTrack(ctx, trackID, identityKey, *identity, source)
}

func (s *Store) Put(
	ctx context.Context,
	trackID string,
	input model.UpsertPreferredVideo,
	identity *model.TrackIdentity,
) (model.PreferredVideo, error) {
	now := s.now().UTC().Format(time.RFC3339Nano)
	identityKey, hasIdentity := canonicalTrackIdentity(identity)
	if !hasIdentity {
		_, err := s.db.ExecContext(ctx, `
			INSERT INTO preferred_videos (
				track_id, provider, video_id, title, publisher, thumbnail_url,
			duration_ms, view_count, mapping_updated_at, last_played_at
		) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
		ON CONFLICT(track_id) DO UPDATE SET
			provider = excluded.provider,
			video_id = excluded.video_id,
			title = excluded.title,
			publisher = excluded.publisher,
			thumbnail_url = excluded.thumbnail_url,
			duration_ms = excluded.duration_ms,
			view_count = excluded.view_count,
			mapping_updated_at = excluded.mapping_updated_at,
			last_played_at = excluded.last_played_at`,
			trackID,
			input.Provider,
			input.VideoID,
			input.Title,
			input.Publisher,
			input.ThumbnailURL,
			input.DurationMS,
			input.ViewCount,
			now,
			now,
		)
		if err != nil {
			return model.PreferredVideo{}, fmt.Errorf("put preferred video: %w", err)
		}
		return s.Get(ctx, trackID)
	}

	tx, err := s.db.BeginTx(ctx, nil)
	if err != nil {
		return model.PreferredVideo{}, fmt.Errorf("begin preferred video update: %w", err)
	}
	defer tx.Rollback()

	minimumDuration, maximumDuration := durationRange(identity.DurationMS)
	if _, err := tx.ExecContext(ctx, `
		UPDATE preferred_videos
		SET provider = ?, video_id = ?, title = ?, publisher = ?, thumbnail_url = ?,
		    duration_ms = ?, view_count = ?, mapping_updated_at = ?
		WHERE track_identity_key = ? AND track_duration_ms BETWEEN ? AND ?`,
		input.Provider,
		input.VideoID,
		input.Title,
		input.Publisher,
		input.ThumbnailURL,
		input.DurationMS,
		input.ViewCount,
		now,
		identityKey,
		minimumDuration,
		maximumDuration,
	); err != nil {
		return model.PreferredVideo{}, fmt.Errorf("update matching preferred videos: %w", err)
	}

	if _, err := tx.ExecContext(ctx, `
		INSERT INTO preferred_videos (
			track_id, provider, video_id, title, publisher, thumbnail_url,
			duration_ms, view_count, mapping_updated_at, last_played_at,
			track_identity_key, track_title, track_artist, track_duration_ms
		) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
		ON CONFLICT(track_id) DO UPDATE SET
			provider = excluded.provider,
			video_id = excluded.video_id,
			title = excluded.title,
			publisher = excluded.publisher,
			thumbnail_url = excluded.thumbnail_url,
			duration_ms = excluded.duration_ms,
			view_count = excluded.view_count,
			mapping_updated_at = excluded.mapping_updated_at,
			last_played_at = excluded.last_played_at,
			track_identity_key = excluded.track_identity_key,
			track_title = excluded.track_title,
			track_artist = excluded.track_artist,
			track_duration_ms = excluded.track_duration_ms`,
		trackID,
		input.Provider,
		input.VideoID,
		input.Title,
		input.Publisher,
		input.ThumbnailURL,
		input.DurationMS,
		input.ViewCount,
		now,
		now,
		identityKey,
		identity.Title,
		identity.Artist,
		identity.DurationMS,
	); err != nil {
		return model.PreferredVideo{}, fmt.Errorf("put preferred video: %w", err)
	}
	if err := tx.Commit(); err != nil {
		return model.PreferredVideo{}, fmt.Errorf("commit preferred video update: %w", err)
	}
	return s.Get(ctx, trackID)
}

func (s *Store) Delete(ctx context.Context, trackID string) error {
	var identityKey sql.NullString
	var durationMS sql.NullInt64
	err := s.db.QueryRowContext(
		ctx,
		"SELECT track_identity_key, track_duration_ms FROM preferred_videos WHERE track_id = ?",
		trackID,
	).Scan(&identityKey, &durationMS)
	if errors.Is(err, sql.ErrNoRows) {
		return ErrNotFound
	}
	if err != nil {
		return fmt.Errorf("read preferred video identity: %w", err)
	}

	var result sql.Result
	if identityKey.Valid && durationMS.Valid {
		minimumDuration, maximumDuration := durationRange(durationMS.Int64)
		result, err = s.db.ExecContext(
			ctx,
			"DELETE FROM preferred_videos WHERE track_identity_key = ? AND track_duration_ms BETWEEN ? AND ?",
			identityKey.String,
			minimumDuration,
			maximumDuration,
		)
	} else {
		result, err = s.db.ExecContext(ctx, "DELETE FROM preferred_videos WHERE track_id = ?", trackID)
	}
	if err != nil {
		return fmt.Errorf("delete preferred video: %w", err)
	}
	affected, err := result.RowsAffected()
	if err != nil {
		return fmt.Errorf("read deleted row count: %w", err)
	}
	if affected == 0 {
		return ErrNotFound
	}
	return nil
}

func (s *Store) MarkPlayed(ctx context.Context, trackID string) (model.PreferredVideo, error) {
	result, err := s.db.ExecContext(
		ctx,
		"UPDATE preferred_videos SET last_played_at = ? WHERE track_id = ?",
		s.now().UTC().Format(time.RFC3339Nano),
		trackID,
	)
	if err != nil {
		return model.PreferredVideo{}, fmt.Errorf("mark preferred video played: %w", err)
	}
	affected, err := result.RowsAffected()
	if err != nil {
		return model.PreferredVideo{}, fmt.Errorf("read updated row count: %w", err)
	}
	if affected == 0 {
		return model.PreferredVideo{}, ErrNotFound
	}
	return s.Get(ctx, trackID)
}

func (s *Store) Recent(ctx context.Context, limit, offset int) ([]model.PreferredVideo, error) {
	query := `
		SELECT track_id, provider, video_id, title, publisher, thumbnail_url,
		       duration_ms, view_count, mapping_updated_at, MAX(last_played_at) AS last_played_at
		FROM preferred_videos
		GROUP BY video_id
		ORDER BY MAX(last_played_at) DESC, video_id ASC`
	var rows *sql.Rows
	var err error
	if limit <= 0 {
		limit = -1
	}
	query += " LIMIT ? OFFSET ?"
	rows, err = s.db.QueryContext(ctx, query, limit, offset)
	if err != nil {
		return nil, fmt.Errorf("query recent videos: %w", err)
	}
	defer rows.Close()

	videos := make([]model.PreferredVideo, 0)
	for rows.Next() {
		video, err := scanVideo(rows)
		if err != nil {
			return nil, err
		}
		videos = append(videos, video)
	}
	if err := rows.Err(); err != nil {
		return nil, fmt.Errorf("iterate recent videos: %w", err)
	}
	return videos, nil
}

func (s *Store) rememberTrackIdentity(
	ctx context.Context,
	trackID string,
	identityKey string,
	identity model.TrackIdentity,
) error {
	_, err := s.db.ExecContext(ctx, `
		UPDATE preferred_videos
		SET track_identity_key = ?, track_title = ?, track_artist = ?, track_duration_ms = ?
		WHERE track_id = ?`,
		identityKey,
		identity.Title,
		identity.Artist,
		identity.DurationMS,
		trackID,
	)
	if err != nil {
		return fmt.Errorf("remember preferred video track identity: %w", err)
	}
	return nil
}

func (s *Store) getByTrackIdentity(
	ctx context.Context,
	identityKey string,
	durationMS int64,
) (model.PreferredVideo, error) {
	minimumDuration, maximumDuration := durationRange(durationMS)
	row := s.db.QueryRowContext(ctx, `
		SELECT track_id, provider, video_id, title, publisher, thumbnail_url,
		       duration_ms, view_count, mapping_updated_at, last_played_at
		FROM preferred_videos
		WHERE track_identity_key = ? AND track_duration_ms BETWEEN ? AND ?
		ORDER BY mapping_updated_at DESC, last_played_at DESC, track_id ASC
		LIMIT 1`, identityKey, minimumDuration, maximumDuration)
	return scanVideo(row)
}

func (s *Store) synchronizeTrackIdentity(
	ctx context.Context,
	identityKey string,
	durationMS int64,
	source model.PreferredVideo,
) error {
	minimumDuration, maximumDuration := durationRange(durationMS)
	_, err := s.db.ExecContext(ctx, `
		UPDATE preferred_videos
		SET provider = ?, video_id = ?, title = ?, publisher = ?, thumbnail_url = ?,
		    duration_ms = ?, view_count = ?, mapping_updated_at = ?
		WHERE track_identity_key = ? AND track_duration_ms BETWEEN ? AND ?`,
		source.Provider,
		source.VideoID,
		source.Title,
		source.Publisher,
		source.ThumbnailURL,
		source.DurationMS,
		source.ViewCount,
		source.MappingUpdatedAt.UTC().Format(time.RFC3339Nano),
		identityKey,
		minimumDuration,
		maximumDuration,
	)
	if err != nil {
		return fmt.Errorf("synchronize preferred video track identity: %w", err)
	}
	return nil
}

func (s *Store) copyMappingToTrack(
	ctx context.Context,
	trackID string,
	identityKey string,
	identity model.TrackIdentity,
	source model.PreferredVideo,
) (model.PreferredVideo, error) {
	_, err := s.db.ExecContext(ctx, `
		INSERT INTO preferred_videos (
			track_id, provider, video_id, title, publisher, thumbnail_url,
			duration_ms, view_count, mapping_updated_at, last_played_at,
			track_identity_key, track_title, track_artist, track_duration_ms
		) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
		ON CONFLICT(track_id) DO NOTHING`,
		trackID,
		source.Provider,
		source.VideoID,
		source.Title,
		source.Publisher,
		source.ThumbnailURL,
		source.DurationMS,
		source.ViewCount,
		source.MappingUpdatedAt.UTC().Format(time.RFC3339Nano),
		source.LastPlayedAt.UTC().Format(time.RFC3339Nano),
		identityKey,
		identity.Title,
		identity.Artist,
		identity.DurationMS,
	)
	if err != nil {
		return model.PreferredVideo{}, fmt.Errorf("copy preferred video to matching track: %w", err)
	}
	return s.Get(ctx, trackID)
}

func canonicalTrackIdentity(identity *model.TrackIdentity) (string, bool) {
	if identity == nil || identity.DurationMS <= 0 {
		return "", false
	}
	title := normalizeIdentityPart(identity.Title)
	artist := normalizeIdentityPart(identity.Artist)
	if title == "" || artist == "" {
		return "", false
	}
	digest := sha256.Sum256([]byte(artist + "\x00" + title))
	return hex.EncodeToString(digest[:]), true
}

var titleNoisePattern = regexp.MustCompile(`(?i)\s*[\(\[\{](?:feat\.?|ft\.?|featuring|remaster(?:ed)?(?:\s+\d+)?|live|official(?:\s+(?:video|audio|music\s+video))?|version|radio\s+edit|deluxe|bonus\s+track|mono|stereo)[^\)\]\}]*[\)\]\}]|\s*-\s*(?:remaster(?:ed)?(?:\s+\d+)?|live|radio\s+edit|mono|stereo).*$`)

func normalizeIdentityPart(value string) string {
	val := strings.ToLower(value)
	val = strings.ReplaceAll(val, "’", "'")
	val = strings.ReplaceAll(val, "‘", "'")
	val = strings.ReplaceAll(val, "“", "\"")
	val = strings.ReplaceAll(val, "”", "\"")
	val = titleNoisePattern.ReplaceAllString(val, "")
	return strings.TrimSpace(strings.Join(strings.Fields(val), " "))
}

func durationRange(durationMS int64) (int64, int64) {
	minimum := durationMS - trackDurationToleranceMS
	if minimum < 1 {
		minimum = 1
	}
	return minimum, durationMS + trackDurationToleranceMS
}

type rowScanner interface {
	Scan(dest ...any) error
}

func scanVideo(row rowScanner) (model.PreferredVideo, error) {
	var video model.PreferredVideo
	var thumbnailURL sql.NullString
	var mappingUpdatedAt string
	var lastPlayedAt string
	err := row.Scan(
		&video.TrackID,
		&video.Provider,
		&video.VideoID,
		&video.Title,
		&video.Publisher,
		&thumbnailURL,
		&video.DurationMS,
		&video.ViewCount,
		&mappingUpdatedAt,
		&lastPlayedAt,
	)
	if errors.Is(err, sql.ErrNoRows) {
		return model.PreferredVideo{}, ErrNotFound
	}
	if err != nil {
		return model.PreferredVideo{}, fmt.Errorf("scan preferred video: %w", err)
	}
	if thumbnailURL.Valid {
		video.ThumbnailURL = &thumbnailURL.String
	}
	video.MappingUpdatedAt, err = time.Parse(time.RFC3339Nano, mappingUpdatedAt)
	if err != nil {
		return model.PreferredVideo{}, fmt.Errorf("parse mapping timestamp: %w", err)
	}
	video.LastPlayedAt, err = time.Parse(time.RFC3339Nano, lastPlayedAt)
	if err != nil {
		return model.PreferredVideo{}, fmt.Errorf("parse played timestamp: %w", err)
	}
	return video, nil
}
