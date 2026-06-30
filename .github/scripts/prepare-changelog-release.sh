#!/usr/bin/env bash
#
# Promote the "Unreleased" section of CHANGELOG.md into a dated release section
# and update the comparison links at the bottom of the file. Run by the Release
# workflow; can also be run by hand before a release.
#
# Usage: prepare-changelog-release.sh <version>
#   e.g. prepare-changelog-release.sh 1.18.0
#
# The release date is today's date in UTC and the release tag is "v<version>",
# matching the tag the Release workflow creates.
set -euo pipefail

version="${1:?usage: prepare-changelog-release.sh <version>}"
changelog="CHANGELOG.md"
date="$(date -u +%Y-%m-%d)"
new_tag="v${version}"

# The "[Unreleased]" link encodes the previous release tag as the base of its
# compare range, e.g. ".../compare/zt-zip-1.17...HEAD" -> previous tag zt-zip-1.17.
unreleased_link="$(grep -E '^\[Unreleased\]:' "$changelog" || true)"
base_url="$(printf '%s\n' "$unreleased_link" | sed -E 's#^\[Unreleased\]: (https?://[^ ]+)/compare/.*#\1#')"
prev_tag="$(printf '%s\n' "$unreleased_link" | sed -E 's#^.*/compare/(.+)\.\.\.HEAD$#\1#')"

if [ -z "$base_url" ] || [ -z "$prev_tag" ] || [ "$base_url" = "$unreleased_link" ]; then
  echo "Could not parse the [Unreleased] comparison link in $changelog" >&2
  exit 1
fi

# Refuse to release an empty Unreleased section.
if ! awk '
    /^## \[Unreleased\]/ { in_section = 1; next }
    /^## \[/             { in_section = 0 }
    in_section && /[^[:space:]]/ { found = 1 }
    END { exit found ? 0 : 1 }
  ' "$changelog"; then
  echo "The [Unreleased] section is empty; nothing to release." >&2
  exit 1
fi

awk -v version="$version" -v date="$date" -v new_tag="$new_tag" \
    -v prev_tag="$prev_tag" -v base_url="$base_url" '
  # Keep the (now empty) Unreleased header and open a dated section below it.
  /^## \[Unreleased\]/ {
    print
    print ""
    print "## [" version "] - " date
    next
  }
  # Rewrite the Unreleased compare link and add the new release link below it.
  /^\[Unreleased\]:/ {
    print "[Unreleased]: " base_url "/compare/" new_tag "...HEAD"
    print "[" version "]: " base_url "/compare/" prev_tag "..." new_tag
    next
  }
  { print }
' "$changelog" > "$changelog.tmp"

mv "$changelog.tmp" "$changelog"
echo "Promoted Unreleased to [$version] - $date (tag $new_tag, previous $prev_tag)."
