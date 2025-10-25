#!/bin/bash

version="$1"

# check if changelog has already been updated
if grep -qE "^## \[$version\] - [0-9]{4}(-[0-9]{2}){2}" CHANGELOG.md; then
  echo "Changelog has already been edited - $version already present"
  echo "--------------"
  cat CHANGELOG.md
  exit 0
fi

prevVersion=$(
    cat CHANGELOG.md | \
    # get lines with version nums on them
    grep '## \[[0-9]+\.[0-9]+\.[0-9]+\(-\(alpha\|beta\|rc\)\(\.[0-9]+\)\?\)\?\] - [0-9]\{4\}\(-[0-9]\{2\}\)\{2\}' | \
    # extract version number
    grep -o '[0-9]+\.[0-9]+\.[0-9]+\(-\(alpha\|beta\|rc\)\(\.[0-9]+\)\?\)\?' | \
    # get latest one
    head -n 1
)

changes=$(npx conventional-changelog-cli -p angular -r 1 | tail -n +3)

# add latest version
echo "## [$version] - $(date +'%Y-%m-%d')" >> CHANGELOG.md.temp
echo "$changes" | tee -a CHANGELOG.md.temp
echo "" | tee -a CHANGELOG.md.temp  # add some whitespace
echo "" | tee -a CHANGELOG.md.temp
tee -a CHANGELOG.md.temp < CHANGELOG.md

mv CHANGELOG.md.temp CHANGELOG.md

# update links
echo "[$version]: https://github.com/$GITHUB_REPOSITORY/compare/$prevVersion...$version" | tee -a CHANGELOG.md

echo "new changelog:"
echo "--------------"
cat CHANGELOG.md