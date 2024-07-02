#!/bin/bash
set -ex
echo "IS_DRY_RUN=${IS_DRY_RUN}"
echo "IS_RC=${IS_RC}"

git config user.name "${GITHUB_ACTOR}"
git config user.email "${GITHUB_ACTOR_ID}+${GITHUB_ACTOR}@users.noreply.github.com"
git remote set-url origin "https://${GITHUB_APP_ID}:${GITHUB_APP_PRIVATE_KEY}@github.com/camunda/camunda-optimize.git"
git fetch
git checkout $BRANCH

SKIP_PUSH_ARTIFACTS=""
PUSH_CHANGES=""
if [ "$IS_DRY_RUN" = "true" ]; then
    SKIP_PUSH_ARTIFACTS="true"
    PUSH_CHANGES="false"
    echo "WARNING: You are running the release in DRY RUN mode."
    echo "No artifacts will be pushed to nexus."
    echo "No git commits will be pushed to the release branch."
    echo "No release tags will be pushed to the Optimize repository."
else
    SKIP_PUSH_ARTIFACTS="false"
    PUSH_CHANGES="true"
    echo "The generated artifacts will be pushed to nexus."
    echo "The release commits and release tag will be pushed to github."
fi

echo "SKIP_PUSH_ARTIFACTS=${SKIP_PUSH_ARTIFACTS}"
echo "PUSH_CHANGES=${PUSH_CHANGES}"

echo "Starting artifact creation:"
mvn -DpushChanges="${PUSH_CHANGES}" -DskipTests -Prelease,engine-latest release:prepare release:perform -Dtag="${RELEASE_VERSION}" -DreleaseVersion="${RELEASE_VERSION}" -DdevelopmentVersion="${DEVELOPMENT_VERSION}" -Darguments="-DskipTests -DskipNexusStagingDeployMojo=${SKIP_PUSH_ARTIFACTS} -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn" -B --fail-at-end -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn

if [[ $IS_RC == true ]]; then
    echo "Removing tag for RC release"
    git push origin ":refs/tags/$RELEASE_VERSION"
fi

echo "Artifacts created:"
ls -1 optimize-distro/target
