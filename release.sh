#!/usr/bin/env bash

#############################################################
#
# Release script.
#
# Input and validate and new version, and write it to the
# root VERSION file. Run both the Scala and Python release job
# and commit any changes.
#
#############################################################

set -eou pipefail

if [ -n "$(git status --untracked-files=no --porcelain)" ]; then
    echo "Working directory dirty - commit any changes before trying to release"
    exit 1
fi

BRANCH=$(git branch | grep \* | cut -d ' ' -f2)

if [ "master" != "$BRANCH" ]; then
    echo "On branch \"$BRANCH\" - must be on branch \"master\" to release"
    exit 1
fi

if ! nc -zv -G 3 nexus.pennsieve.cc 443 > /dev/null 2>&1; then
    echo "Could not connect to nexus.pennsieve.cc - are you on the VPN?"
    exit 1
fi

echo "Current version is $(cat VERSION)"

read -p "Enter a new version: " VERSION

if ! [[ "$VERSION" =~ ^[[:digit:]]+\.[[:digit:]]+\.[[:digit:]]+ ]]; then
    echo "Invalid version - must follow semver, e.g. 3.0.7"
    exit 1
fi

echo $VERSION > VERSION

VERSION="$VERSION" make -C python release
VERSION="$VERSION" make -C scala release

echo "Committing version $VERSION..."
git commit -a -m "Version $VERSION"

echo "Pushing $VERSION to \"master\"..."
git push origin master

echo "Done"
