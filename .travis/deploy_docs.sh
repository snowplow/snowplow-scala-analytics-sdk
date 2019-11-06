#!/bin/bash

ENCRYPTED_KEY_VAR="encrypted_${ENCRYPTION_LABEL}_key"
ENCRYPTED_IV_VAR="encrypted_${ENCRYPTION_LABEL}_iv"
ENCRYPTED_KEY=${!ENCRYPTED_KEY_VAR}
ENCRYPTED_IV=${!ENCRYPTED_IV_VAR}

git config --global user.name "$USER"
git config --global user.email "$TRAVIS_BUILD_NUMBER@$TRAVIS_COMMIT"

openssl aes-256-cbc -K $ENCRYPTED_KEY -iv $ENCRYPTED_IV -in project/travis-deploy-key.enc -out project/travis-deploy-key -d
chmod 600 project/travis-deploy-key

eval "$(ssh-agent -s)"
ssh-add project/travis-deploy-key

sbt ghpagesPushSite
