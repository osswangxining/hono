#!/bin/bash

BASE=`dirname $0`
ABS_BASE=`cd $BASE && pwd`
echo $ABS_BASE

BUILDER_IMAGE=eclipsehono/qpid-dispatch-build

# Assumes that DOCKER_USER and DOCKER_PASS are set
REPO=eclipsehono/qpid-dispatch
TAG="latest"

if [ -n "$TRAVIS_TAG" ]
then
    TAG="$TRAVIS_TAG"
fi

# create the image for building the Proton and Dispatch Router binaries
docker build -t $BUILDER_IMAGE:latest $ABS_BASE/qpid-dispatch-build || exit 1

# build the binaries
docker run -v $ABS_BASE:/binaries $BUILDER_IMAGE:latest || exit 1

# create the Dispatch Router image
docker build -t $REPO:travis-$TRAVIS_BUILD_NUMBER $ABS_BASE || exit 1

if [ -n "$DOCKER_USER" ]
then
    # push Dispatch Router image to Docker Hub
    docker tag $REPO:travis-$TRAVIS_BUILD_NUMBER $REPO:$TAG || exit 1
    docker login -u $DOCKER_USER -p $DOCKER_PASS || exit 1
    docker push $REPO || exit 1
fi
