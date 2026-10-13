#!/usr/bin/env bash

#############################################################
#
# Release script.
#
# Input and validate a new version, write it to the root VERSION
# file and the Python package, commit, and push a vX.Y.Z tag.
# Jenkins builds the tag and publishes both libraries to AWS
# CodeArtifact.
#
#############################################################

set -eou pipefail

if [ -n "$(git status --untracked-files=no --porcelain)" ]; then
    echo "Working directory dirty - commit any changes before trying to release"
    exit 1
fi

BRANCH=$(git branch | grep \* | cut -d ' ' -f2)

if [ "main" != "$BRANCH" ]; then
    echo "On branch \"$BRANCH\" - must be on branch \"main\" to release"
    exit 1
fi

echo "Current version is $(cat VERSION)"

read -p "Enter a new version: " VERSION

if ! [[ "$VERSION" =~ ^[[:digit:]]+\.[[:digit:]]+\.[[:digit:]]+ ]]; then
    echo "Invalid version - must follow semver, e.g. 3.0.7"
    exit 1
fi

echo $VERSION > VERSION
echo "__version__ = \"${VERSION}\"" > python/auth_middleware/version.py

echo "Committing version $VERSION..."
git commit -a -m "Version $VERSION"
git tag "v$VERSION"

echo "Pushing $VERSION and tag v$VERSION to \"main\"..."
git push origin main "v$VERSION"

echo "Done: Jenkins publishes $VERSION from the tag build."
