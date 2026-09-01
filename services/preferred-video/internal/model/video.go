package model

import "time"

const ProviderYouTube = "youtube"

type PreferredVideo struct {
	TrackID          string    `json:"trackId"`
	Provider         string    `json:"provider"`
	VideoID          string    `json:"videoId"`
	Title            string    `json:"title"`
	Publisher        string    `json:"publisher"`
	ThumbnailURL     *string   `json:"thumbnailUrl"`
	DurationMS       int64     `json:"durationMs"`
	ViewCount        int64     `json:"viewCount"`
	MappingUpdatedAt time.Time `json:"mappingUpdatedAt"`
	LastPlayedAt     time.Time `json:"lastPlayedAt"`
}

type UpsertPreferredVideo struct {
	Provider     string  `json:"provider"`
	VideoID      string  `json:"videoId"`
	Title        string  `json:"title"`
	Publisher    string  `json:"publisher"`
	ThumbnailURL *string `json:"thumbnailUrl"`
	DurationMS   int64   `json:"durationMs"`
	ViewCount    int64   `json:"viewCount"`
}
