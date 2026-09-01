package api

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"log/slog"
	"net/http"
	"net/url"
	"regexp"
	"strconv"
	"strings"
	"time"

	"github.com/Venkatpandey/TuneFlow/services/preferred-video/internal/model"
	"github.com/Venkatpandey/TuneFlow/services/preferred-video/internal/storage"
)

const (
	apiVersion      = "v1"
	defaultLimit    = 5
	maximumLimit    = 20
	maximumBodySize = 64 * 1024
)

var youtubeVideoIDPattern = regexp.MustCompile(`^[A-Za-z0-9_-]{11}$`)

type VideoStore interface {
	Health(context.Context) error
	Get(context.Context, string) (model.PreferredVideo, error)
	Put(context.Context, string, model.UpsertPreferredVideo) (model.PreferredVideo, error)
	Delete(context.Context, string) error
	MarkPlayed(context.Context, string) (model.PreferredVideo, error)
	Recent(context.Context, int) ([]model.PreferredVideo, error)
}

type Handler struct {
	store  VideoStore
	logger *slog.Logger
}

type videoResponse struct {
	APIVersion     string               `json:"apiVersion"`
	PreferredVideo model.PreferredVideo `json:"preferredVideo"`
}

type recentResponse struct {
	APIVersion string                 `json:"apiVersion"`
	Videos     []model.PreferredVideo `json:"videos"`
}

type errorBody struct {
	Code    string `json:"code"`
	Message string `json:"message"`
}

type errorResponse struct {
	APIVersion string    `json:"apiVersion"`
	Error      errorBody `json:"error"`
}

func NewHandler(store VideoStore, logger *slog.Logger) http.Handler {
	handler := &Handler{store: store, logger: logger}
	mux := http.NewServeMux()
	mux.HandleFunc("GET /healthz", handler.health)
	mux.HandleFunc("GET /v1/tracks/{trackId}/preferred-video", handler.getPreferredVideo)
	mux.HandleFunc("PUT /v1/tracks/{trackId}/preferred-video", handler.putPreferredVideo)
	mux.HandleFunc("DELETE /v1/tracks/{trackId}/preferred-video", handler.deletePreferredVideo)
	mux.HandleFunc("POST /v1/tracks/{trackId}/preferred-video/played", handler.markPlayed)
	mux.HandleFunc("GET /v1/videos/recent", handler.recentVideos)
	return handler.recoverPanics(handler.logRequests(mux))
}

func (h *Handler) health(w http.ResponseWriter, r *http.Request) {
	if err := h.store.Health(r.Context()); err != nil {
		h.logger.Error("health check failed", "error", err)
		writeError(w, http.StatusServiceUnavailable, "unavailable", "database is unavailable")
		return
	}
	writeJSON(w, http.StatusOK, map[string]string{"apiVersion": apiVersion, "status": "ok"})
}

func (h *Handler) getPreferredVideo(w http.ResponseWriter, r *http.Request) {
	trackID, ok := validatedTrackID(w, r)
	if !ok {
		return
	}
	video, err := h.store.Get(r.Context(), trackID)
	if errors.Is(err, storage.ErrNotFound) {
		writeError(w, http.StatusNotFound, "not_found", "preferred video was not found")
		return
	}
	if err != nil {
		h.internalError(w, "get preferred video", err)
		return
	}
	writeVideo(w, http.StatusOK, video)
}

func (h *Handler) putPreferredVideo(w http.ResponseWriter, r *http.Request) {
	trackID, ok := validatedTrackID(w, r)
	if !ok {
		return
	}
	var input model.UpsertPreferredVideo
	if err := decodeJSON(w, r, &input); err != nil {
		writeError(w, http.StatusBadRequest, "invalid_json", err.Error())
		return
	}
	if message := validateInput(input); message != "" {
		writeError(w, http.StatusBadRequest, "invalid_input", message)
		return
	}
	video, err := h.store.Put(r.Context(), trackID, input)
	if err != nil {
		h.internalError(w, "put preferred video", err)
		return
	}
	writeVideo(w, http.StatusOK, video)
}

func (h *Handler) deletePreferredVideo(w http.ResponseWriter, r *http.Request) {
	trackID, ok := validatedTrackID(w, r)
	if !ok {
		return
	}
	err := h.store.Delete(r.Context(), trackID)
	if errors.Is(err, storage.ErrNotFound) {
		w.WriteHeader(http.StatusNoContent)
		return
	}
	if err != nil {
		h.internalError(w, "delete preferred video", err)
		return
	}
	w.WriteHeader(http.StatusNoContent)
}

