# TuneFlow preferred-video service

Small LAN-only Go service storing TuneFlow track-to-YouTube mappings and recent video playback in SQLite. TuneFlow talks to this service over REST; only this process opens the SQLite database.

The service has no authentication. Never expose its port to the public internet, a router port-forward, or an untrusted network.

## NAS deployment

Requirements:

- Docker Engine with Docker Compose v2
- A persistent NAS directory writable by container UID `65532`
- A stable LAN address or hostname reachable from the TV
- Public pull access to `ghcr.io/venkatpandey/tuneflow-preferred-video`, or a prior `docker login ghcr.io` when the package is private

Copy `compose.yaml` and `.env.example` from this directory to one NAS deployment directory, then rename `.env.example` to `.env`.

Edit `.env` for the NAS:

```dotenv
PREFERRED_VIDEO_IMAGE_TAG=latest
PREFERRED_VIDEO_BIND_ADDRESS=192.168.1.10
PREFERRED_VIDEO_PORT=8090
PREFERRED_VIDEO_DATA_DIR=/volume1/docker/tuneflow-preferred-video
```

`PREFERRED_VIDEO_BIND_ADDRESS` should be the NAS LAN address, and `PREFERRED_VIDEO_DATA_DIR` should be an absolute persistent directory. Then:

```bash
mkdir -p /volume1/docker/tuneflow-preferred-video
chown 65532:65532 /volume1/docker/tuneflow-preferred-video
docker compose pull
docker compose up -d
docker compose ps
curl --fail http://192.168.1.10:8090/healthz
```

Adjust paths and ownership commands for the NAS platform. The database lives at `${PREFERRED_VIDEO_DATA_DIR}/preferred-videos.db`, outside the container writable layer. Container recreation and upgrades keep it.

Compose applies a read-only root filesystem, drops Linux capabilities, enables `no-new-privileges`, configures a health check, and uses `restart: unless-stopped`.

To upgrade after a new image is published:

```bash
docker compose pull
docker compose up -d
```

`latest` is published from `main`. Version tags such as `v1.2.0` publish an immutable matching image tag, and each build also publishes a `sha-...` tag. Set `PREFERRED_VIDEO_IMAGE_TAG` to a version or commit tag when a deployment must stay pinned.

The GitHub Actions publish workflow builds both `linux/amd64` and `linux/arm64` images and pushes them to GHCR. After the first publish, make the package public in GitHub package settings for anonymous NAS pulls. If it remains private, log in on the NAS before pulling:

```bash
echo "$GHCR_TOKEN" | docker login ghcr.io --username YOUR_GITHUB_USER --password-stdin
```

Use a classic personal access token with only `read:packages` for private pulls. Do not store the token in `compose.yaml` or commit it.

## TuneFlow APK configuration

After the container is healthy, open TuneFlow and select **Home > Quick Actions > Video Service**. Enter the LAN URL, for example:

```text
http://192.168.1.10:8090
```

TuneFlow stores only this service address locally. Preferred mappings and video history remain in the NAS SQLite service. The URL can be changed or disabled without rebuilding the APK.

Managed builds can still provide a default service address with either:

```bash
PREFERRED_VIDEO_SERVICE_URL=http://192.168.1.10:8090 ./gradlew :app:assembleDebug
```

or:

```bash
./gradlew :app:assembleDebug -PpreferredVideoServiceUrl=http://192.168.1.10:8090
```

The runtime value overrides the build default. An APK with no configured service URL keeps video search/playback working but has no durable preferred mappings or recent-video row.

## Configuration

| Variable | Default | Purpose |
| --- | --- | --- |
| `LISTEN_ADDRESS` | `0.0.0.0:8090` | Container listen address and port |
| `DATABASE_PATH` | `/data/preferred-videos.db` | SQLite file inside mounted volume |
| `READ_TIMEOUT` | `5s` | HTTP read and header timeout |
| `WRITE_TIMEOUT` | `10s` | HTTP response write timeout |
| `IDLE_TIMEOUT` | `60s` | HTTP keep-alive idle timeout |
| `SHUTDOWN_TIMEOUT` | `10s` | Graceful shutdown deadline |
| `HEALTHCHECK_URL` | `http://127.0.0.1:8090/healthz` | Container health probe URL |

## API

All JSON responses include `"apiVersion":"v1"`.

- `GET /healthz`
- `GET /v1/tracks/{trackId}/preferred-video`
- `PUT /v1/tracks/{trackId}/preferred-video`
- `DELETE /v1/tracks/{trackId}/preferred-video`
- `POST /v1/tracks/{trackId}/preferred-video/played`
- `GET /v1/videos/recent?limit=5` (`limit` capped at 20)

Example mapping write after confirmed playback:

```bash
curl --fail-with-body \
  --request PUT \
  --header 'Content-Type: application/json' \
  --data '{
    "provider": "youtube",
    "videoId": "dQw4w9WgXcQ",
    "title": "Example",
    "publisher": "Example artist",
    "thumbnailUrl": "https://i.ytimg.com/vi/dQw4w9WgXcQ/hqdefault.jpg",
    "durationMs": 180000,
    "viewCount": 1
  }' \
  http://192.168.1.10:8090/v1/tracks/TRACK_ID/preferred-video
```

## Migrations

SQL migrations are embedded from `internal/storage/migrations` and recorded in `schema_migrations`. Startup applies each migration once, in filename order and inside a transaction. Repeated startup does not recreate tables or reapply completed migrations. Migration SQL also uses `IF NOT EXISTS` as a second safety guard.

Never edit an already-deployed migration. Add the next numbered migration instead.

## Backup and restore

Use a stopped-container archive so the SQLite database and any WAL sidecars remain together:

```bash
docker compose stop preferred-video
tar -C /volume1/docker/tuneflow-preferred-video \
  -czf /volume1/backups/tuneflow-preferred-videos-$(date +%F).tar.gz \
  preferred-videos.db preferred-videos.db-wal preferred-videos.db-shm 2>/dev/null || \
tar -C /volume1/docker/tuneflow-preferred-video \
  -czf /volume1/backups/tuneflow-preferred-videos-$(date +%F).tar.gz \
  preferred-videos.db
docker compose start preferred-video
```

Restore:

```bash
docker compose stop preferred-video
mkdir -p /volume1/docker/tuneflow-preferred-video-restore
tar -C /volume1/docker/tuneflow-preferred-video-restore \
  -xzf /volume1/backups/tuneflow-preferred-videos-2026-09-01.tar.gz
chown -R 65532:65532 /volume1/docker/tuneflow-preferred-video-restore
```

Point `PREFERRED_VIDEO_DATA_DIR` at restored directory, start container, then verify `/healthz` and recent results. Restore only trusted backup. Keep original data directory until verification passes.

## Local verification

```bash
go test ./...
go vet ./...
docker compose config
docker build .
```
