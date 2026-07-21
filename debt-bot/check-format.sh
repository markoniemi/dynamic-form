#!/bin/bash
# Check Python code style with ruff and black
# Usage: ./check-format.sh [--fix]

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FIX=${1:-}

echo "🔍 Checking Python code style with ruff and black..."

if [ "$FIX" = "--fix" ]; then
    echo "📝 Auto-formatting with black..."
    black "$SCRIPT_DIR" --line-length=100

    echo "🧹 Linting with ruff (auto-fix)..."
    ruff check "$SCRIPT_DIR" --fix

    echo "✅ Code formatted and linted!"
else
    echo "🧹 Linting with ruff..."
    ruff check "$SCRIPT_DIR"

    echo "🎨 Checking format with black..."
    black "$SCRIPT_DIR" --check --line-length=100

    echo "✅ All checks passed!"
fi
