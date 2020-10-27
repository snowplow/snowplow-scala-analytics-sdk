#!/bin/bash

set -e

github_ref=$1

project_version=$(sbt version -Dsbt.log.noformat=true | perl -ne 'print "$1\n" if /info.*(\d+\.\d+\.\d+[^\r\n]*)/' | tail -n 1 | tr -d '\n')

if [[ "${github_ref}" = "refs/tags/${project_version}" ]]; then
  exit 0
else
  exit 1
fi