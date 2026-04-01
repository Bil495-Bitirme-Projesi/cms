#!/usr/bin/env bash
# ============================================================
# generate-certs.sh
# Generates a self-signed TLS certificate for CMS.
#
# Usage:
#   bash nginx/generate-certs.sh
#
# Output: nginx/certs/server.crt  +  nginx/certs/server.key
#
# For production with a publicly trusted certificate, replace
# the generated files with your CA-signed certificate and key,
# keeping the same filenames so nginx.conf does not need changes.
# ============================================================

set -e

CERT_DIR="$(cd "$(dirname "$0")" && pwd)/certs"
mkdir -p "$CERT_DIR"

if [ -f "$CERT_DIR/server.crt" ] && [ -f "$CERT_DIR/server.key" ]; then
    echo "[certs] Certificates already exist — skipping generation."
    echo "        Delete $CERT_DIR to regenerate."
    exit 0
fi

openssl req -x509 -nodes -days 3650 \
    -newkey rsa:2048 \
    -keyout "$CERT_DIR/server.key" \
    -out    "$CERT_DIR/server.crt" \
    -subj   "/C=TR/O=CMS/CN=cms-server" \
    -addext "subjectAltName=IP:127.0.0.1,DNS:localhost"

echo "[certs] Self-signed certificate generated:"
echo "        $CERT_DIR/server.crt  (valid 10 years)"
echo "        $CERT_DIR/server.key"

