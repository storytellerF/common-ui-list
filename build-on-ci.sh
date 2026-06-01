#!/bin/bash

mkdir -p ~/.gradle
echo "gpr.user=$ORG_GRADLE_PROJECT_gpr_user" >> ~/.gradle/gradle.properties
echo "gpr.key=$ORG_GRADLE_PROJECT_gpr_key" >> ~/.gradle/gradle.properties

./gradlew build