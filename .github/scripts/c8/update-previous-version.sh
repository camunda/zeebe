#!/bin/bash
set -ex

git config user.name "${GITHUB_ACTOR}"
git config user.email "${GITHUB_ACTOR_ID}+${GITHUB_ACTOR}@users.noreply.github.com"
git remote set-url origin "https://${GITHUB_APP_ID}:${GITHUB_APP_PRIVATE_KEY}@github.com/camunda/camunda.git"

if [ "$IS_PATCH" = "false" ]; then
  # only major / minor GA (.0) release versions will trigger an auto-update of previousVersion property.
  echo "Auto-updating previousVersion property as release version is a valid major/minor version."
  git fetch
  git checkout ${BRANCH}
  sed -i "s/project.previousVersion>.*</project.previousVersion>${RELEASE_VERSION}</g" pom.xml
  git add pom.xml
  # This is needed to not abort the job in case 'git diff' returns a status different from 0
  set +e
  git diff --staged --quiet
  diff_result=$?
  set -e

  if [ $diff_result -ne 0 ]; then
    git commit -m "chore: update previousVersion to new release version ${RELEASE_VERSION}"
    echo "pushing to branch ${BRANCH}"
    if [ "$IS_DRY_RUN" = "true" ]; then
      echo "not pushing to branch ${BRANCH} in dry run mode"
    else
      echo "pushing to branch ${BRANCH}"
      git push origin ${BRANCH}
    fi
  else
    echo "Release version ${RELEASE_VERSION} did not change. Nothing to commit."
  fi
else
  echo "Not auto-updating previousVersion property as release version is not a valid major/minor version."
fi
