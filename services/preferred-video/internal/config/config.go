package config

import (
	"fmt"
	"os"
	"strconv"
	"strings"
	"time"
)

type Config struct {
	ListenAddress   string
	DatabasePath    string
	ReadTimeout     time.Duration
	WriteTimeout    time.Duration
	IdleTimeout     time.Duration
	ShutdownTimeout time.Duration
}

func Load() (Config, error) {
	config := Config{
		ListenAddress:   envOrDefault("LISTEN_ADDRESS", "0.0.0.0:8090"),
		DatabasePath:    envOrDefault("DATABASE_PATH", "/data/preferred-videos.db"),
		ReadTimeout:     5 * time.Second,
		WriteTimeout:    10 * time.Second,
		IdleTimeout:     60 * time.Second,
		ShutdownTimeout: 10 * time.Second,
	}

	var err error
	if config.ReadTimeout, err = durationFromEnv("READ_TIMEOUT", config.ReadTimeout); err != nil {
		return Config{}, err
	}
	if config.WriteTimeout, err = durationFromEnv("WRITE_TIMEOUT", config.WriteTimeout); err != nil {
		return Config{}, err
	}
	if config.IdleTimeout, err = durationFromEnv("IDLE_TIMEOUT", config.IdleTimeout); err != nil {
		return Config{}, err
	}
	if config.ShutdownTimeout, err = durationFromEnv("SHUTDOWN_TIMEOUT", config.ShutdownTimeout); err != nil {
		return Config{}, err
	}
	if strings.TrimSpace(config.ListenAddress) == "" {
		return Config{}, fmt.Errorf("LISTEN_ADDRESS must not be empty")
	}
	if strings.TrimSpace(config.DatabasePath) == "" {
		return Config{}, fmt.Errorf("DATABASE_PATH must not be empty")
	}
	return config, nil
}

func envOrDefault(name, fallback string) string {
	if value, ok := os.LookupEnv(name); ok {
		return value
	}
	return fallback
}

func durationFromEnv(name string, fallback time.Duration) (time.Duration, error) {
	raw, ok := os.LookupEnv(name)
	if !ok {
		return fallback, nil
	}
	value, err := time.ParseDuration(raw)
	if err != nil || value <= 0 {
		return 0, fmt.Errorf("%s must be a positive duration, got %s", name, strconv.Quote(raw))
	}
	return value, nil
}
