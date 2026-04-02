#!/usr/bin/env bash
# ============================================================
# generate-certs.sh
# Generates a self-signed TLS certificate for CMS.
#
# Usage:
#   bash nginx/generate-certs.sh [HOST [EXTRA_HOST ...]]
#
#   All provided values are added to subjectAltName.
#   Accepts IP addresses and DNS hostnames.
#   127.0.0.1 and localhost are always included.
#
# Examples:
#   bash nginx/generate-certs.sh
#       # localhost only (single-machine dev)
#
#   bash nginx/generate-certs.sh cms-server.local 192.168.137.1 10.42.0.1
#       # Recommended for demo:
#       #   cms-server.local  — mDNS hostname (set CMS machine hostname to "cms-server")
#       #   192.168.137.1     — Windows Mobile Hotspot host IP (always fixed)
#       #   10.42.0.1         — Linux NetworkManager hotspot host IP
#       # AIS and phone connect to the hotspot; no static IP or DNS server needed.
#
#   bash nginx/generate-certs.sh 192.168.1.42
#       # Fixed LAN IP
#
# Output: nginx/certs/server.crt  +  nginx/certs/server.key
#
# Distribute server.crt to:
#   - AIS:          configure as trusted CA for HTTPS requests to CMS
#   - Flutter app:  bundle for certificate pinning / network_security_config
# Never share server.key outside the CMS host machine.
# ============================================================

set -e

CERT_DIR="$(cd "$(dirname "$0")" && pwd)/certs"
mkdir -p "$CERT_DIR"

if [ -f "$CERT_DIR/server.crt" ] && [ -f "$CERT_DIR/server.key" ]; then
    echo "[certs] Certificates already exist — skipping generation."
    echo "        Delete $CERT_DIR/server.crt and $CERT_DIR/server.key to regenerate."
    exit 0
fi

# Build subjectAltName — always include localhost
SAN_PARTS="IP:127.0.0.1,DNS:localhost"
CN="${1:-localhost}"

for HOST in "$@"; do
    if [[ "$HOST" =~ ^[0-9]+\.[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
        SAN_PARTS="${SAN_PARTS},IP:${HOST}"
    else
        SAN_PARTS="${SAN_PARTS},DNS:${HOST}"
    fi
done

echo "[certs] Generating self-signed certificate"
echo "[certs] CN              : ${CN}"
echo "[certs] subjectAltName  : ${SAN_PARTS}"
echo ""

# On Git Bash / MSYS2, MSYS path conversion would mangle -subj (/C=TR → C:/...)
# Fix: convert output paths to mixed Windows format with cygpath (so openssl can
# open them natively), then disable path conversion for the entire command so
# -subj is passed through unchanged.
if command -v cygpath &>/dev/null; then
    KEY_OUT=$(cygpath -m "$CERT_DIR/server.key")
    CERT_OUT=$(cygpath -m "$CERT_DIR/server.crt")
else
    KEY_OUT="$CERT_DIR/server.key"
    CERT_OUT="$CERT_DIR/server.crt"
fi

MSYS_NO_PATHCONV=1 openssl req -x509 -nodes -days 3650 \
    -newkey rsa:2048 \
    -keyout "$KEY_OUT" \
    -out    "$CERT_OUT" \
    -subj   "/C=TR/O=CMS/CN=${CN}" \
    -addext "subjectAltName=${SAN_PARTS}"

echo ""
echo "[certs] Done. Files written to:"
echo "        $CERT_DIR/server.crt  (valid 10 years) — distribute to AIS and Flutter"
echo "        $CERT_DIR/server.key  — keep private, never commit or share insecurely"
