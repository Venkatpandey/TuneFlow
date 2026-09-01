# syntax=docker/dockerfile:1.7
FROM golang:1.25-alpine AS build

WORKDIR /src
COPY go.mod go.sum ./
RUN go mod download

COPY . .
RUN CGO_ENABLED=0 GOOS=linux go test ./... && \
    CGO_ENABLED=0 GOOS=linux go build \
      -trimpath \
      -ldflags="-s -w" \
      -o /out/preferred-video-service \
      ./cmd/server

FROM gcr.io/distroless/static-debian12:nonroot

WORKDIR /app
COPY --from=build /out/preferred-video-service /app/preferred-video-service

EXPOSE 8090
VOLUME ["/data"]
USER nonroot:nonroot
ENTRYPOINT ["/app/preferred-video-service"]
