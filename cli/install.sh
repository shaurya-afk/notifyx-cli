#!/bin/bash

set -e  # Exit on error

# Detect path
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CLI_FILE="$SCRIPT_DIR/notifyx"

echo "📦 Installing notifyx CLI..."

# Check if CLI exists
if [[ ! -f "$CLI_FILE" ]]; then
  echo "❌ Could not find notifyx script at $CLI_FILE"
  exit 1
fi

# Make it executable
chmod +x "$CLI_FILE"

# Remove old symlink if exists
if [[ -L /usr/local/bin/notifyx ]]; then
  echo "⚠️ Removing old notifyx symlink"
  sudo rm /usr/local/bin/notifyx
fi

# Create new symlink
echo "🔗 Linking notifyx to /usr/local/bin"
sudo ln -s "$CLI_FILE" /usr/local/bin/notifyx

# Check dependencies
echo "Checking dependencies..."
for cmd in curl jq; do
  if ! command -v $cmd &>/dev/null; then
    echo "Missing required tool: $cmd"
    echo "Please install it and re-run this script."
    exit 1
  fi
done

# Done
echo "notifyx CLI installed successfully!"
notifyx help || echo "CLI linked, but something went wrong when running it."
