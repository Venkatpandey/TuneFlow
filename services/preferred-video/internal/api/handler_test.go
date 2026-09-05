package api

import (
	"bytes"
	"context"
	"errors"
	"io"
	"log/slog"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
	"time"

	"github.com/Venkatpandey/TuneFlow/services/preferred-video/internal/model"
	"github.com/Venkatpandey/TuneFlow/services/preferred-video/internal/storage"
)

func TestGetPreferredVideoSuccess(t *testing.T) {
	wanted := storedVideo("track-1", "aaaaaaaaaaa")
	store := &fakeStore{getVideo: wanted}
	response := serve(t, store, http.MethodGet, "/v1/tracks/track-1/preferred-video", "")

	if response.Code != http.StatusOK {
		t.Fatalf("status = %d, body = %s", response.Code, response.Body.String())
	}
	if !strings.Contains(response.Body.String(), `"apiVersion":"v1"`) ||
		!strings.Contains(response.Body.String(), `"videoId":"aaaaaaaaaaa"`) {
		t.Fatalf("unexpected response: %s", response.Body.String())
	}
}

func TestGetPreferredVideoMissing(t *testing.T) {
	store := &fakeStore{getErr: storage.ErrNotFound}
	response := serve(t, store, http.MethodGet, "/v1/tracks/missing/preferred-video", "")

	if response.Code != http.StatusNotFound {
		t.Fatalf("status = %d, body = %s", response.Code, response.Body.String())
	}
	if !strings.Contains(response.Body.String(), `"code":"not_found"`) {
		t.Fatalf("unexpected response: %s", response.Body.String())
	}
}

func TestPutPreferredVideoValidatesInput(t *testing.T) {
	body := `{"provider":"youtube","videoId":"bad","title":"Title","publisher":"Artist","durationMs":1,"viewCount":0}`
	response := serve(t, &fakeStore{}, http.MethodPut, "/v1/tracks/track-1/preferred-video", body)

	if response.Code != http.StatusBadRequest {
		t.Fatalf("status = %d, body = %s", response.Code, response.Body.String())
	}
	if !strings.Contains(response.Body.String(), `"code":"invalid_input"`) {
		t.Fatalf("unexpected response: %s", response.Body.String())
	}
}

func TestPutPreferredVideoPersistsValidatedInput(t *testing.T) {
	store := &fakeStore{putVideo: storedVideo("track-1", "aaaaaaaaaaa")}
	body := `{"provider":"youtube","videoId":"aaaaaaaaaaa","title":"Title","publisher":"Artist","thumbnailUrl":null,"durationMs":180000,"viewCount":42}`
	response := serve(t, store, http.MethodPut, "/v1/tracks/track-1/preferred-video", body)

	if response.Code != http.StatusOK {
		t.Fatalf("status = %d, body = %s", response.Code, response.Body.String())
	}
	if store.putTrackID != "track-1" || store.putInput.VideoID != "aaaaaaaaaaa" {
		t.Fatalf("unexpected put call: track=%s input=%+v", store.putTrackID, store.putInput)
	}
}

func TestDatabaseFailureReturnsGenericServerError(t *testing.T) {
	store := &fakeStore{getErr: errors.New("database file /secret/path failed")}
	response := serve(t, store, http.MethodGet, "/v1/tracks/track-1/preferred-video", "")

	if response.Code != http.StatusInternalServerError {
		t.Fatalf("status = %d, body = %s", response.Code, response.Body.String())
	}
	if strings.Contains(response.Body.String(), "/secret/path") {
		t.Fatalf("response leaked internal error: %s", response.Body.String())
	}
}

func TestRecentLimitIsCappedAtOneHundred(t *testing.T) {
	store := &fakeStore{recentVideos: []model.PreferredVideo{}}
	response := serve(t, store, http.MethodGet, "/v1/videos/recent?limit=200", "")

	if response.Code != http.StatusOK {
		t.Fatalf("status = %d, body = %s", response.Code, response.Body.String())
	}
	if store.recentLimit != 100 {
		t.Fatalf("recent limit = %d, want 100", store.recentLimit)
	}
}

func serve(t *testing.T, store VideoStore, method, target, body string) *httptest.ResponseRecorder {
	t.Helper()
	logger := slog.New(slog.NewTextHandler(io.Discard, nil))
	handler := NewHandler(store, logger)
	request := httptest.NewRequest(method, target, bytes.NewBufferString(body))
	if body != "" {
		request.Header.Set("Content-Type", "application/json")
	}
	response := httptest.NewRecorder()
	handler.ServeHTTP(response, request)
	return response
}

type fakeStore struct {
	getVideo     model.PreferredVideo
	getErr       error
	putVideo     model.PreferredVideo
	putErr       error
	putTrackID   string
	putInput     model.UpsertPreferredVideo
	recentVideos []model.PreferredVideo
	recentErr    error
	recentLimit  int
}

func (f *fakeStore) Health(context.Context) error { return nil }

func (f *fakeStore) Get(context.Context, string) (model.PreferredVideo, error) {
	return f.getVideo, f.getErr
}

func (f *fakeStore) Put(
	_ context.Context,
	trackID string,
	input model.UpsertPreferredVideo,
) (model.PreferredVideo, error) {
	f.putTrackID = trackID
	f.putInput = input
	return f.putVideo, f.putErr
}

func (f *fakeStore) Delete(context.Context, string) error { return nil }

func (f *fakeStore) MarkPlayed(context.Context, string) (model.PreferredVideo, error) {
	return f.getVideo, f.getErr
}

func (f *fakeStore) Recent(_ context.Context, limit int) ([]model.PreferredVideo, error) {
	f.recentLimit = limit
	return f.recentVideos, f.recentErr
}

func storedVideo(trackID, videoID string) model.PreferredVideo {
	timestamp := time.Date(2026, time.August, 30, 10, 0, 0, 0, time.UTC)
	return model.PreferredVideo{
		TrackID:          trackID,
		Provider:         model.ProviderYouTube,
		VideoID:          videoID,
		Title:            "Title",
		Publisher:        "Artist",
		DurationMS:       180_000,
		ViewCount:        42,
		MappingUpdatedAt: timestamp,
		LastPlayedAt:     timestamp,
	}
}
