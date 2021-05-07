#!/usr/bin/env bash

echo "Generating KDoc"
./gradlew dokkaHtml

rm -rf docs/static/kdoc

for module in rekotlin rekotlin-router
do
    mkdir -p docs/static/kdoc/$module
    cp -R $module/build/dokka/html/. docs/static/kdoc/$module
done

cd docs

echo "Deleting old publication"
rm -rf public
mkdir public
git worktree prune
rm -rf .git/worktrees/public/

echo "Checking out gh-pages branch into docs/public"
git worktree add -B gh-pages public origin/gh-pages

echo "Removing existing files"
rm -rf public/*

echo "Generating site"
hugo

echo "Updating gh-pages branch"
cd public && git add --all && git commit -m "Publishing to gh-pages"

git push origin gh-pages
