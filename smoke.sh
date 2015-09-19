#!/usr/bin/env bash

user_home="$(eval echo ~$USER)"

profile=$1

docker run --name $profile-restbucks-ordering-smoke \
           --rm \
           -t \
           -v $user_home/.gradle:/root/.gradle \
           --link $profile-restbucks-ordering:app \
           hippoom/restbucks \
           ./gradlew runSmokeTest