#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEFAULT_OUTPUT_PATH="$ROOT_DIR/dist/tuneflow-tv.apk"
DEFAULT_KEYSTORE_PATH="$ROOT_DIR/release.keystore"

output_path="${OUTPUT_PATH:-$DEFAULT_OUTPUT_PATH}"
keystore_path="${SIGNING_STORE_FILE:-${KEYSTORE_PATH:-$DEFAULT_KEYSTORE_PATH}}"
store_password="${SIGNING_STORE_PASSWORD:-${KEYSTORE_PASSWORD:-}}"
key_alias="${SIGNING_KEY_ALIAS:-tuneflow}"
key_password="${SIGNING_KEY_PASSWORD:-${SIGNING_STORE_PASSWORD:-${KEYSTORE_PASSWORD:-}}}"
do_clean="false"

usage() {
    cat <<'EOF'
Usage:
  scripts/build-local-release.sh [options]

Options:
  --keystore PATH         Path to the release keystore file.
  --store-password VALUE  Keystore password.
  --key-alias VALUE       Key alias. Default: tuneflow
  --key-password VALUE    Key password. Defaults to store password.
  --output PATH           Output APK path. Default: dist/tuneflow-tv.apk
  --clean                 Run ./gradlew clean before assembleRelease.
  -h, --help              Show this help.

Environment variables also supported:
  SIGNING_STORE_FILE
  SIGNING_STORE_PASSWORD
  SIGNING_KEY_ALIAS
  SIGNING_KEY_PASSWORD
  OUTPUT_PATH

Example:
  scripts/build-local-release.sh \
    --keystore /Users/me/keys/release.keystore \
    --store-password 'secret123' \
    --key-alias tuneflow \
    --key-password 'secret123'
EOF
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --keystore)
            keystore_path="$2"
            shift 2
            ;;
        --store-password)
            store_password="$2"
            shift 2
            ;;
        --key-alias)
            key_alias="$2"
            shift 2
            ;;
        --key-password)
            key_password="$2"
            shift 2
            ;;
        --output)
            output_path="$2"
            shift 2
            ;;
        --clean)
            do_clean="true"
            shift
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        *)
            echo "Unknown argument: $1" >&2
            usage
            exit 1
            ;;
    esac
done

if [[ ! -f "$keystore_path" ]]; then
    echo "Keystore not found: $keystore_path" >&2
    exit 1
fi

if [[ -z "$store_password" ]]; then
    echo "Missing keystore password. Use --store-password or SIGNING_STORE_PASSWORD." >&2
    exit 1
fi

if [[ -z "$key_alias" ]]; then
    echo "Missing key alias. Use --key-alias or SIGNING_KEY_ALIAS." >&2
    exit 1
fi

if [[ -z "$key_password" ]]; then
    echo "Missing key password. Use --key-password or SIGNING_KEY_PASSWORD." >&2
    exit 1
fi

export SIGNING_STORE_FILE="$keystore_path"
export SIGNING_STORE_PASSWORD="$store_password"
export SIGNING_KEY_ALIAS="$key_alias"
export SIGNING_KEY_PASSWORD="$key_password"

cd "$ROOT_DIR"

if [[ "$do_clean" == "true" ]]; then
    ./gradlew clean
fi

./gradlew :app:assembleRelease --stacktrace

apk_path="$(find app/build/outputs/apk/release -maxdepth 1 -type f -name '*.apk' ! -name '*-unsigned.apk' | head -n1)"

if [[ -z "$apk_path" ]]; then
    echo "Signed APK not found under app/build/outputs/apk/release" >&2
    exit 1
fi

mkdir -p "$(dirname "$output_path")"
cp "$apk_path" "$output_path"

find_apksigner() {
    local sdk_root=""

    if [[ -n "${ANDROID_HOME:-}" ]]; then
        sdk_root="$ANDROID_HOME"
    elif [[ -n "${ANDROID_SDK_ROOT:-}" ]]; then
        sdk_root="$ANDROID_SDK_ROOT"
    elif [[ -f "$ROOT_DIR/local.properties" ]]; then
        sdk_root="$(sed -n 's/^sdk.dir=//p' "$ROOT_DIR/local.properties" | tail -n1 | sed 's#\\:#:#g' | sed 's#\\\\#/#g')"
    elif [[ -d "$HOME/Library/Android/sdk" ]]; then
        sdk_root="$HOME/Library/Android/sdk"
    fi

    if [[ -n "$sdk_root" && -d "$sdk_root/build-tools" ]]; then
        find "$sdk_root/build-tools" -type f -name apksigner | sort -V | tail -n1
    fi
}

apksigner_path="$(find_apksigner || true)"

if [[ -n "$apksigner_path" ]]; then
    "$apksigner_path" verify --print-certs "$output_path"
else
    echo "apksigner not found; skipped signature verification." >&2
fi

echo "Built signed APK: $output_path"
