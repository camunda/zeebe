#!/bin/bash
set -ex
echo "IS_DRY_RUN=${IS_DRY_RUN}"
echo "IS_RC=${IS_RC}"

git config user.name "${GITHUB_ACTOR}"
git config user.email "${GITHUB_ACTOR_ID}+${GITHUB_ACTOR}@users.noreply.github.com"
git remote set-url origin "https://${GITHUB_ACTOR}:${GITHUB_TOKEN}@github.com/camunda/camunda.git"
git fetch
git checkout $BRANCH

SKIP_PUSH_ARTIFACTS="true"
if [ "$IS_DRY_RUN" = "true" ]; then
    SKIP_PUSH_ARTIFACTS="true"
    echo "WARNING: You are running the release in DRY RUN mode."
    echo "No artifacts will be pushed to nexus."
    echo "No git commits will be pushed to the release branch."
    echo "No release tags will be pushed to the Optimize repository."
else
    SKIP_PUSH_ARTIFACTS="false"
    echo "The generated artifacts will be pushed to nexus."
    echo "The release commits and release tag will be pushed to github."
fi

echo "SKIP_PUSH_ARTIFACTS=${SKIP_PUSH_ARTIFACTS}"

TAG="$RELEASE_VERSION-optimize-test"

echo "Release perform"
mvn -f optimize \
    -DlocalCheckout=true \
    -DskipTests=true \
    -B \
    --fail-at-end \
    -Prelease,engine-latest \
    -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn \
    release:perform \
    -Darguments="-Dmaven.deploy.skip=${SKIP_PUSH_ARTIFACTS} -DskipTests -DskipNexusStagingDeployMojo=${SKIP_PUSH_ARTIFACTS} -B --fail-at-end -Dorg.slf4j.simpleLogger.log.org.apache.maven.cli.transfer.Slf4jMavenTransferListener=warn -f pom.xml"

if [[ $IS_RC == true ]]; then
    echo "Removing tag for RC release"
    git push origin ":refs/tags/$TAG"
fi

echo "Artifacts created:"
ls -1 optimize-distro/target
