#!/bin/bash

set -o pipefail

declare Pkg=npm-publish
declare Version=0.1.0

function msg() {
    echo "$Pkg: $*"
}

function err() {
    msg "$*" 1>&2
}

function die() {
    echo "npm-publish: $*"
    exit 1
}

function main() {
    local module_version=$1
    if [[ ! $module_version ]]; then
        err "first parameter must be the version number of the module to publish"
        return 10
    fi

    local target="target/nodejs"
    rm -rf "$target"
    if ! mkdir -p "$target"; then
        err "failed to create $target"
        return 1
    fi

    local model_src=src/main/typescript
    if ! cp -r "$model_src"/* "$target"; then
        err "error copying TypeScript model in $model_src to $target"
        return 1
    fi

    if ! jq --arg version "$module_version" '.version = $version' "$model_src/package.json" > "$target/package.json"; then
        err "failed to set version in package.json"
        return 1
    fi

    cd "$target"
    cd user-model

    if [[ $NPM_TOKEN ]]; then
        msg "Creating local .npmrc using API key from environment"
        if ! ( umask 077 && echo "//registry.npmjs.org/:_authToken=$NPM_TOKEN" > "$HOME/.npmrc" ); then
            err "failed to create $HOME/.npmrc"
            return 1
        fi
    else
        echo "assuming your .npmrc is setup correctly for this project"
    fi

    # npm honors this
    rm -f "$target/.gitignore"

    if ! ( cd "$target" && npm publish --access=public ); then
        error "failed to publish node module"
        cat "$target/npm-debug.log"
        return 1
    fi
}

main "$@" || exit 1
exit 0