func (h *Handler) markPlayed(w http.ResponseWriter, r *http.Request) {
	trackID, ok := validatedTrackID(w, r)
	if !ok {
		return
	}
	video, err := h.store.MarkPlayed(r.Context(), trackID)
	if errors.Is(err, storage.ErrNotFound) {
		writeError(w, http.StatusNotFound, "not_found", "preferred video was not found")
		return
	}
	if err != nil {
		h.internalError(w, "mark preferred video played", err)
		return
	}
	writeVideo(w, http.StatusOK, video)
}

func (h *Handler) recentVideos(w http.ResponseWriter, r *http.Request) {
	limit := defaultLimit
	if raw := r.URL.Query().Get("limit"); raw != "" {
		parsed, err := strconv.Atoi(raw)
		if err != nil || parsed < 1 {
			writeError(w, http.StatusBadRequest, "invalid_input", "limit must be a positive integer")
			return
		}
		limit = min(parsed, maximumLimit)
	}
	videos, err := h.store.Recent(r.Context(), limit)
	if err != nil {
		h.internalError(w, "list recent videos", err)
		return
	}
	writeJSON(w, http.StatusOK, recentResponse{APIVersion: apiVersion, Videos: videos})
}

func (h *Handler) internalError(w http.ResponseWriter, operation string, err error) {
	h.logger.Error("database operation failed", "operation", operation, "error", err)
	writeError(w, http.StatusInternalServerError, "internal_error", "request could not be completed")
}

func validatedTrackID(w http.ResponseWriter, r *http.Request) (string, bool) {
	trackID := strings.TrimSpace(r.PathValue("trackId"))
	if trackID == "" || len(trackID) > 256 || strings.ContainsAny(trackID, "\r\n\x00") {
		writeError(w, http.StatusBadRequest, "invalid_input", "trackId is invalid")
		return "", false
	}
	return trackID, true
}

func validateInput(input model.UpsertPreferredVideo) string {
	if input.Provider != model.ProviderYouTube {
		return "provider must be youtube"
	}
	if !youtubeVideoIDPattern.MatchString(input.VideoID) {
		return "videoId must be an 11-character YouTube video ID"
	}
	if strings.TrimSpace(input.Title) == "" || len(input.Title) > 500 {
		return "title must contain 1 to 500 characters"
	}
	if strings.TrimSpace(input.Publisher) == "" || len(input.Publisher) > 500 {
		return "publisher must contain 1 to 500 characters"
	}
	if input.DurationMS < 0 {
		return "durationMs must not be negative"
	}
	if input.ViewCount < 0 {
		return "viewCount must not be negative"
	}
	if input.ThumbnailURL != nil {
		parsed, err := url.ParseRequestURI(*input.ThumbnailURL)
		if err != nil || (parsed.Scheme != "http" && parsed.Scheme != "https") || parsed.Host == "" {
			return "thumbnailUrl must be an HTTP or HTTPS URL"
		}
	}
	return ""
}

func decodeJSON(w http.ResponseWriter, r *http.Request, destination any) error {
	r.Body = http.MaxBytesReader(w, r.Body, maximumBodySize)
	decoder := json.NewDecoder(r.Body)
	decoder.DisallowUnknownFields()
	if err := decoder.Decode(destination); err != nil {
		return fmt.Errorf("request body must be valid JSON: %w", err)
	}
	if err := decoder.Decode(&struct{}{}); !errors.Is(err, io.EOF) {
		return errors.New("request body must contain one JSON object")
	}
	return nil
}

func writeVideo(w http.ResponseWriter, status int, video model.PreferredVideo) {
	writeJSON(w, status, videoResponse{APIVersion: apiVersion, PreferredVideo: video})
}

func writeError(w http.ResponseWriter, status int, code, message string) {
	writeJSON(w, status, errorResponse{
		APIVersion: apiVersion,
		Error:      errorBody{Code: code, Message: message},
	})
}

func writeJSON(w http.ResponseWriter, status int, body any) {
	w.Header().Set("Content-Type", "application/json; charset=utf-8")
	w.Header().Set("Cache-Control", "no-store")
	w.WriteHeader(status)
	_ = json.NewEncoder(w).Encode(body)
}

type statusRecorder struct {
	http.ResponseWriter
	status int
}

func (r *statusRecorder) WriteHeader(status int) {
	r.status = status
	r.ResponseWriter.WriteHeader(status)
}

func (h *Handler) logRequests(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		started := time.Now()
		recorder := &statusRecorder{ResponseWriter: w, status: http.StatusOK}
		next.ServeHTTP(recorder, r)
		h.logger.Info(
			"request completed",
			"method", r.Method,
			"route", r.Pattern,
			"status", recorder.status,
			"duration_ms", time.Since(started).Milliseconds(),
		)
	})
}

func (h *Handler) recoverPanics(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		defer func() {
			if recovered := recover(); recovered != nil {
				h.logger.Error("request panic recovered", "error", recovered)
				writeError(w, http.StatusInternalServerError, "internal_error", "request could not be completed")
			}
		}()
		next.ServeHTTP(w, r)
	})
}
