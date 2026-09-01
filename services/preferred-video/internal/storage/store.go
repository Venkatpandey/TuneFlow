package storage

import (
	"context"
	"database/sql"
	"embed"
	"errors"
	"fmt"
	"io/fs"
	"os"
	"path/filepath"
	"sort"
	"strings"
	"time"

	"github.com/Venkatpandey/TuneFlow/services/preferred-video/internal/model"
	_ "modernc.org/sqlite"
)

var ErrNotFound = errors.New("preferred video not found")

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

func (s *Store) Put(
	ctx context.Context,
	trackID string,
	input model.UpsertPreferredVideo,
) (model.PreferredVideo, error) {
	now := s.now().UTC().Format(time.RFC3339Nano)
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

func (s *Store) Delete(ctx context.Context, trackID string) error {
	result, err := s.db.ExecContext(ctx, "DELETE FROM preferred_videos WHERE track_id = ?", trackID)
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

func (s *Store) Recent(ctx context.Context, limit int) ([]model.PreferredVideo, error) {
	rows, err := s.db.QueryContext(ctx, `
		SELECT track_id, provider, video_id, title, publisher, thumbnail_url,
		       duration_ms, view_count, mapping_updated_at, last_played_at
		FROM preferred_videos
		ORDER BY last_played_at DESC, track_id ASC
		LIMIT ?`, limit)
	if err != nil {
		return nil, fmt.Errorf("query recent videos: %w", err)
	}
	defer rows.Close()

	videos := make([]model.PreferredVideo, 0, limit)
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
